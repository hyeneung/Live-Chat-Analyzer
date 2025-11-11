# Live Chat Analyzer - Custom Docker Image Build Plan

This document outlines the step-by-step process for building and pushing custom Docker images required for the Live Chat Analyzer application. These images are then used in the Kubernetes deployment.

**Note:** Replace `hyeneung` with your Docker Hub username in all `docker build` and `docker push` commands.

## Prerequisites

*   **Docker:** Docker Desktop or Docker Engine installed and running.
*   **Docker Hub Account:** Authenticated to your Docker Hub account (`docker login`).
*   **Git:** Git installed for cloning the repository.

## Image Build and Push Steps

### 1. Build and Push `user-server` Image

*   **Description:** The backend service for user management.
*   **Image Name:** `hyeneung/user-server:latest`

```bash
docker build --no-cache -t hyeneung/user-server:latest ./user-server
docker push hyeneung/user-server:latest
```

### 2. Build and Push `chat-server` Image

*   **Description:** The backend service for chat message handling and WebSocket communication.
*   **Image Name:** `hyeneung/chat-server:latest`

```bash
docker build --no-cache -t hyeneung/chat-server:latest ./chat-server
docker push hyeneung/chat-server:latest
```

### 3. Build and Push `flink-sentiment-analyzer` Image

*   **Description:** The Flink application for real-time sentiment analysis of chat messages.
*   **Image Name:** `hyeneung/flink-sentiment-analyzer:latest`

```bash
docker build --no-cache -t hyeneung/flink-sentiment-analyzer:latest ./infra/flink
docker push hyeneung/flink-sentiment-analyzer:latest
```

### 4. Build and Push `spark-summarizer` Image

*   **Description:** The Spark application for summarizing chat data.
*   **Image Name:** `hyeneung/spark-summarizer:latest`

```bash
docker build --no-cache -t hyeneung/spark-summarizer:latest ./infra/spark
docker push hyeneung/spark-summarizer:latest
```

### 5. Build and Push `kafka-connect-cassandra-sink` Image

*   **Description:** The Kafka Connect image with the Cassandra Sink connector for persisting chat data.
*   **Image Name:** `hyeneung/kafka-connect-cassandra-sink:latest`

```bash
docker build --no-cache -t hyeneung/kafka-connect-cassandra-sink:latest ./infra/cassandra/kafka-connect-cassandra-sink
docker push hyeneung/kafka-connect-cassandra-sink:latest
```
