# Roadside Assistance (RSA) Platform — Startup Guide

This guide describes how to run and deploy the AI-powered Roadside Assistance (RSA) platform. The application is configured to run locally or orchestrate on **Docker Desktop Kubernetes**.

---

## 📋 Prerequisites

Before starting, ensure you have the following tools installed:
- **Java 21** (or 26)
- **Maven** (3.8+)
- **Node.js** (v18+) and **npm**
- **Docker Desktop** (with **Kubernetes** enabled in Settings -> Kubernetes)
- **Ollama** (Local LLM service)
- **kubectl** (installed automatically with Docker Desktop)

---

## 🛠️ Step 1: Initialize Local LLM & Embeddings (Ollama)

1. Make sure the Ollama desktop application is running.
2. Pull the required models:
   ```bash
   # Pull the reasoning chat model
   ollama pull qwen2.5:7b

   # Pull the embedding model (used for RAG knowledge database)
   ollama pull nomic-embed-text
   ```

---

## 💻 Option A: Run Locally (Development Mode)

If you want to run the application directly on your machine:

### 1. Start Qdrant Vector Database
Use Docker Compose to launch Qdrant in the background:
```bash
docker-compose up -d
```
*This starts Qdrant REST on port `6333` and gRPC on `6334`.*

### 2. Build the Angular SPA Frontend
Navigate to the `frontend/` directory, install dependencies, and build the application:
```bash
cd frontend
npm install
npm run build
cd ..
```
*This builds the Angular SPA and outputs the production assets directly into the Spring Boot `/src/main/resources/static` directory.*

### 3. Run the Spring Boot Application
Compile and start the backend:
```bash
mvn spring-boot:run
```
Once started:
- Access the dashboard in your browser at: [http://localhost:8080](http://localhost:8080)
- The app will automatically connect to Qdrant, embed the Markdown files in `src/main/resources/knowledge/`, and set up WebSocket brokers.

---

## ☸️ Option B: Deploy to Docker Desktop Kubernetes

When using **Docker Desktop Kubernetes**, the local Docker image registry is shared directly with the Kubernetes cluster context. There is no need for local docker-env redirection or pushing images to registries.

### 1. Set Kubernetes context to Docker Desktop
Verify that your active context is set to Docker Desktop:
```bash
kubectl config use-context docker-desktop
```

### 2. Build the Frontend and Docker Image
Compile the Angular UI so it is packaged inside the Spring Boot container, then trigger the Docker build:
```bash
# Build frontend assets
cd frontend && npm install && npm run build && cd ..

# Build container image
docker build -t incident-commander:latest .
```

### 3. Deploy Manifests to Kubernetes
Deploy all configurations (database, pvc, backend, hpa):
```bash
# 1. Deploy Qdrant PVC, Service, and Deployment
kubectl apply -f k8s/qdrant-deployment.yaml

# 2. Deploy Spring Boot Backend Deployment & Service
kubectl apply -f k8s/spring-boot-deployment.yaml
kubectl apply -f k8s/spring-boot-service.yaml

# 3. Configure the Horizontal Pod Autoscaler (HPA) rules
kubectl apply -f k8s/hpa.yaml
```

### 4. Access the Application
Since the Spring Boot Service is configured as a `LoadBalancer` type, Docker Desktop automatically maps it to your host. 

Simply open your browser and navigate to:
👉 [**http://localhost:8080**](http://localhost:8080)

*No port forwarding or extra setup needed!*

### 5. Monitor Autoscaling & Replicas
Verify the status of your running pods and autoscaler (minimum 1 pod, scaling up to 2 max):
```bash
kubectl get pods
kubectl get hpa
```
