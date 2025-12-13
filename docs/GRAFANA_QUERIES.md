# Grafana Dashboard Queries for Live-Chat-Analyzer

This document provides a comprehensive list of PromQL queries and corresponding titles for building a Grafana dashboard to monitor the Live-Chat-Analyzer system during load testing.

---

### 1. Kafka Key Metrics

Metrics collected via Kafka Exporter.

| Title | PromQL Query | Description |
| :--- | :--- | :--- |
| **Kafka Consumer Lag (Overall)** | `sum(kafka_consumergroup_lag) by (consumergroup, topic)` | Shows the total number of messages (lag) accumulated per topic and consumer group. A continuously rising trend indicates a bottleneck in that consumer. |
| **Consumer Lag: `raw-chats` (Flink)** | `kafka_consumergroup_lag{topic="raw-chats"}` | Shows how well Flink is consuming messages from the `raw-chats` topic. |
| **Consumer Lag: `summary-requests` (Spark)** | `kafka_consumergroup_lag{topic="summary-requests"}` | Shows how well Spark is consuming messages from the `summary-requests` topic. **(Key bottleneck indicator)** |
| **Consumer Lag: `analysis-result` (Chat-Server)** | `kafka_consumergroup_lag{topic="analysis-result"}` | Shows how well the Chat-Server is consuming messages from the `analysis-result` topic. |
| **Consumer Lag: `summary-results` (Chat-Server)** | `kafka_consumergroup_lag{topic="summary-results"}` | Shows how well the Chat-Server is consuming messages from the `summary-results` topic. |
| **Topic Incoming Message Rate (RPS)** | `sum(rate(kafka_topic_partition_current_offset[1m])) by (topic)` | Shows the rate of messages (messages per second) flowing into each topic. Useful for observing incoming load during testing. |

---

### 2. Flink Key Metrics

Metrics collected via Flink's Prometheus Reporter.

| Title | PromQL Query | Description |
| :--- | :--- | :---|
| **Flink Backpressure** | `sum(rate(flink_taskmanager_job_task_backPressuredTimeMsPerSecond[1m])) by (job_name, task_name)` | Indicates the level of backpressure on Flink operators. A value greater than 0 suggests that downstream systems (often Kafka) are slowing down. `job_name` and `task_name` labels can be selected from suggestions in the Grafana query editor. |
| **Flink Records In Per Second (Job)** | `sum(rate(flink_taskmanager_job_task_numRecordsInPerSecond[1m])) by (job_name)` | Shows the number of records per second being input to Flink Jobs. |
| **Flink Records Out Per Second (Job)** | `sum(rate(flink_taskmanager_job_task_numRecordsOutPerSecond[1m])) by (job_name)` | Shows the number of records per second being output from Flink Jobs. |

---

### 3. Spark Key Metrics (including Custom Metrics)

Metrics from Spark's Prometheus Servlet and custom metrics added to `summarizer.py`.

| Title | PromQL Query | Description |
| :--- | :--- | :--- |
| **Spark OpenAI API Avg Latency** | `rate(spark_openai_api_latency_seconds_sum[5m]) / rate(spark_openai_api_latency_seconds_count[5m])` | Average response time (in seconds) for OpenAI API calls. **(Key indicator for bottleneck root cause)** |
| **Spark Cassandra Query Avg Latency** | `rate(spark_cassandra_query_latency_seconds_sum[5m]) / rate(spark_cassandra_query_latency_seconds_count[5m])` | Average response time (in seconds) for Cassandra query operations. **(Key indicator for bottleneck root cause)** |
| **Spark Master: Running Applications** | `spark_master_apps_running` | Number of applications currently running on the Spark Master. |
| **Spark Master: Alive Workers** | `spark_master_workers_alive` | Number of active Spark Workers connected to the Spark Master. |

---

### 4. User-Server / Chat-Server Key Metrics (Spring Boot Actuator)

Common metrics collected from Spring Boot Actuator endpoints. Filter by `application="user-server"` or `application="chat-server"`.

| Title | PromQL Query | Description |
| :--- | :--- | :--- |
| **HTTP Request Throughput (RPS)** | `sum(rate(http_server_requests_seconds_count{application="user-server"}[1m])) by (uri, status)` | Shows the rate of requests per second (RPS) for each API endpoint, broken down by HTTP status code. |
| **HTTP Average Response Time** | `sum(rate(http_server_requests_seconds_sum{application="user-server"}[1m])) by (uri) / sum(rate(http_server_requests_seconds_count{application="user-server"}[1m])) by (uri)` | Shows the average response time (in seconds) for each API endpoint. |
| **JVM Memory Usage** | `jvm_memory_used_bytes{application="user-server"}` | Shows the JVM heap memory usage for the application. |
| **System CPU Usage** | `system_cpu_usage{application="user-server"}` | Shows the CPU utilization of the application (value between 0.0 and 1.0). |
| **Active WebSocket Sessions (Chat-Server Only)** | `websocket_sessions_active{application="chat-server"}` | Shows the number of active WebSocket sessions connected to the Chat-Server. |

---

### 5. Container Resource Metrics (cAdvisor)

Container-level resource metrics collected directly from the Kubernetes nodes' Kubelet/cAdvisor endpoint.

| Title | PromQL Query | Description |
| :--- | :--- | :--- |
| **Pod CPU Usage** | `sum(rate(container_cpu_usage_seconds_total{image!=""}[5m])) by (pod)` | Shows the amount of CPU cores being used by each Pod. |
| **Pod Memory Usage** | `sum(container_memory_working_set_bytes{image!=""}) by (pod)` | Shows the working set memory usage for each Pod. |
| **Container Network Receive** | `sum(rate(container_network_receive_bytes_total[5m])) by (pod)` | Shows the rate of network traffic (bytes/sec) being received by each Pod. |
| **Container Network Transmit** | `sum(rate(container_network_transmit_bytes_total[5m])) by (pod)` | Shows the rate of network traffic (bytes/sec) being transmitted by each Pod. |
---

### 6. Redis Key Metrics

Metrics collected via Redis Exporter.

| Title | PromQL Query | Description |
| :--- | :--- | :--- |
| **Memory Usage** | `redis_memory_used_bytes` | Shows the total memory allocated by Redis. |
| **Connected Clients** | `redis_connected_clients` | Number of client connections (excluding connections from replicas). |
| **Commands Processed Per Second** | `rate(redis_commands_total[5m])` | The rate of commands processed by the Redis server per second. |
| **Uptime** | `redis_uptime_in_seconds / 3600` | Redis server uptime in hours. |
| **Keyspace Hit Ratio** | `(rate(redis_keyspace_hits_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m]))) * 100` | Percentage of successful key lookups. A high ratio is desirable. |
| **Evicted Keys Per Second** | `rate(redis_evicted_keys_total[5m])` | The rate of keys being evicted due to memory limits. Should ideally be 0. |
| **Average Command Latency (All)** | `rate(redis_commands_duration_seconds_total[5m]) / rate(redis_commands_total[5m])` | The average time for all commands to be processed by Redis. A good indicator of overall Redis health. |