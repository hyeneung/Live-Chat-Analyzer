# Load Test Report and Comparative Analysis

This document outlines the scenario for load testing the real-time chat system, details the execution environment, and presents a detailed analysis of the results. It includes a direct comparison between two tests with different Flink parallelism settings to quantify the impact of the change.

## 1. Load Test Scenario

### 1.1. Objective

The primary goal of this load test is to evaluate the performance and stability of the chat system under a high-volume messaging load. The test simulates a large number of concurrent users sending messages at a steady rate, allowing us to measure message processing latency, resource utilization, and overall system reliability. This scenario specifically targets the latency improvements made by refactoring the `chat-server` to publish messages directly to Redis.

### 1.2. Test Configuration

The load test is configured using the following parameters from the `load-test/.env` file:

| Parameter                      | Value                                                | Description                                                              |
| ------------------------------ | ---------------------------------------------------- | ------------------------------------------------------------------------ |
| `WEBSOCKET_URL`                | `https://api.live-streaming.store/chat/ws`           | The target WebSocket endpoint for the `chat-server`.                     |
| `NUM_CLIENTS`                  | `500`                                                | The total number of concurrent WebSocket clients to simulate.            |
| `RAMP_UP_TIME_SECONDS`         | `30`                                                 | The duration over which all clients will be gradually connected.         |
| `MESSAGES_PER_SECOND_PER_CLIENT` | `0.5`                                                | The average number of messages each client sends per second.             |

### 1.3. Execution Flow

1.  **Ramp-up Phase (0-30 seconds)**: The test begins by gradually connecting 500 clients over a 30-second period. This equates to approximately 16-17 new client connections per second, preventing a sudden connection spike.
2.  **Steady-State Phase**: Once all clients are connected, they begin sending messages. Each of the 500 clients sends, on average, one message every two seconds.
3.  **Peak Load**: The system reaches a sustained peak load where it is expected to process **250 messages per second** (500 clients * 0.5 messages/sec/client) from the clients, in addition to handling the fan-out delivery back to all 500 clients via Redis Pub/Sub.

## 2. System Execution Environment

The services are deployed on an Azure Kubernetes Service (AKS) cluster with the following node and service configurations.

### 2.1. Kubernetes Node Pools

The cluster consists of two node pools with different VM sizes to accommodate system and user workloads.

| Pool Name     | Instance Type     | vCPUs | Memory | OS    | Description                               |
| ------------- | ----------------- | ----- | ------ | ----- | ----------------------------------------- |
| `agentpool`   | `Standard_D4as_v4`| 4     | 16 GiB | Linux | Primary node pool for system components.  |
| `a`           | `Standard_B2as_v2`| 2     | 8 GiB  | Linux | Secondary node pool for user applications.|

### 2.2. Service Deployment Configuration

The key components of the real-time data pipeline are configured as follows:

| Service               | Replicas / Parallelism                                 | Details                                                                                           |
| --------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| **Chat Server**       | 2 Replicas                                             | Handles WebSocket connections and the initial message fan-out.                                    |
| **User Server**       | 1 Replica                                              | Manages user authentication and stream metadata.                                                  |
| **Kafka Cluster**     | 3 Brokers                                              | Provides a fault-tolerant message bus.                                                            |
| &nbsp;&nbsp;↳ Topics | 4 Partitions each                                      | All topics (`raw-chats`, `analysis-result`, etc.) are partitioned for parallel consumption.       |
| **Flink Cluster**     | Parallelism: 4 (2 TaskManagers with 2 slots each)      | Aligns with the Kafka topic partition count for optimal parallel processing of sentiment analysis.  |
| **Redis Cluster**     | 1 Master, 3 Replicas                                   | Manages high-throughput Pub/Sub messaging for real-time fan-out and caching.                      |
| **Spark Cluster**     | 1 Master, 1 Worker                                     | Handles periodic batch summarization jobs.                                                        |
| **Databases**         | Cassandra (1 Replica), MySQL (1 Replica)               | Provide persistent storage for chat logs and relational metadata, respectively.                   |

---

## 3. Detailed Analysis of Primary Test (Flink Parallelism = 4)

This section provides a detailed analysis of the main test, conducted from **07:05 to 07:22**, where the system was stable and performed optimally.

### 3.1. Key Metrics Summary

| Component / Metric                  | Peak Value                         | Average (during load)            | Status / Notes                                             |
| ----------------------------------- | ---------------------------------- | -------------------------------- | ---------------------------------------------------------- |
| **`chat-server-0` CPU**             | 1.0 vCPU Core                      | ~0.9 vCPU Core                   | **Saturated**. Indicates a primary performance bottleneck. |
| **`chat-server-1` CPU**             | 1.0 vCPU Core                      | ~0.9 vCPU Core                   | **Saturated**. Load is well-distributed.                   |
| **`chat-server-0` JVM Memory**      | ~950 MB                            | ~800 MB                          | **High Usage**. Shows significant memory imbalance.        |
| **`chat-server-1` JVM Memory**      | ~250 MB                            | ~250 MB                          | **Stable**. Highlights the imbalance with Pod 0.           |
| **WebSocket Sessions**              | 464 total (228 + 236)              | ~450                             | Excellent balance between pods.                            |
| **Kafka Ingress (`raw-chats`)**     | 280 RPS                            | ~125 RPS                         | Throttled by server CPU, but pipeline handled the peak.    |
| **Kafka Lag (Flink)**               | < 10 messages                      | ~0                               | **Healthy**. No processing delay.                          |
| **Kafka Lag (Cassandra Sink)**      | 17,641 messages                    | -                                | Temporary spike, but recovered quickly.                    |
| **Flink Backpressure**              | 0 %                                | 0 %                              | **Healthy**. No backpressure at any stage.                 |

### 3.2. Detailed Interpretation
-   **CPU Bottleneck**: The `chat-server` pods consistently hit their CPU limits (~1.0 core) during the peak phase (`07:13`-`07:16`). This saturation is the definitive bottleneck of the system, limiting its ability to handle more than ~500 users.
-   **Memory Imbalance**: There is a severe and consistent memory imbalance between `chat-server-0` (~950MB) and `chat-server-1` (~250MB). This is not related to load distribution (as sessions/CPU were balanced) and points to an application-level issue like a memory leak or inefficient object allocation in one pod, requiring a deep-dive analysis.
-   **Pipeline Health**: The data pipeline itself was perfectly healthy. The peak ingress of 280 RPS on the `raw-chats` topic was handled flawlessly by Flink, with zero backpressure and negligible consumer lag for real-time analysis. This confirms the parallelism=4 configuration is effective.

---

## 4. Comparative Analysis: Flink Parallelism (1 vs. 4)

Two tests were conducted targeting a load of **500 concurrent users** generating approximately **250 messages per second (RPS)** in total. The key difference was the Flink job configuration.
The impact of increasing Flink parallelism is most evident when comparing the unhealthy baseline test (P=1) with the stable primary test (P=4).

| Parameter           | Test 1 (Baseline)                                   | Test 2 (Improved)                                   | Change                                |
| ------------------- | --------------------------------------------------- | --------------------------------------------------- | ------------------------------------- |
| Time Window         | `06:30` - `06:45`                                   | `07:05` - `07:22`                                   | -                                     |
| **Flink Parallelism** | **1**                                               | **4**                                               | **Increased from 1 to 4**             |
| Kafka Partitions    | 4                                                   | 4                                                   | Identical                             |


### 4.1. Pipeline Performance Comparison

| Metric                  | Test 1 (P=1, Unhealthy)                      | Test 2 (P=4, Healthy)                        | Impact of Change                                      |
| ----------------------- | -------------------------------------------- | -------------------------------------------- | ----------------------------------------------------- |
| **Flink Backpressure**  | **100%** (at `06:38`)                        | **0%**                                       | **Problem Solved**: Processing bottleneck eliminated. |
| **Kafka Lag (Flink)**   | **>20,000** messages (at `06:40`)            | **<10** messages                             | **Problem Solved**: Real-time processing restored.    |
| **Kafka Ingress (RPS)** | Throttled to **~100 RPS**                    | Reached **280 RPS**                          | **3x Improvement**: System can now handle target load.  |
| **Flink Throughput**    | Struggled at **2-3 RPS**                     | Peaked at **>5 RPS**                         | Matches the higher ingress rate.                      |

### 4.2. Analysis
With **Parallelism=1**, the Flink job was unable to process data from 4 Kafka partitions, leading to a complete pipeline stall. This created **100% backpressure**, which caused **massive consumer lag** and throttled the entire system's message intake. By increasing **Parallelism to 4**, each Flink instance handled one partition, resolving the bottleneck and allowing the system to achieve its full performance potential.

---

## 5. Final Conclusion & Recommendations

1.  **Primary Finding**: Increasing Flink parallelism from 1 to 4 was a **critical success**. It resolved the severe data pipeline bottleneck, enabling the system to handle its full target load without data processing delays.
2.  **Identified Bottleneck**: The load tests clearly identify that the next performance limit is the **CPU capacity of the `chat-server` pods**.
3.  **Recommendations**:
    -   **Scale Horizontally**: To support more than 500 users, the `chat-server` deployment must be scaled horizontally (add more pods).
    -   **Investigate Memory**: A high-priority investigation into the **JVM memory imbalance** between `chat-server` pods is crucial for long-term stability.

## 6. Test Artifacts

-   **Load Test Result (Grafana Snapshot):**
    -   [https://snapshots.raintank.io/dashboard/snapshot/0KndBiPatPtWRRVlItf9s2tYqrJzWT8D](https://snapshots.raintank.io/dashboard/snapshot/0KndBiPatPtWRRVlItf9s2tYqrJzWT8D)
-   **Load Testing Video:**
    -   [https://drive.google.com/file/d/1ENJO6DjVqvnZNL91MDd98rzW2g5X1lVx/view?usp=sharing](https://drive.google.com/file/d/1ENJO6DjVqvnZNL91MDd98rzW2g5X1lVx/view?usp=sharing)
-   **Node CPU Utilization Screenshot(Before&After the test):**
    -   <img width="1029" height="230" alt="image" src="https://github.com/user-attachments/assets/f46ed09d-9920-4d8c-88ca-310877c279d7" />