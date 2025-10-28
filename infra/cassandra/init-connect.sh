#!/bin/sh

# Wait for Kafka Connect to be ready
echo "Waiting for Kafka Connect to start..."
until [ "$(curl -s -o /dev/null -w '%{http_code}' http://kafka-connect:8083/connectors)" = "200" ]; do
  echo "Kafka Connect is not ready yet. Retrying in 5 seconds..."
  sleep 5
done

echo "Kafka Connect is ready."

# Check if the connector already exists
STATUS_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://kafka-connect:8083/connectors/cassandra-sink-connector)

if [ "$STATUS_CODE" = "200" ]; then
  echo "Connector 'cassandra-sink-connector' already exists. Skipping creation."
else
  echo "Connector 'cassandra-sink-connector' does not exist. Creating..."
  # Create the connector
  curl -X POST -H "Content-Type: application/json" --data @/config/cassandra-sink-config.json http://kafka-connect:8083/connectors
  echo "\nConnector creation command sent."
fi
