# Deployment to Azure Kubernetes Service (AKS)

This section provides the first steps for deploying the application to Azure Kubernetes Service (AKS).

## Prerequisites for Azure

*   An active Azure Account and Subscription.
*   [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed and configured.
*   `kubectl` command-line tool installed.
*   Container images (e.g., `hyeneung/user-server`) pushed to a container registry accessible by AKS (like Docker Hub or Azure Container Registry).

## Step 1: Set up Azure Infrastructure

This phase creates the core Azure resources required to run the application.

### 1.1. Login to Azure CLI

Authenticate with Azure. You will be prompted to log in via a web browser.

```bash
az login
```

### 1.2. Create a Resource Group

A resource group is a logical container for all your Azure resources.

```bash
az group create --name LiveChatResourceGroup --location koreacentral
```

### 1.3. Create the AKS Cluster

Provision a managed Kubernetes cluster with two nodes.

```bash
az aks create --resource-group LiveChatResourceGroup --name LiveChatAKSCluster --node-count 2 --enable-managed-identity --generate-ssh-keys
```
*Note: This process can take several minutes to complete.*

### 1.4. Configure kubectl

Download the cluster configuration and configure `kubectl` to connect to your new AKS cluster.

```bash
az aks get-credentials --resource-group LiveChatResourceGroup --name LiveChatAKSCluster
```
You can verify the connection is successful by running `kubectl get nodes`.

### 1.5: Install NGINX Ingress Controller (for Path-based Routing)

To enable path-based routing and consolidate external access for `chat-server` and `user-server` services, we will deploy the NGINX Ingress Controller. This allows us to use a single Azure Load Balancer for multiple services, reducing costs and simplifying management, especially when a custom domain is not available.

First, ensure your Kubernetes services (`chat-server-service`, `user-server-service`) are configured as `type: ClusterIP`. This was addressed by modifying `k8s/chat-server.yaml` and `k8s/user-server.yaml`.

Install the NGINX Ingress Controller using Helm:

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --create-namespace --namespace ingress-basic \
  --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz \
  --set controller.service.externalTrafficPolicy=Local
```

**Explanation of Helm parameters:**
*   `helm repo add ingress-nginx ...` and `helm repo update`: Adds and updates the official Helm chart repository for NGINX Ingress Controller.
*   `--create-namespace --namespace ingress-basic`: Installs the Ingress Controller into a new namespace named `ingress-basic`.
*   `--set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz`: This Azure-specific annotation configures the Azure Load Balancer's health probe to correctly check the status of the NGINX Ingress Controller, especially when `externalTrafficPolicy` is set to `Local`.
*   `--set controller.service.externalTrafficPolicy=Local`: Ensures that the client's source IP address is preserved when traffic reaches the Ingress Controller.

**Verification:**
Verify that the NGINX Ingress Controller pods are running:

```bash
kubectl get pods -n ingress-basic -l app.kubernetes.io/name=ingress-nginx
```

After the pods are running, find the external IP address of the Ingress Controller's service. This IP is the public entry point for your application.

```bash
kubectl get service -n ingress-basic
```
Look for the `EXTERNAL-IP` value for the `ingress-nginx-controller` service.

**Reference:** For more details on deploying NGINX Ingress Controller on AKS, refer to the Azure documentation:
[Create an ingress controller on Azure Kubernetes Service (AKS)](https://learn.microsoft.com/ko-kr/troubleshoot/azure/azure-kubernetes/load-bal-ingress-c/create-unmanaged-ingress-controller?tabs=azure-cli)

---

# Live Chat Analyzer - Kubernetes Deployment Plan (Local)

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

*   **Verification:**
    ```bash
    kubectl get pods -l app.kubernetes.io/name=zookeeper -w
    kubectl get pods -l app.kubernetes.io/name=kafka -w
    ```

#### 1.5. Initialize Kafka Topics

Run a Kubernetes Job to create the necessary topics in Kafka. This should be run after the Kafka brokers are available.

*   **File:** `k8s/kafka-topics-init-job.yaml`

```bash
kubectl apply -f k8s/kafka-topics-init-job.yaml
```

*   **Verification:** Check the logs of the job pod to ensure topics were created successfully.
    ```bash
    # Wait for the job to complete
    kubectl wait --for=condition=complete job/kafka-topics-init --timeout=120s
    # Check logs
    kubectl logs job/kafka-topics-init
    ```

#### 1.6. Apply Core ConfigMaps

Apply the core ConfigMaps that define configurations for various services, including the Cassandra schema definition.

*   **File:** `k8s/configmaps.yaml`

```bash
kubectl apply -f k8s/configmaps.yaml
```

*   **Verification:**
    ```bash
    kubectl get configmaps user-server-config chat-server-config cassandra-init-schema
    ```

#### 1.7. Apply Core Secrets

Apply the core Secrets that provide sensitive data for various services. Remember to replace placeholder values before applying.

*   **File:** `k8s/secrets.yaml`

```bash
kubectl apply -f k8s/secrets.yaml
```

*   **Verification:**
    ```bash
    kubectl get secret cassandra jwt-secret spark-openai-secret
    ```
    *   **Note:** The `mysql` and `redis` secrets are created by their respective Helm charts.

#### 1.8. Deploy Cassandra

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

#### 1.9. Initialize Cassandra Schema

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

### Phase 2: Monitoring and Application Deployments

This phase deploys the monitoring stack and the core application services.

**Important Note:** Before proceeding with this phase, ensure you have built and pushed the custom Docker images as outlined in `DOCKER_BUILD_PLAN.md`.

#### 2.1. Deploy Monitoring Stack

Deploy Prometheus for metrics collection, Kafka Exporter to expose Kafka metrics, and Grafana for visualization.

*   **Files:** `k8s/prometheus.yaml`, `k8s/kafka-exporter.yaml`, `k8s/grafana.yaml`

```bash
kubectl apply -f k8s/prometheus.yaml
kubectl apply -f k8s/kafka-exporter.yaml
kubectl apply -f k8s/grafana.yaml
```

*   **Verification & Access:**
    *   Check that monitoring pods are running: `kubectl get pods -l app=prometheus,app=kafka-exporter,app=grafana`
    *   The Grafana service is of type `NodePort`. Find the port and access it via `http://<NODE_IP>:<PORT>` or `http://localhost:<PORT>` if using port-forwarding.
        ```bash
        kubectl get svc/grafana-service
        ```

#### 2.2. Deploy `user-server`

*   **File:** `k8s/user-server.yaml`

```bash
kubectl apply -f k8s/user-server.yaml
```

#### 2.3. Deploy `chat-server`

*   **File:** `k8s/chat-server.yaml`

```bash
kubectl apply -f k8s/chat-server.yaml
```

#### 2.4. Deploy Ingress with TLS (HTTPS)
To expose the backend services to the internet with a custom domain and secure them with HTTPS, we will deploy an Ingress resource and use `cert-manager` to automatically handle TLS certificates.

##### 2.4.1. Install cert-manager
Install `cert-manager` into your cluster using its official Helm chart. This tool will automatically provision and renew TLS certificates from Let's Encrypt.
```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.14.5 \
  --set installCRDs=true
```
**Verification:**
Wait for the `cert-manager` pods to be running in the `cert-manager` namespace.
```bash
kubectl get pods -n cert-manager
```

##### 2.4.2. Create a ClusterIssuer
Create a `ClusterIssuer` resource to tell `cert-manager` how to issue certificates. This requires creating a `k8s/cluster-issuer.yaml` file and applying it. Remember to add your email address to the file.
```bash
kubectl apply -f k8s/cluster-issuer.yaml
```

##### 2.4.3. Create and Deploy the Ingress Resource
Apply the final `k8s/ingress.yaml` file, which defines routing rules and requests a TLS certificate.
```bash
kubectl apply -f k8s/ingress.yaml
```
**Verification:**
1.  **Check Ingress Status:** Get the Public IP of the Ingress Controller.
    ```bash
    kubectl get ingress path-based-ingress
    ```
2.  **Check Certificate Status:**
    ```bash
    kubectl describe certificate api-live-streaming-store-tls
    ```
3.  **Test HTTPS Routing:** Once ready, access your API via `https://api.live-streaming.store/users`.

#### 2.5. Deploy Flink Cluster
Deploy Flink JobManager and TaskManager.
*   **File:** `k8s/flink-jobmanager.yaml`, `k8s/flink-taskmanager.yaml`
```bash
kubectl apply -f k8s/flink-jobmanager.yaml
kubectl apply -f k8s/flink-taskmanager.yaml
```

#### 2.6. Deploy Flink Sentiment Analyzer Application
Deploy the Flink Python application.
*   **File:** `k8s/flink-sentiment-analyzer.yaml`
```bash
kubectl apply -f k8s/flink-sentiment-analyzer.yaml
```

#### 2.7. Deploy Kafka Connect Cluster
Deploy the Kafka Connect service.
*   **File:** `k8s/kafka-connect.yaml`
```bash
kubectl apply -f k8s/kafka-connect.yaml
```

#### 2.8. Deploy Kafka Connect Init Job
Run a Kubernetes Job to register the Cassandra Sink connector.
*   **File:** `k8s/kafka-connect-init-job.yaml`
```bash
kubectl apply -f k8s/kafka-connect-init-job.yaml
```

#### 2.9. Deploy Spark Cluster
Deploy Spark Master and Worker.
*   **File:** `k8s/spark-master.yaml`, `k8s/spark-worker.yaml`
```bash
kubectl apply -f k8s/spark-master.yaml
kubectl apply -f k8s/spark-worker.yaml
```

#### 2.10. Deploy Spark Summarizer Application
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
*   **Success Criteria:** All pods are running without `CrashLoopBackOff` or `Error` states. All init jobs should show `Completed`.

#### 3.2. Check Service Connectivity
Verify that services are accessible within the cluster.
```bash
kubectl get services
```
*   **Success Criteria:** All expected services are listed and have appropriate ClusterIPs or NodePorts.
