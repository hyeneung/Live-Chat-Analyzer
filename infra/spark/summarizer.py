import logging
import asyncio
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json, length, to_json, struct
from pyspark.sql.types import StructType, StructField, StringType

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Kafka and Cassandra Configuration
BOOTSTRAP_SERVERS = "kafka:29092"
SUMMARY_REQUEST_TOPIC = "summary-requests"
SUMMARY_RESULTS_TOPIC = "summary-results"
CASSANDRA_HOST = "cassandra"
CASSANDRA_KEYSPACE = "chat_data"
CASSANDRA_TABLE = "messages"
NUM_PARTITIONS = 4

spark = None

async def get_summary_from_gpt(text):
    """Placeholder for GPT API call."""
    logging.info("Calling GPT API (mock) for text starting with: %s", text[:50])
    await asyncio.sleep(1) # Simulate network latency
    return f"This is a summary of: {text[:100]}..."

def summarize_partition(iterator):
    rows = list(iterator)
    if not rows:
        return iter([])

    chats_by_stream = {}
    for row in rows:
        if row.stream_id not in chats_by_stream:
            chats_by_stream[row.stream_id] = []
        chats_by_stream[row.stream_id].append(row.content)

    async def summarize_all():
        tasks = []
        for stream_id, chats in chats_by_stream.items():
            full_chat = "\n".join(chats)
            tasks.append(get_summary_from_gpt(full_chat))
        return await asyncio.gather(*tasks)

    summaries = asyncio.run(summarize_all())
    
    results = []
    stream_ids = list(chats_by_stream.keys())
    for i, summary in enumerate(summaries):
        results.append({"streamId": stream_ids[i], "summary": summary})
        
    return iter(results)

def process_batch(df, epoch_id):
    stream_ids = [row.streamId for row in df.collect()]
    if not stream_ids:
        return

    logging.info("Processing batch for stream_ids: %s", stream_ids)

    base_cassandra_df = spark.read \
        .format("org.apache.spark.sql.cassandra") \
        .option("keyspace", CASSANDRA_KEYSPACE) \
        .option("table", CASSANDRA_TABLE)

    all_chats_df = None

    for stream_id in stream_ids:
        # Read from Cassandra for each stream_id.
        # The new schema sorts by created_at DESC, so limit(300) gets the latest.
        cassandra_df = base_cassandra_df.load().where(col("stream_id") == stream_id).limit(300)
        
        if all_chats_df is None:
            all_chats_df = cassandra_df
        else:
            all_chats_df = all_chats_df.union(cassandra_df)

    if all_chats_df is None:
        logging.info("No chat messages found for the given stream_ids.")
        return

    chats_df = all_chats_df

    # Repartition to distribute the work, ensuring all messages for a given stream are in the same partition.
    repartitioned_df = chats_df.repartition(NUM_PARTITIONS, "stream_id")
    logging.info("Repartitioned to %d partitions", repartitioned_df.rdd.getNumPartitions())

    # Preprocessing / Filtering
    preprocessed_df = repartitioned_df.filter(length(col("content")) >= 5)
    preprocessed_df = preprocessed_df.filter(~col("content").rlike(r"^(.)\\1+$"))

    # Summarize using mapPartitions
    summaries_rdd = preprocessed_df.rdd.mapPartitions(summarize_partition)
    if summaries_rdd.isEmpty():
        logging.info("No summaries generated.")
        return
        
    summaries_df = summaries_rdd.toDF()

    logging.info("Summaries:")
    summaries_df.show(truncate=False)

    # Publish to Kafka
    kafka_df = summaries_df.select(col("streamId").alias("key"), to_json(struct(col("streamId"), col("summary"))).alias("value"))

    kafka_df.write \
        .format("kafka") \
        .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS) \
        .option("topic", SUMMARY_RESULTS_TOPIC) \
        .save()
    logging.info("Published summaries to Kafka topic: %s", SUMMARY_RESULTS_TOPIC)


def main():
    global spark
    spark = SparkSession.builder \
        .appName("ChatSummarizer") \
        .config("spark.cassandra.connection.host", CASSANDRA_HOST) \
        .getOrCreate()

    # Set log level
    spark.sparkContext.setLogLevel("WARN")
    logging.info("Spark Session created")

    # Define schema for the incoming Kafka message
    schema = StructType([
        StructField("streamId", StringType(), True)
    ])

    # Read from Kafka topic
    df = spark.readStream \
        .format("kafka") \
        .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS) \
        .option("subscribe", SUMMARY_REQUEST_TOPIC) \
        .option("startingOffsets", "latest") \
        .load()

    logging.info("Streaming from Kafka topic: %s", SUMMARY_REQUEST_TOPIC)

    # Parse the value from Kafka message
    parsed_df = df.select(from_json(col("value").cast("string"), schema).alias("data")).select("data.*")

    # Process each micro-batch
    query = parsed_df.writeStream \
        .foreachBatch(process_batch) \
        .start()

    logging.info("Streaming query started. Waiting for termination...")
    query.awaitTermination()

if __name__ == "__main__":
    main()