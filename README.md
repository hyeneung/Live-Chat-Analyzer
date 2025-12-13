# 💬 Live Chat Analyzer

A real-time, distributed system for analyzing and summarizing high-volume chat streams from live broadcasts. The system is designed to handle thousands of messages per second, providing immediate sentiment analysis and periodic summaries to viewers and broadcasters.

## ✨ Features

- **High-Volume Chat Ingestion**: Reliably processes and stores thousands of chat messages per second.
- **Real-Time Sentiment Analysis**: Instantly analyzes and displays the sentiment of each chat message.
- **Periodic Chat Summarization**: Generates periodic summaries of the conversation, providing a quick overview of the chat's context.
- **Live Viewer Count**: Tracks and displays the number of viewers in a broadcast in real-time.
- **Scalable, Distributed Architecture**: Built on a microservices architecture using a message-driven approach, allowing for independent scaling of components.
- **Containerized & Orchestrated**: Fully containerized with Docker and ready for deployment on Kubernetes.

## 📹 Demo
The system performs real-time sentiment analysis on chat messages to categorize viewer responses (e.g., compliments, insults, humor). For chat summarization, the system triggers a summary generation after every 20 new messages by preprocessing and aggregating the previous 100 comments. This summary is generated via the OpenAI API and reflected on the viewer interface dynamically.
![demo](https://github.com/user-attachments/assets/b09f81fa-b7af-46f1-b2d8-b937784a64ee)

## 🚀 System Architecture

This project is composed of several microservices and a data processing pipeline that work together to provide real-time analysis.

<img width="2370" height="1202" alt="image" src="https://github.com/user-attachments/assets/954a3939-c3ed-482e-9dbb-74a4052a5723" />

This project is built using a distributed microservices architecture, leveraging Kubernetes for orchestration and Vercel for frontend hosting.

### Core Components

-   **Frontend (Vercel)**: A **Vue.js** single-page application hosted externally on **Vercel**, providing the user interface.
-   **User Server (Spring Boot)**: Manages user authentication, stream metadata, tracks viewer counts, and publishes updates to Redis. Delivers real-time viewer count updates via **Server-Sent Events (SSE)**.
-   **Chat Server (Spring Boot)**: Handles real-time communication via **WebSockets (STOMP)**. It publishes raw chat messages to Kafka for asynchronous processing. For immediate fan-out to clients, it directly publishes messages to dynamic Redis Pub/Sub channels. It also consumes processed data (like analysis and summary results) from other Kafka topics, which are then distributed to clients via Redis.

### Data & Processing Pipeline

-   **Kafka**: Acts as the central, durable message broker.
    -   `raw-chats`: A topic for all incoming raw chat messages from the `chat-server`.
    -   `summary-requests`: A topic used to trigger the Spark summarization job.
    -   `analysis-results` & `summary-results`: Topics for processed data from the analysis pipeline.

-   **Flink**: A stream-processing framework that consumes from `raw-chats`, performs real-time sentiment analysis, and publishes results back to a Kafka topic. It also sends messages to the `summary-requests` topic to trigger periodic summarization.

-   **Spark**: A distributed processing framework that consumes from the `summary-requests` topic to periodically generate chat summaries from the data in Cassandra/Kafka.

-   **Redis**: Used as a high-speed, in-memory backbone for real-time messaging, caching, and state management.
    -   **Pub/Sub Backplane**: Functions as a messaging backplane. Services publish updates to dynamic `broadcast:{streamId}` channels, allowing `chat-server` instances to subscribe only to the streams their clients are watching, enabling efficient message fan-out.
    -   **Stream Data & Caching**: Stores the set of active user IDs for each stream (used to calculate live viewer counts) and caches the generated chat summaries.
    -   **Authentication State**: Stores user refresh tokens to manage authentication sessions and enable secure token rotation.

-   **Cassandra**: A highly-scalable NoSQL database used for the persistent storage of all chat messages from the `raw-chats` topic.

## 🛠️ Tech Stack

-   **Frontend**: `Vue.js`
-   **Backend**: `Java`, `Spring Boot`
-   **Messaging/Streaming**: `Apache Kafka`, `WebSockets`, `SSE`
-   **Stream Processing**: `Apache Flink`
-   **Batch Processing**: `Apache Spark`
-   **Database**: `Cassandra` (Primary Storage), `Redis` (Pub/Sub & Caching)
-   **Infrastructure**: `Docker`, `Kubernetes`, `Prometheus`, `Grafana`

## 📂 Project Structure

```
.
├── chat-generator/    # Scripts to generate mock chat data for testing
├── chat-server/       # Spring Boot service for WebSocket chat handling
├── user-server/       # Spring Boot service for user and stream management
├── docs/              # Documentation for deployment and queries
├── frontend/          # Vue.js frontend application
├── infra/             # Docker-compose setup and configurations for infrastructure (Kafka, Flink, Spark, etc.)
└── k8s/               # Kubernetes manifests for production deployment
```

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for more details.