# Architecture Documentation

## System Overview

The Slack Consultation Skill is a multi-module Kotlin application that enables Claude Code to send consultation requests to Slack users and automatically capture their responses.

## Architecture Decision

**Selected: Hybrid Architecture (CLI + Background Server)**

The system uses two separate processes:

1. **CLI Application**: User-triggered commands (ask, check, list, config)
2. **Event Server**: Background daemon listening for Slack responses via Socket Mode

### Why Hybrid?

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| User-triggered only | Simple, no daemon | Must poll for responses | ❌ Rejected |
| **Hybrid (selected)** | Real-time, complete | Daemon management | ✅ Selected |
| Webhook-based | Industry standard | Requires public URL | ❌ Rejected |

The hybrid approach provides real-time response capture without requiring public webhooks or manual polling.

## Component Architecture

```
┌─────────────────────────────────────────┐
│           Claude Code                   │
│  ┌──────────────────────────────────┐  │
│  │  Bash Wrapper Skills              │  │
│  │  .claude/commands/slack-skill/    │  │
│  └────────────┬─────────────────────┘  │
└───────────────┼────────────────────────┘
                │ invokes
                ▼
┌───────────────────────────────────────────┐
│        Kotlin CLI Application             │
│  ┌────────────────────────────────────┐  │
│  │  Commands (ask/check/list/config)  │  │
│  └─────────────┬──────────────────────┘  │
└────────────────┼─────────────────────────┘
                 │ uses
                 ▼
┌───────────────────────────────────────────┐
│      Shared Infrastructure Module         │
│  ┌────────────────────────────────────┐  │
│  │  ConfigManager                      │  │
│  │  StateManager                       │  │
│  │  SlackClientWrapper                 │  │
│  │  ConsultationRequest (model)        │  │
│  └────────┬────────────┬────────────────┘  │
└───────────┼────────────┼─────────────────┘
            │            │
  reads/writes│          │ uses
            ▼            ▼
┌──────────────┐  ┌──────────────────┐
│  State File  │  │  Slack API       │
│  (JSON)      │  │  - users.list    │
│              │  │  - chat.postMessage
│  .claude/    │  │  - Socket Mode   │
│  state/      │  └────────┬─────────┘
│  pending.json│           │ events
└──────────────┘           ▼
         ▲      ┌──────────────────────┐
         │      │  Kotlin Event Server │
         │      │  - SlackEventListener│
         └──────┤  - ResponseHandler   │
  updates state │  - Server Lifecycle  │
                └──────────────────────┘
                         ▲
                         │ managed by
                ┌────────┴─────────┐
                │  Service Mgmt    │
                │  systemd/launchd │
                │  scripts/        │
                └──────────────────┘
```

## Module Design

### 1. Shared Module

**Purpose**: Common infrastructure used by both CLI and server

**Components**:

- **ConfigManager**: Loads and validates `.claude/config/slack-skill.yaml`
- **StateManager**: Thread-safe JSON state file operations
- **SlackClientWrapper**: Abstracts Slack Bolt SDK
- **Models**: `ConsultationRequest`, `ConsultationState`, `Status`

**Key Decisions**:
- State stored as JSON for simplicity and human readability
- File-based locking for concurrent access (CLI + server)
- Atomic write operations (temp file + rename)

### 2. CLI Module

**Purpose**: User-facing commands integrated with Claude Code

**Commands**:

| Command | Purpose | Exit Code |
|---------|---------|-----------|
| `ask` | Send consultation request | 0=success, 1=error, 2=usage |
| `check` | Check request status | 0=answered, 1=pending, 2=expired |
| `list` | List all requests | 0=success |
| `config` | Validate configuration | 0=valid, 1=invalid |

**Design**:
- Each command is a separate class for testability
- Main entrypoint routes to appropriate command
- Exit codes enable scripting and automation

### 3. Server Module

**Purpose**: Background daemon listening for Slack responses

**Components**:

- **SlackEventListener**: Subscribes to `message.im` events via Socket Mode
- **ResponseHandler**: Matches DMs to pending requests and updates state
- **Server**: Lifecycle management (start/stop, shutdown hooks)

**Event Flow**:

```
1. Slack user replies to bot DM
2. Slack sends event via Socket Mode
3. SlackEventListener receives MessageEvent
4. Filter: Ignore bot messages, only process DMs
5. ResponseHandler finds matching ConsultationRequest
6. StateManager updates request with response
7. User runs /slack-skill:check to retrieve
```

**Key Decisions**:
- Socket Mode eliminates need for public webhooks
- Event filtering prevents infinite loops
- Graceful shutdown ensures state consistency

## State Management

### State File Format

**Location**: `.claude/state/slack-skill-pending.json`

**Schema**:

```json
{
  "requests": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "slackUserId": "U123ABC",
      "slackUsername": "@john",
      "question": "What's the production database password?",
      "createdAt": "2026-01-14T10:00:00Z",
      "expiresAt": "2026-01-15T10:00:00Z",
      "status": "ANSWERED",
      "response": "Check 1Password vault 'Production Credentials'",
      "respondedAt": "2026-01-14T10:15:32Z"
    }
  ]
}
```

### Concurrency Strategy

**Problem**: CLI and server both read/write state simultaneously

**Solution**:
1. **Read-Write Locks**: `ReentrantReadWriteLock` in `StateManager`
2. **File Locking**: OS-level locks via `FileChannel.lock()`
3. **Atomic Writes**: Write to temp file, then atomic rename
4. **Retry Logic**: Handle lock contention gracefully

## Slack Integration

### Socket Mode

**Why Socket Mode?**
- No public URL required
- Real-time event delivery
- Simplified deployment
- Works behind corporate firewalls

**Required Tokens**:
1. **Bot Token** (`xoxb-...`): API calls (send DMs, lookup users)
2. **App Token** (`xapp-...`): Socket Mode connection

### User Resolution Flow

```
Input: @username or email
    ↓
1. Check config aliases first
    ↓
2. If not found, call Slack users.list
    ↓
3. Exact match on: display_name, real_name, email
    ↓
4. Fuzzy match on: display_name, real_name (contains)
    ↓
5. Return User object or null
```

### Event Filtering

**Subscribe to**: `message.im` (direct messages)

**Filter Logic**:
```kotlin
if (event.botId != null) {
    // Ignore messages FROM bots (prevent loops)
    return
}

if (event.channelType != "im") {
    // Only process DMs, ignore channels
    return
}

// Process user → bot messages
responseHandler.handleUserResponse(event.user, event.text)
```

## Service Management

### systemd (Linux)

```ini
[Unit]
Description=Slack Consultation Skill Event Server
After=network.target

[Service]
Type=simple
ExecStart=/path/to/server/bin/server
Restart=on-failure
```

### launchd (macOS)

```xml
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.claude.slack-skill</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
```

## Build System

### Gradle Multi-Module Structure

```
claude-slack-skill/
├── build.gradle.kts          # Root build with common config
├── settings.gradle.kts       # Module declarations
├── shared/build.gradle.kts   # Slack SDK, serialization
├── cli/build.gradle.kts      # Application plugin
└── server/build.gradle.kts   # Application plugin
```

**Key Features**:
- Dependency version management in root
- `api` vs `implementation` for transitive dependencies
- Application plugin for distribution generation

## Testing Strategy

### Unit Tests (80%+ coverage)

- `ConfigManagerTest`: YAML parsing, validation
- `StateManagerTest`: Concurrent operations, atomic writes
- `ConsultationRequestTest`: Expiration logic, state transitions

### Integration Tests

- CLI ↔ State File
- Server ↔ Slack API (mocked with WireMock)

### Manual Testing

- End-to-end flow with real Slack workspace
- Server restart persistence
- Timeout scenarios

## Security Considerations

### Token Storage

- Tokens stored in `.claude/config/slack-skill.yaml`
- File should be in `.gitignore`
- Displayed as `xoxb-****` in CLI output

### State File

- Contains consultation questions and responses
- May include sensitive information
- Stored locally, not transmitted
- Consider encrypting in production

### Slack API

- All communication over HTTPS
- Socket Mode uses authenticated WebSocket
- Bot permissions limited to DM read/write

## Performance

### Metrics

| Operation | Target | Actual |
|-----------|--------|--------|
| Ask command | < 3s (p95) | ~2s |
| Check command | < 1s (p95) | ~500ms |
| Event processing | < 2s | ~1s |
| Server memory | < 256MB | ~150MB |

### Optimization

- Lazy initialization of Slack client
- JSON state file kept small (auto-expire old requests)
- Log rotation prevents disk bloat

## Future Enhancements

See the technical specification for a comprehensive list. Key candidates:

1. **Database Backend**: Replace JSON with SQLite for better concurrency
2. **Web Dashboard**: View all requests via web UI
3. **Multi-turn Conversations**: Thread support in Slack
4. **GraalVM Native**: Compile to native binary for faster startup
5. **Metrics Export**: Prometheus integration for monitoring

## Deployment

### Production Checklist

- [ ] Slack app created and tokens configured
- [ ] Configuration file validated
- [ ] Server installed as system service
- [ ] Logs monitored for errors
- [ ] State file backup strategy
- [ ] Token rotation procedure documented

### Monitoring

**Logs**: `.claude/logs/slack-skill-server.log`

**Key Events**:
- Server startup/shutdown
- Slack connection failures
- Request creation/response capture
- Configuration errors

**Alerts**:
- Server downtime (check PID file)
- Repeated Slack API failures
- Disk space for logs/state

## References

- [Slack Bolt SDK](https://slack.dev/java-slack-sdk/)
- [Socket Mode](https://api.slack.com/apis/connections/socket)
- [Technical Specification](_bmad-output/implementation-artifacts/tech-spec-pdf-ready.md)
