# Live Chat Analyzer - Kubernetes Deployment Plan

This document outlines the step-by-step process for deploying the Live Chat Analyzer application and its infrastructure components to a local Kubernetes cluster (e.g., Docker Desktop Kubernetes). It covers Helm chart installations, Kubernetes configuration management using ConfigMaps and Secrets, and application deployments.

## Prerequisites

*   **Kubernetes Cluster:** A running Kubernetes cluster (e.g., Docker Desktop Kubernetes).
*   **kubectl:** Kubernetes command-line tool configured to connect to your cluster.
*   **Helm:** Kubernetes package manager installed.
*   **Git:** Git installed for cloning the repository.
*   **Docker & Docker Hub Account:** Required for custom image builds (see `DOCKER_BUILD_PLAN.md`).
*   **OpenSSL:** For generating secrets (if not using existing ones).

## Deployment Phases

### Phase 1: Core Infrastructure Setup

This phase deploys essential infrastructure components like MySQL, Redis, Kafka, Cassandra, and their initial configurations.

**Important Note on Bitnami Images:**
As of August 28, 2025, Bitnami is migrating images previously hosted on `registry.public.bitnami.com` to a separate "Bitnami Legacy" repository. This may cause DNS issues or restricted image access for the existing address. To ensure stable deployments, it is recommended to use images from the `bitnamilegacy` repository.

#### 1.1. Add Helm Repositories

Ensure you have the necessary Helm repositories added:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add confluentinc https://packages.confluent.io/helm
helm repo update
```

#### 1.2. Deploy MySQL

Deploy MySQL using the Bitnami Helm chart.

*   **Kubernetes Service Name:** `mysql`

```bash
helm install mysql bitnami/mysql \
  --set auth.rootPassword=root_password_shown_in_github \
  --set auth.database=live_chat \
  --set image.repository=bitnamilegacy/mysql \
  --set primary.persistence.enabled=false # For local development, disable persistence
```
helm install mysql bitnami/mysql --set auth.rootPassword=root_password_shown_in_github --set auth.database=live_chat --set image.repository=bitnamilegacy/mysql --set primary.persistence.enabled=false

*   **Verification:**
    ```bash
    kubectl get pods -l app.kubernetes.io/name=mysql
    kubectl get secret mysql -o jsonpath='{.data.mysql-root-password}' | base64 --decode # Verify password
    ```

#### 1.3. Deploy Redis

Deploy Redis using the Bitnami Helm chart.

*   **Kubernetes Service Name:** `redis-master`

```bash
helm install redis bitnami/redis \
  --set auth.password=Szhj3bfhe4O \
  --set image.repository=bitnamilegacy/redis \
  --set master.persistence.enabled=false # For local development, disable persistence
```

helm install redis bitnami/redis --set auth.password=Szhj3bfhe4O --set image.repository=bitnamilegacy/redis --set master.persistence.enabled=false

*   **Verification:**
    ```bash
    kubectl get pods -l app.kubernetes.io/name=redis
    kubectl get secret redis -o jsonpath='{.data.redis-password}' | base64 --decode # Verify password
    ```

#### 1.4. Deploy Kafka

Deploy Kafka using the Bitnami Helm chart. This will also deploy Zookeeper as a dependency.

*   **Kubernetes Service Name:** `kafka`

```bash
helm install kafka bitnami/kafka \
  --set listeners.client.protocol=PLAINTEXT \
  --set listeners.interbroker.protocol=PLAINTEXT \
  --set listeners.controller.protocol=PLAINTEXT \
  --set listeners.external.protocol=PLAINTEXT \
  --set sasl.enabled=false \
  --set zookeeper.persistence.enabled=false \
  --set persistence.enabled=false \
  --set replicaCount=1 \
  --set image.repository=bitnamilegacy/kafka \
  --set zookeeper.image.repository=bitnamilegacy/zookeeper \
  --set controller.resources.requests.memory="1Gi" \
  --set controller.resources.limits.memory="1.5Gi"
```
helm install kafka bitnami/kafka --set listeners.client.protocol=PLAINTEXT --set listeners.interbroker.protocol=PLAINTEXT --set listeners.controller.protocol=PLAINTEXT --set listeners.external.protocol=PLAINTEXT --set sasl.enabled=false --set zookeeper.persistence.enabled=false --set persistence.enabled=false --set replicaCount=1 --set image.repository=bitnamilegacy/kafka --set zookeeper.image.repository=bitnamilegacy/zookeeper --set controller.resources.requests.memory="1Gi" --set controller.resources.limits.memory="1.5Gi"

*   **Verification:**
    ```bash
    kubectl get pods -l app.kubernetes.io/name=zookeeper
    ```

#### 1.5. Apply Core ConfigMaps

Apply the core ConfigMaps that define configurations for various services, including the Cassandra schema definition.

*   **File:** `k8s/configmaps.yaml`

```bash
kubectl apply -f k8s/configmaps.yaml
```

*   **Verification:**
    ```bash
    kubectl get configmaps user-server-config chat-server-config cassandra-init-schema
    ```

#### 1.6. Apply Core Secrets

Apply the core Secrets that provide sensitive data for various services. Remember to replace placeholder values before applying.

*   **File:** `k8s/secrets.yaml`

```bash
kubectl apply -f k8s/secrets.yaml
```

*   **Verification:**
    ```bash
    kubectl get secret cassandra jwt-secret spark-openai-secret
    kubectl get secret cassandra -o jsonpath='{.data.cassandra-password}' | base64 --decode
    kubectl get secret jwt-secret -o jsonpath='{.data.jwt-secret-key}' | base64 --decode
    kubectl get secret spark-openai-secret -o jsonpath='{.data.OPENAI_API_KEY}' | base64 --decode
    ```
    *   **Note:** The `mysql` and `redis` secrets are created by their respective Helm charts.

#### 1.7. Deploy Cassandra

Deploy Cassandra using a custom Kubernetes StatefulSet.

*   **Kubernetes Service Name:** `cassandra-service`, `cassandra-client-service`

```bash
kubectl apply -f k8s/cassandra.yaml
```

*   **Wait for Cassandra Pod Readiness:**
    Monitor the `cassandra-0` pod until it is in a `Running` and `Ready` state.

    ```bash
    kubectl get pods -l app=cassandra -w
    ```

#### 1.8. Initialize Cassandra Schema

Run a Kubernetes Job to apply the Cassandra schema using the ConfigMap created earlier.

*   **File:** `k8s/cassandra-schema-job.yaml`

```bash
kubectl apply -f k8s/cassandra-schema-job.yaml
```

*   **Verification:**
    1.  **Check Job Pod Logs:**
        Get the name of the `cassandra-schema-init` job pod:
        ```bash
        kubectl get pods -l job-name=cassandra-schema-init
        ```
        Then check its logs:
        ```bash
        kubectl logs <cassandra-schema-init-pod-name>
        ```
        Look for "Applying schema..." and an Exit Code of 0.

    2.  **Verify Schema in Cassandra:**
        Connect to Cassandra using `cqlsh` and verify the `chat_data` keyspace and `messages` table.
        ```bash
        kubectl exec -it cassandra-0 -- cqlsh -u cassandra -p cassandra
        ```
        Once in `cqlsh`:
        ```cqlsh
        DESCRIBE KEYSPACES;
        USE chat_data;
        DESCRIBE TABLES;
        SELECT * FROM messages LIMIT 10;
        ```

### Phase 2: Application Deployments

This phase deploys the core application services and the Flink, Kafka Connect, and Spark clusters.

**Important Note:** Before proceeding with this phase, ensure you have built and pushed the custom Docker images as outlined in `DOCKER_BUILD_PLAN.md`.

#### 2.1. Deploy `user-server`

*   **File:** `k8s/user-server.yaml`

```bash
kubectl apply -f k8s/user-server.yaml
```

#### 2.2. Deploy `chat-server`

*   **File:** `k8s/chat-server.yaml`

```bash
kubectl apply -f k8s/chat-server.yaml
```

#### 2.3. Deploy Flink Cluster

Deploy Flink JobManager and TaskManager.

*   **File:** `k8s/flink-jobmanager.yaml`, `k8s/flink-taskmanager.yaml`

```bash
kubectl apply -f k8s/flink-jobmanager.yaml
kubectl apply -f k8s/flink-taskmanager.yaml
```

#### 2.4. Deploy Flink Sentiment Analyzer Application

Deploy the Flink Python application.

*   **File:** `k8s/flink-sentiment-analyzer.yaml`

```bash
kubectl apply -f k8s/flink-sentiment-analyzer.yaml
```

#### 2.5. Deploy Kafka Connect Cluster

Deploy the Kafka Connect service.

*   **File:** `k8s/kafka-connect.yaml`

```bash
kubectl apply -f k8s/kafka-connect.yaml
```

#### 2.6. Deploy Kafka Connect Init Job

Run a Kubernetes Job to register the Cassandra Sink connector.

*   **File:** `k8s/kafka-connect-init-job.yaml`

```bash
kubectl apply -f k8s/kafka-connect-init-job.yaml
```

#### 2.7. Deploy Spark Cluster

Deploy Spark Master and Worker.

*   **File:** `k8s/spark-master.yaml`, `k8s/spark-worker.yaml`

```bash
kubectl apply -f k8s/spark-master.yaml
kubectl apply -f k8s/spark-worker.yaml
```

#### 2.8. Deploy Spark Summarizer Application

Deploy the Spark Python application.

*   **File:** `k8s/spark-summarizer.yaml`

```bash
kubectl apply -f k8s/spark-summarizer.yaml
```

### Phase 3: Verification

Monitor the status of all deployed pods and services.

#### 3.1. Monitor Pod Status

Continuously check the status of all pods until they are in a `Running` or `Completed` state.

```bash
kubectl get pods -w
```

*   **Success Criteria:** All pods related to MySQL, Redis, Kafka, Zookeeper, Flink (JobManager, TaskManager, Sentiment Analyzer), Kafka Connect, Spark (Master, Worker, Summarizer), `user-server`, and `chat-server` are running without `CrashLoopBackOff` or `Error` states. Kafka Connect Init Job should show `Completed`.

#### 3.2. Check Service Connectivity

Verify that services are accessible within the cluster.

```bash
kubectl get services
```

*   **Success Criteria:** All expected services (e.g., `mysql`, `redis-master`, `kafka`, `flink-jobmanager-service`, `kafka-connect-service`, `spark-master-service`, `user-server-service`, `chat-server-service`) are listed and have appropriate ClusterIPs.

---