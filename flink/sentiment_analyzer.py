import json
import logging
import sys
from transformers import pipeline

from pyflink.common import SimpleStringSchema, Types, WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, ProcessFunction, KeyedProcessFunction 
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
BOOTSTRAP_SERVERS = "kafka:29092"
SOURCE_TOPIC = "raw-chats"
SINK_TOPIC = "analysis-result"
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
    Analyzes the sentiment of messages per chat room and aggregates the
    positive/negative ratios.
    """

    def __init__(self, labels):
        self.state = None
        self.classifier = None
        self.labels = labels

    def open(self, runtime_context):
        """
        Initializes the state and loads the model when the Flink Task starts.
        """
        logging.info("Loading sentiment analysis model...")
        state_descriptor = ValueStateDescriptor("sentiment_counts", Types.PICKLED_BYTE_ARRAY())
        self.state = runtime_context.get_state(state_descriptor)
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
        # Using a hypothesis_template can improve classification accuracy in specific domains.
        result = self.classifier(content, self.labels)
        
        # Select the label with the highest score as the result.
        top_label = result['labels'][0]
        logging.info(f"Classification result for streamId {stream_id}: {top_label}")

        # 3. Update label counts and total count
        current_counts = self._update_counts(current_counts, top_label)
        logging.info(f"[State] Updated counts for {stream_id}: {current_counts}")

        # 4. Calculate the final result (ratios) and yield for the sink
        output_data = self._calculate_ratios(stream_id, current_counts)
        output_json = json.dumps(output_data, ensure_ascii=False)

        logging.info(f"Sending analysis result to sink: {output_json}")
        # Yield the result as a JSON string to the output stream.
        yield output_json

# ----------------------------------------------------------------------------
#  ★ 3. Main Function to Run the Flink Job ★
# ----------------------------------------------------------------------------

def run_flink_job():
    """Defines the source, processing, and sink for the PyFlink application and executes the job."""
    logging.info("Starting Flink sentiment analysis job...")
    env = StreamExecutionEnvironment.get_execution_environment()
    # Set parallelism to 1, considering model loading time and resource usage.
    # This may need adjustment in a production environment based on resource availability.
    env.set_parallelism(1)

    logging.info(f"Kafka Source: bootstrap_servers={BOOTSTRAP_SERVERS}, topic={SOURCE_TOPIC}, group_id={KAFKA_GROUP_ID}")
    logging.info(f"Kafka Sink: bootstrap_servers={BOOTSTRAP_SERVERS}, topic={SINK_TOPIC}")

    # --- Kafka Source ---
    # Consumes messages from the 'raw-chats' topic.
    kafka_source = KafkaSource.builder() \
        .set_bootstrap_servers(BOOTSTRAP_SERVERS) \
        .set_topics(SOURCE_TOPIC) \
        .set_group_id(KAFKA_GROUP_ID) \
        .set_starting_offsets(KafkaOffsetsInitializer.latest()) \
        .set_value_only_deserializer(SimpleStringSchema()) \
        .build()

    # --- Kafka Sink ---
    # Sends analysis results to the 'analysis-result' topic.
    kafka_sink = KafkaSink.builder() \
        .set_bootstrap_servers(BOOTSTRAP_SERVERS) \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder() \
                .set_topic(SINK_TOPIC) \
                .set_value_serialization_schema(SimpleStringSchema()) \
                .build()
        ).build()

    # --- Data Stream Processing Logic ---
    data_stream = env.from_source(kafka_source, WatermarkStrategy.no_watermarks(), "Kafka Source")

    # 1. Parse JSON strings from Kafka and convert to (streamId, content) tuples.
    #    Filter out any messages that fail to parse.
    parsed_stream = data_stream.map(parse_kafka_message, output_type=Types.TUPLE([Types.STRING(), Types.STRING()])) \
                               .filter(lambda x: x is not None)

    # 2. Group the data stream by chat room ID (streamId).
    keyed_stream = parsed_stream.key_by(lambda x: x[0])

    # 3. Apply the sentiment analysis and aggregation logic to the grouped stream.
    result_stream = keyed_stream.process(SentimentAnalyzerAggregator(CANDIDATE_LABELS), output_type=Types.STRING())
    
    # 4. Send the final results to the Kafka Sink.
    result_stream.sink_to(kafka_sink)

    # Execute the Flink job
    logging.info("Executing Flink job graph...")
    env.execute("Python Real-time Chat Sentiment Analysis")

# ----------------------------------------------------------------------------
#  ★ 4. Local Model Test Function ★
# ----------------------------------------------------------------------------

def test_model_locally():
    """Loads the classifier and tests it on sample sentences without Flink/Kafka."""
    logging.info("--- Running local model test ---")
    
    # 1. Load the classifier model
    logging.info("Loading model...")
    classifier = get_zeroshot_classifier()
    logging.info("Model loaded.")

    # 2. Prepare sample sentences
    sample_sentences = [
        "안녕하세요",
        "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ",
        "???",
        "재미없다",
        "역시 oo다"
    ]

    # 3. Classify each sentence and print the result
    for sentence in sample_sentences:
        logging.info(f"\nSentence: '{sentence}'")
        result = classifier(sentence, CANDIDATE_LABELS)
        # Format and print the scores for clarity
        scores_str = ", ".join([f"{label}: {score:.4f}" for label, score in zip(result['labels'], result['scores'])])
        logging.info(f" -> Result: {scores_str}")

    logging.info("--- Local model test finished ---")

if __name__ == '__main__':
    # Check for a 'test' argument to run the local test function
    if len(sys.argv) > 1 and sys.argv[1].lower() == 'test':
        test_model_locally()
    else:
        run_flink_job()