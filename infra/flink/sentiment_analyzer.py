import json
import logging
import sys
from transformers import pipeline

from pyflink.common import SimpleStringSchema, Types, WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, ProcessFunction, KeyedProcessFunction, OutputTag
from pyflink.datastream.state import ValueStateDescriptor
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema
from pyflink.datastream.connectors.kafka import KafkaOffsetsInitializer

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# ----------------------------------------------------------------------------
#  ★ 1. Zero-shot Classification Model Loader & Label Definition ★
# ----------------------------------------------------------------------------

# Define labels for zero-shot classification
CANDIDATE_LABELS = ["칭찬", "비난", "웃음", "질문", "조언"]

# --- Kafka Configuration ---
BOOTSTRAP_SERVERS = "kafka-controller-headless:9092"
SOURCE_TOPIC = "raw-chats"
SINK_TOPIC = "analysis-result"
SUMMARY_REQUEST_TOPIC = "summary-requests" # New topic for summary requests
KAFKA_GROUP_ID = "flink-sentiment-py-group"

def get_zeroshot_classifier():
    """Loads a pre-trained zero-shot classification model from Hugging Face."""
    # If GPU is available, add device=0 for faster processing.
    return pipeline("zero-shot-classification", model="pongjin/roberta_with_kornli")

def parse_kafka_message(msg: str):
    """
    Parses a JSON string from Kafka into a tuple.
    Returns None if parsing fails.
    Expected input format:
    '{ 
        "streamId": "some-id",
        "content": "some message",
        "sender": {
            "id": "sender-id",
            "name": "Sender Name",
            "profileImageUrl": "http://example.com/image.png"
        }
    }'
    """
    try:
        data = json.loads(msg)
        return data['streamId'], data['content']
    except (json.JSONDecodeError, KeyError) as e:
        logging.warning(f"Failed to parse message: {msg}. Error: {e}")
        return None

# ----------------------------------------------------------------------------
#  ★ 2. Core Flink Logic: Classification and Aggregation ProcessFunction ★
# ----------------------------------------------------------------------------

class SentimentAnalyzerAggregator(KeyedProcessFunction):
    """
    Analyzes the sentiment of messages per chat room, aggregates the
    positive/negative ratios, and triggers summarization requests.
    """

    def __init__(self, labels):
        self.state = None
        self.classifier = None
        self.labels = labels
        self.msg_count_state = None
        self.summary_output_tag = OutputTag("summary-request", Types.STRING())

    def open(self, runtime_context):
        """
        Initializes the state and loads the model when the Flink Task starts.
        """
        logging.info("Loading sentiment analysis model...")
        state_descriptor = ValueStateDescriptor("sentiment_counts", Types.PICKLED_BYTE_ARRAY())
        self.state = runtime_context.get_state(state_descriptor)
        
        msg_count_descriptor = ValueStateDescriptor("msg_count", Types.INT())
        self.msg_count_state = runtime_context.get_state(msg_count_descriptor)

        self.classifier = get_zeroshot_classifier()
        logging.info("Model loaded successfully.")

    def _update_counts(self, current_counts, top_label):
        """Updates the sentiment counts based on the classification result."""
        if top_label in current_counts:
            current_counts[top_label] += 1
        current_counts['total'] += 1
        self.state.update(current_counts)
        return current_counts

    def _calculate_ratios(self, stream_id, counts):
        """Calculates the sentiment ratios and formats the output."""
        total = counts['total']
        output_data = {"streamId": stream_id, "totalCount": total, "ratios": {}}

        if total > 0:
            for label in self.labels:
                ratio = counts[label] / total
                output_data["ratios"][label] = round(ratio, 4)
        
        return output_data

    def process_element(self, value, ctx: 'ProcessFunction.Context'):
        """
        The main logic that processes each individual message.
        `value` is a tuple in the format (streamId, content).
        """
        stream_id, content = value
        logging.info(f"Processing message for streamId: {stream_id}")

        # 1. Retrieve the current state (historical data) for the given stream_id
        current_counts = self.state.value()
        logging.info(f"[State] Retrieved counts for {stream_id}: {current_counts}")

        if current_counts is None:
            # If this is the first message for this chat room, initialize the counts dictionary.
            current_counts = {label: 0 for label in self.labels}
            current_counts['total'] = 0
            logging.info(f"[State] Initialized new counts for {stream_id}: {current_counts}")

        # 2. Perform zero-shot classification
        result = self.classifier(content, self.labels)
        top_label = result['labels'][0]
        logging.info(f"Classification result for streamId {stream_id}: {top_label}")

        # 3. Update label counts and total count
        current_counts = self._update_counts(current_counts, top_label)
        logging.info(f"[State] Updated counts for {stream_id}: {current_counts}")

        # 4. Calculate the final result (ratios) and yield for the sink
        output_data = self._calculate_ratios(stream_id, current_counts)
        output_json = json.dumps(output_data, ensure_ascii=False)

        logging.info(f"Sending analysis result to sink: {output_json}")
        yield output_json

        # 5. Check for summary trigger condition
        msg_count = self.msg_count_state.value()
        if msg_count is None:
            msg_count = 0
        
        msg_count += 1
        self.msg_count_state.update(msg_count)
        logging.info(f"Message count for {stream_id} is now {msg_count}")

        if msg_count >= 20:
            logging.info(f"Message count for {stream_id} reached 50. Triggering summary.")
            summary_request = {"streamId": stream_id}
            summary_request_json = json.dumps(summary_request, ensure_ascii=False)
            yield (self.summary_output_tag, summary_request_json)
            # Reset counter
            self.msg_count_state.update(0)
            logging.info(f"Reset message count for {stream_id} to 0.")


# ----------------------------------------------------------------------------
#  ★ 3. Main Function to Run the Flink Job ★
# ----------------------------------------------------------------------------

def run_flink_job():
    """Defines the source, processing, and sink for the PyFlink application and executes the job."""
    logging.info("Starting Flink sentiment analysis job...")
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_parallelism(1)

    logging.info(f"Kafka Source: bootstrap_servers={BOOTSTRAP_SERVERS}, topic={SOURCE_TOPIC}, group_id={KAFKA_GROUP_ID}")
    logging.info(f"Kafka Sink: bootstrap_servers={BOOTSTRAP_SERVERS}, topic={SINK_TOPIC}")
    logging.info(f"Summary Request Sink: bootstrap_servers={BOOTSTRAP_SERVERS}, topic={SUMMARY_REQUEST_TOPIC}")


    # --- Kafka Source ---
    kafka_source = KafkaSource.builder() \
        .set_bootstrap_servers(BOOTSTRAP_SERVERS) \
        .set_topics(SOURCE_TOPIC) \
        .set_group_id(KAFKA_GROUP_ID) \
        .set_starting_offsets(KafkaOffsetsInitializer.latest()) \
        .set_value_only_deserializer(SimpleStringSchema()) \
        .build()

    # --- Kafka Sink for analysis results ---
    analysis_sink = KafkaSink.builder() \
        .set_bootstrap_servers(BOOTSTRAP_SERVERS) \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder() \
                .set_topic(SINK_TOPIC) \
                .set_value_serialization_schema(SimpleStringSchema()) \
                .build()
        ).build()

    # --- Kafka Sink for summary requests ---
    summary_sink = KafkaSink.builder() \
        .set_bootstrap_servers(BOOTSTRAP_SERVERS) \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder() \
                .set_topic(SUMMARY_REQUEST_TOPIC) \
                .set_value_serialization_schema(SimpleStringSchema()) \
                .build()
        ).build()

    # --- Data Stream Processing Logic ---
    data_stream = env.from_source(kafka_source, WatermarkStrategy.no_watermarks(), "Kafka Source")

    parsed_stream = data_stream.map(parse_kafka_message, output_type=Types.TUPLE([Types.STRING(), Types.STRING()])) \
                               .filter(lambda x: x is not None)

    keyed_stream = parsed_stream.key_by(lambda x: x[0])

    # Main sentiment analysis stream
    analysis_stream = keyed_stream.process(SentimentAnalyzerAggregator(CANDIDATE_LABELS), output_type=Types.STRING())
    
    # Side output for summary requests
    summary_stream = analysis_stream.get_side_output(OutputTag("summary-request", Types.STRING()))

    # Sink streams to Kafka
    analysis_stream.sink_to(analysis_sink)
    summary_stream.sink_to(summary_sink)

    logging.info("Executing Flink job graph...")
    env.execute("Python Real-time Chat Sentiment Analysis and Summarization Trigger")

# ----------------------------------------------------------------------------
#  ★ 4. Local Model Test Function ★
# ----------------------------------------------------------------------------

def test_model_locally():
    """Loads the classifier and tests it on sample sentences without Flink/Kafka."""
    logging.info("--- Running local model test ---")
    
    logging.info("Loading model...")
    classifier = get_zeroshot_classifier()
    logging.info("Model loaded.")

    sample_sentences = [
        "안녕하세요",
        "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ",
        "???",
        "재미없다",
        "역시 oo다"
    ]

    for sentence in sample_sentences:
        logging.info(f"\nSentence: '{sentence}'")
        result = classifier(sentence, CANDIDATE_LABELS)
        scores_str = ", ".join([f"{label}: {score:.4f}" for label, score in zip(result['labels'], result['scores'])])
        logging.info(f" -> Result: {scores_str}")

    logging.info("--- Local model test finished ---")

if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1].lower() == 'test':
        test_model_locally()
    else:
        run_flink_job()
