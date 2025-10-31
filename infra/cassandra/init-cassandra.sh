#!/bin/sh
echo "Waiting for Cassandra to start..."
until cqlsh cassandra -e "describe keyspaces" > /dev/null 2>&1; do
  echo "Cassandra is unavailable - sleeping"
  sleep 5
done
echo "Cassandra is up - executing schema"
cqlsh cassandra -f /scripts/schema.cql
echo "Schema applied"