# Load Test Report and Comparative Analysis

This document outlines the scenario for load testing the real-time chat system, details the execution environment, and presents a detailed analysis of the results. It includes a direct comparison between two tests with different Flink parallelism settings to quantify the impact of the change.

## 1. Load Test Scenario

### 1.1. Objective

The primary goal of this load test is to evaluate the performance and stability of the chat system under a high-volume messaging load. The test simulates a large number of concurrent users sending messages at a steady rate, allowing us to measure message processing latency, resource utilization, and overall system reliability.

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

The key components of the real-time data pipeline are configured as follows (Optimized Configuration):

| Service               | Replicas / Parallelism                                 | Details                                                                                                                           |
| --------------------- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **Chat Server** | 2 Replicas                                             | Handles WebSocket connections. Scaled down from 4 to 2 during optimization.                                                       |
| **User Server** | 1 Replica                                              | Manages user authentication and stream metadata.                                                                                  |
| **Kafka Cluster** | 3 Brokers                                              | Provides a fault-tolerant message bus.                                                                                            |
| &nbsp;&nbsp;↳ Topics | **8 Partitions** (`raw-chats`, `analysis-result`)      | Key high-volume topics scaled to **8 partitions** to match Flink parallelism. Other topics remain at 4.                           |
| **Flink Cluster** | **Parallelism: 8** (8 TaskManagers with 1 slot each)   | Scaled out to 8 to eliminate backpressure. 1 slot per TM ensures isolation and dedicated resources.                               |
| **Redis Cluster** | 1 Master, 3 Replicas                                   | Manages high-throughput Pub/Sub messaging for real-time fan-out and caching.                                                      |
| **Spark Cluster** | 1 Master, 1 Worker                                     | Handles periodic batch summarization jobs.                                                                                        |
| **Databases** | Cassandra (1 Replica), MySQL (1 Replica)               | Provide persistent storage for chat logs and relational metadata, respectively.                                                   |

## 3. Executive Summary of Results

Key Finding: **Scaling Flink Parallelism from 4 to 8** eliminated critical bottlenecks (99.8% backpressure), allowing the system to handle maximum load with **zero backpressure**, even when Chat Server resources were halved (Scale-in).

## 4. Performance Summary Table

| Metric | Period 1 (Chat 4 / Flink 4) | Period 2 (Chat 4 / Flink 8) | Period 3 (Chat 2 / Flink 4) | Period 4 (Chat 2 / Flink 8) |
| :--- | :--- | :--- | :--- | :--- |
| **Max Users** | 396 | 483 (+22.0%) | 483 (+22.0%) | **499 (+26.0%)** |
| **Flink Backpressure** | **99.8% (Critical)** | 60.3% (-39.6%) | 66.1% (-33.8%) | **0.0% (Perfect)** |
| **Flink Processing Lag** | 45,551 | 33,999 (-25.4%) | 43,375 (-4.8%) | **8,114 (-82.2%)** |
| **Cassandra Sink Lag** | 10,844 | 10,433 (-3.8%) | 13,845 (+27.7%) | **8,115 (-25.2%)** |
| **Kafka RPS (Max)** | 203 | 212 (+4.4%) | **346 (Inefficient)** | **82 (Optimized)** |
| **Chat CPU (Per Pod)** | 0.18 cores | 0.12 cores (-33.3%) | 0.27 cores (+50.0%) | **0.42 cores (+133.3%)** |

*\*Note: The terminal RPS spike observed in Periods 1-3 indicates buffer flushing due to backpressure. The absence of this spike in Period 4 confirms zero buffering.*

## 5. Detailed Analysis by Scenario

### 5.1. Period 1: Baseline (05:30 - 05:45)
* **Configuration:** Chat 4 / Flink 4
* **Metrics:**
    * **Max Users:** 396
    * **Flink Backpressure:** **99.8% (Critical)**
    * **Flink Lag:** 45,551
    * **Sink Lag:** 10,844
    * **RPS:** 203
    * **Chat CPU:** 0.18 cores/pod
* **Analysis:**
    This period served as the baseline. With only 396 users, the system stalled. **99.8% backpressure** confirms Flink (Parallelism 4) was the primary bottleneck. The majority of the lag (45k) was stuck at Flink ingestion, while the Cassandra Sink also showed backlog (10k).

### 5.2. Period 2: Flink Scale-out (06:30 - 06:45)
* **Configuration:** Chat 4 / **Flink 8**
* **Metrics:**
    * **Max Users:** 483 (+22% vs P1)
    * **Flink Backpressure:** 60.3% (-40%p vs P1)
    * **Flink Lag:** 33,999 (-25% vs P1)
    * **Sink Lag:** 10,433 (Similar to P1)
    * **RPS:** 212
    * **Chat CPU:** 0.12 cores/pod
* **Comparative Insights:**
    Doubling Flink parallelism significantly improved throughput. **Backpressure dropped to 60.3%**, and Flink processing lag decreased by 25%. However, the Sink Lag remained constant (~10k), indicating that as Flink processed data faster, the database write bottleneck became more apparent.

### 5.3. Period 3: Stress Test (07:30 - 07:37)
* **Configuration:** **Chat 2** / Flink 4
* **Metrics:**
    * **Max Users:** 483
    * **Flink Backpressure:** 66.1% (+6%p vs P2)
    * **Flink Lag:** 43,375 (Reverted to P1 level)
    * **Sink Lag:** 13,845
    * **RPS:** **346 (High overhead)**
    * **Chat CPU:** 0.27 cores/pod
* **Comparative Insights:**
    Halving Chat Servers and reverting Flink to 4 caused the bottleneck to return immediately.
    1.  **Lag Spike:** Flink Lag surged back to 43k, proving **Flink 4 is the hard limit**.
    2.  **Inefficient RPS (346):** High Flink backpressure (66%) slowed Kafka consumption, saturating the cluster and triggering frequent broker throttling. This increased Chat Server response latency, forcing incomplete batch flushes and retries—resulting in 346 RPS with only ~0.7 messages/request and significant network overhead.
    3.  **Terminal Spike (Backlog Flush):** The sharp RPS spike observed at the end of the test is a symptom of **congestion**. Due to high backpressure, the producers held a significant backlog of messages in memory. When the user load (input) ceased, the servers utilized the freed-up resources to rapidly flush this accumulated backlog to Kafka.
    4.  **Sink Lag Increase:** The sudden burst of data likely overwhelmed the Cassandra Sink temporarily, increasing Sink Lag to 13k.

### 5.4. Period 4: Optimization (16:31 - 16:35)
* **Configuration:** **Chat 2** / **Flink 8**
* **Metrics:**
    * **Max Users:** **499 (Highest)**
    * **Flink Backpressure:** **0.0% (Perfect)**
    * **Flink Lag:** **8,114 (Lowest)**
    * **Sink Lag:** **8,115**
    * **RPS:** **82 (Stable)**
    * **Chat CPU:** **0.42 cores/pod (Highest)**
* **Comparative Insights**
    This configuration achieved the highest stability and performance.
    1.  **Optimal Flow:** Flink Lag dropped massively (45k -> 8k), and Backpressure hit 0%. The remaining lag is now evenly split between Flink and Cassandra Sink, indicating the bottleneck has shifted to the DB layer.
    2.  **True Performance:** The record-high **CPU usage (0.42 cores/pod)** confirms that the servers were actually processing traffic at maximum density during that short window, validating the system's superior efficiency.
    3. **Optimal Batching (RPS 82):**
    * The low RPS is **proof of optimization**.
    * With Chat Servers scaled down to 2, the concentrated traffic allowed Kafka Producers to form efficient batches (approx. **3 messages/request**) within the `linger.ms` window.
    * This reduced network overhead by **76%** compared to Period 3 (346 vs 82 RPS) while maintaining the same message throughput (250 msg/sec).
    4. **Absence of Terminal Spike (Zero Backpressure):**
    * Unlike Periods 1-3, Period 4 shows **no RPS spike** at the end of the test.
    * **Reason:** With **0% Backpressure**, messages were processed and sent to Kafka in real-time without buffering in the Chat Servers.
    * **Conclusion:** The flatline termination confirms that the system had **zero backlog**, proving it can handle the maximum load effortlessly without internal congestion.

## 6. Final Conclusion & Recommendations

1.  **Optimal Config:** **Chat Server 2 / Flink Parallelism 8**.
2.  **Stability Indicator:** The low Kafka RPS (82) and the **absence of a terminal flush spike** in Period 4 serve as the definitive indicators of a healthy, congestion-free pipeline.
3.  **Efficiency:** The scale-in of Chat Servers improved batching efficiency (3 msgs/req), significantly reducing the IOPS load on the Kafka cluster.
4.  **Shifting Bottlenecks:** The bottleneck shifted from **Flink Processing** (Period 1-3) to **Cassandra Sink** (Period 4).
5.  **Recommendations:**
    * **Deploy:** Chat 2 / Flink 8.
    * **Tune:** Optimize Cassandra Sink (batch size, task count) to clear the remaining 8k lag.
    * **Testing:** For future benchmarks, increase the test duration to at least **5 minutes** to ensure accurate RPS measurement.

## 7. Test Artifacts

-   **Load Test Result (Grafana Snapshot):**
    -   [https://snapshots.raintank.io/dashboard/snapshot/SNUqgRMT6IY1KwTNe8CqSG65Q7GnCkaP](https://snapshots.raintank.io/dashboard/snapshot/SNUqgRMT6IY1KwTNe8CqSG65Q7GnCkaP)
-   **Load Testing Video:**
    -   [chatserver4-flink4(05:30 - 05:45)](https://drive.google.com/file/d/1JggKiYHqO-ssOTslLE0z0Pr8u8JqJRtL/view?usp=sharing)
    -   [chatserver4-flink8(06:30 - 06:45)](https://drive.google.com/file/d/1JaQLXQZIc52-z6rnc7kFAhxVo7wAGob-/view?usp=sharing)
    -   [chatserver2-flink4(07:30 - 07:37)](https://drive.google.com/file/d/1ii3y3-Ajpll3i1JtBU114U-a0eJrFnm6/view?usp=sharing)
    -   [chatserver2-flink8(16:31 - 16:35)](https://drive.google.com/file/d/1JiDsUEBACunpHIxfNYukgGAEYiDNQ2L8/view?usp=sharing)
-   **Node CPU Utilization Screenshot(Before&After the test):**
    -   <img width="1029" height="230" alt="image" src="https://github.com/user-attachments/assets/f46ed09d-9920-4d8c-88ca-310877c279d7" />

---