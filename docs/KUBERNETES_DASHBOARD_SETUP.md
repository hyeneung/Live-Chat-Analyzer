# Kubernetes Dashboard Setup Guide

This document explains the commands used to set up and access the official Kubernetes Dashboard.

## Command Overview

The setup process involves four main commands. The first three commands configure the dashboard in the cluster, and the final command allows you to access it from your local machine.

---

### 1. Deploy the Dashboard

**Command:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
```

**Purpose:**
*   This command downloads the official recommended configuration file for the Kubernetes Dashboard (version 2.7.0 in this case).
*   It then applies this configuration to your cluster.
*   This creates all the necessary Kubernetes resources for the dashboard to run, including:
    *   A new `kubernetes-dashboard` namespace.
    *   The `Deployment` for the dashboard application itself.
    *   The `Service` to expose the dashboard within the cluster.
    *   Necessary `ServiceAccounts`, `Roles`, and `RoleBindings` for the dashboard to function with minimal permissions.

---

### 2. Create an Admin User for the Dashboard

**Command:**
```bash
kubectl apply -f dashboard-adminuser.yaml
```

**Purpose:**
*   This command applies a custom configuration file (`dashboard-adminuser.yaml`) to create a user with administrative privileges, specifically for accessing the dashboard.
*   It creates two resources:
    1.  `ServiceAccount` named `admin-user`: This acts as the "user" identity that you will use to log in.
    2.  `ClusterRoleBinding` named `admin-user`: This gives the `admin-user` `ServiceAccount` the permissions of the `cluster-admin` `ClusterRole`. The `cluster-admin` role is a default, built-in role that has superuser access to the entire cluster.
*   **Why this is needed:** By default, the dashboard is deployed with minimal permissions for security reasons. This step explicitly creates a powerful user so you can view and manage everything in the cluster through the dashboard.

---

### 3. Generate a Login Token

**Command:**
```bash
kubectl -n kubernetes-dashboard create token admin-user
```

**Purpose:**
*   This command generates a temporary authentication token for the `admin-user` `ServiceAccount` that was created in the previous step.
*   The dashboard's login screen does not use a traditional username and password. Instead, it uses bearer tokens for authentication.
*   You will copy the output of this command and paste it into the "Token" field on the dashboard's login page to prove you are the `admin-user`.

---

### 4. Access the Dashboard

**Command:**
```bash
kubectl proxy
```

**Purpose:**
*   This command starts a secure proxy server on your local machine (by default, on `localhost:8001`).
*   This proxy authenticates to the Kubernetes API server and forwards traffic from your local machine to the services running inside the cluster.
*   **Crucially, it does not expose your cluster to the public internet.** It only allows local access.
*   While this command is running, you can access cluster services through your browser via special `localhost:8001` URLs. This is how you are able to reach the dashboard, which is running inside the cluster, from your local web browser.
*   This command must be kept running in a terminal for as long as you want to access the dashboard.
