# Deployment Guide

This guide covers deploying the Slack Consultation Skill in both local development and production (GKE) environments.

## Local Development Setup

### Prerequisites

- Java 17+
- Docker (for MongoDB)
- Slack app configured (see README.md)

### 1. Start MongoDB

```bash
docker run -d \
  --name slack-skill-mongo \
  -p 27017:27017 \
  mongo:6.0
```

To persist data across restarts:

```bash
docker run -d \
  --name slack-skill-mongo \
  -p 27017:27017 \
  -v slack-skill-data:/data/db \
  mongo:6.0
```

### 2. Configure the Application

Edit `.claude/config/slack-skill.yaml`:

```yaml
slack_bot_token: "xoxb-your-token"
slack_app_token: "xapp-your-token"
timeout_hours: 24

# MongoDB (uses defaults for local dev)
mongo_connection_string: "mongodb://localhost:27017"
mongo_database: "slack_skill"

# Health endpoints
health_port: 8080
heartbeat_interval_seconds: 30
heartbeat_stale_threshold_seconds: 90

user_aliases:
  john: "U123ABC"
log_level: INFO
```

### 3. Start the Server

```bash
./gradlew :server:run
```

### 4. Verify Health

```bash
# General health status
curl http://localhost:8080/health

# Kubernetes-style probes
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready

# Heartbeat status
curl http://localhost:8080/health/heartbeat
```

Expected responses:

```json
// /health
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00Z",
  "details": {
    "mongodb": "connected",
    "heartbeat": "healthy"
  }
}

// /health/live
{
  "status": "ok",
  "timestamp": "2024-01-15T10:30:00Z"
}

// /health/ready
{
  "status": "ready",
  "timestamp": "2024-01-15T10:30:00Z",
  "details": {
    "mongodb": "connected"
  }
}

// /health/heartbeat
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": "healthy",
  "isStale": false
}
```

### 5. Test the CLI

```bash
# With server running
./gradlew :cli:run --args="ask @youruser 'Test question'"

# Check status
./gradlew :cli:run --args="check"
```

---

## Production Deployment (GKE)

### Environment Variables

The following environment variables override config file values:

| Variable | Description | Required |
|----------|-------------|----------|
| `MONGODB_CONNECTION_STRING` | MongoDB connection URI | Yes |
| `MONGODB_DATABASE` | Database name | No (default: `slack_skill`) |

Slack tokens should be provided via Kubernetes Secrets mounted as files or environment variables.

### Kubernetes Manifests

#### Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: slack-skill-secrets
type: Opaque
stringData:
  slack-bot-token: "xoxb-your-production-token"
  slack-app-token: "xapp-your-production-token"
  mongodb-uri: "mongodb+srv://user:pass@cluster.mongodb.net/slack_skill"
```

#### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: slack-skill-config
data:
  slack-skill.yaml: |
    slack_bot_token: "${SLACK_BOT_TOKEN}"
    slack_app_token: "${SLACK_APP_TOKEN}"
    timeout_hours: 24
    health_port: 8080
    heartbeat_interval_seconds: 30
    heartbeat_stale_threshold_seconds: 90
    log_level: INFO
```

#### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: slack-skill-server
  labels:
    app: slack-skill
spec:
  replicas: 1  # Single replica recommended for Socket Mode
  selector:
    matchLabels:
      app: slack-skill
  template:
    metadata:
      labels:
        app: slack-skill
    spec:
      containers:
      - name: slack-skill
        image: your-registry/slack-skill:latest
        ports:
        - containerPort: 8080
          name: health
        env:
        - name: SLACK_BOT_TOKEN
          valueFrom:
            secretKeyRef:
              name: slack-skill-secrets
              key: slack-bot-token
        - name: SLACK_APP_TOKEN
          valueFrom:
            secretKeyRef:
              name: slack-skill-secrets
              key: slack-app-token
        - name: MONGODB_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: slack-skill-secrets
              key: mongodb-uri
        - name: MONGODB_DATABASE
          value: "slack_skill"
        volumeMounts:
        - name: config
          mountPath: /.claude/config
          readOnly: true
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
      volumes:
      - name: config
        configMap:
          name: slack-skill-config
```

#### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: slack-skill
spec:
  selector:
    app: slack-skill
  ports:
  - port: 8080
    targetPort: 8080
    name: health
  type: ClusterIP
```

### Liveness vs Readiness Probes

| Probe | Endpoint | Purpose |
|-------|----------|---------|
| Liveness | `/health/live` | Restarts pod if server is completely unresponsive |
| Readiness | `/health/ready` | Removes pod from service if MongoDB is disconnected |

### Scaling Considerations

**Important:** Slack Socket Mode maintains a persistent WebSocket connection. Running multiple replicas can cause duplicate message handling. Recommended approaches:

1. **Single Replica** (simplest): Run one pod, rely on Kubernetes for restart on failure
2. **Leader Election**: Implement leader election so only one instance handles events
3. **Message Deduplication**: Use MongoDB's unique index on request ID to prevent duplicates

### MongoDB Configuration

For production, use MongoDB Atlas or a managed MongoDB service:

1. Create a cluster with appropriate tier
2. Enable authentication
3. Configure network access (VPC peering or IP allowlist)
4. Create a database user with read/write access to `slack_skill` database
5. Use the connection string in your secret

Example Atlas connection string:
```
mongodb+srv://user:password@cluster0.xxxxx.mongodb.net/slack_skill?retryWrites=true&w=majority
```

### Monitoring

Health endpoints provide basic monitoring. For production, consider:

1. **Prometheus metrics**: Expose metrics endpoint
2. **Log aggregation**: Ship logs to Cloud Logging or similar
3. **Alerting**: Alert on:
   - Pod restarts
   - Readiness probe failures
   - Stale heartbeats (> 90s without update)

### Troubleshooting

#### Server not starting
```bash
# Check logs
kubectl logs -l app=slack-skill

# Check events
kubectl describe pod -l app=slack-skill
```

#### MongoDB connection issues
```bash
# Verify secret is mounted
kubectl exec -it <pod-name> -- env | grep MONGODB

# Test connection from pod
kubectl exec -it <pod-name> -- curl http://localhost:8080/health/ready
```

#### Slack connection issues
- Verify Socket Mode is enabled in Slack app settings
- Check that `xapp-` token has `connections:write` scope
- Ensure `xoxb-` token has all required bot scopes
