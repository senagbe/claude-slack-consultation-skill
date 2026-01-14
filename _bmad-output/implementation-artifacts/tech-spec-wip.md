---
title: 'Slack Consultation Skill for Claude Code'
slug: 'slack-consultation-skill'
created: '2026-01-14'
status: 'ready-for-implementation'
stepsCompleted: [1, 2, 3]
tech_stack: ['Kotlin', 'Gradle', 'Slack Bolt SDK Socket Mode', 'Bash', 'systemd/launchd']
architecture: 'hybrid-cli-server'
modules: ['shared', 'cli', 'server']
files_to_modify: []
code_patterns: []
test_patterns: []
---

# Tech-Spec: Slack Consultation Skill for Claude Code

**Created:** 2026-01-14

## Overview

### Problem Statement

When Claude needs external input from subject matter experts during a conversation, the current workflow halts. There's no mechanism for Claude to request asynchronous consultation from humans via their preferred communication channel (Slack), forcing users to manually reach out and relay information back.

### Solution

A Claude Code skill (`/slack-skill:ask`) that allows users to request Slack-based consultation. The skill sends a question to a specified Slack user via DM, waits up to 24 hours for a response, and stores the response in a retrievable location. The system uses Slack API for user lookup and message delivery, file-based state tracking for pending requests, and gracefully handles timeouts without blocking the conversation.

### Scope

**In Scope:**
- Multi-module Kotlin project: CLI + background event server
- Bash wrapper skills: `/slack-skill:ask`, `/slack-skill:check`, `/slack-skill:list`, `/slack-skill:config`, `/slack-skill:server`
- Kotlin CLI (Gradle-based) with Slack Bolt SDK integration
- Background event server using Slack Socket Mode for real-time response capture
- Slack user resolution via Slack API (users.list, search by name/email)
- Send question as Slack DM to resolved user
- Automatic response capture when Slack user replies to bot DM
- File-based state management with concurrent access safety (`.claude/state/slack-skill-pending.json`)
- 24-hour timeout with expiration tracking (configurable)
- Service management (systemd/launchd) with auto-start on boot
- Auto-installation of dependencies and services on first run
- Configuration file: `.claude/config/slack-skill.yaml` (tokens, aliases, timeout)

**Out of Scope:**
- Autonomous Claude invocation (server only captures responses, doesn't trigger Claude)
- Multi-channel notifications or group DMs (only 1:1 DMs for now)
- Agent-to-agent consultation (human-only in v1)
- Slack workspace management or admin features
- Response threading or full conversation history tracking
- Integration with other messaging platforms (Discord, Teams, etc.)
- Native binary compilation (GraalVM) - future enhancement
- Web dashboard or GUI for managing consultations

## Context for Development

### Codebase Patterns

This is a brand new project. The codebase will follow these patterns:
- Claude Code skills are markdown files in `.claude/commands/{namespace}/` with frontmatter + `!bash` directives
- Skills are hot-reloaded automatically (Claude Code 2026 feature)
- BMAD framework is installed for workflow orchestration

### Files to Reference

| File | Purpose |
| ---- | ------- |
| `.mcp.json` | MCP server configuration (currently empty) |

### Technical Decisions

**Language & Build:**
- **Kotlin** for all source code (user preference: JVM comfort)
- **Gradle with wrapper** for dependency management
- **Slack Bolt SDK for Java** (MIT license, actively maintained)
- Standard JVM execution (no native compilation in v1)

**Architecture:**
- **Hybrid architecture** - User-triggered CLI + Background event server
- **Server Component**: Lightweight Kotlin daemon using Slack Bolt SDK Socket Mode
- **CLI Component**: Bash wrapper invokes Kotlin CLI application
- **Process Management**: systemd (Linux) / launchd (macOS) for daemon lifecycle
- First-run auto-installation via `./gradlew installDist`
- Multi-module Gradle project: `shared`, `cli`, `server`

**State & Configuration:**
- **State**: JSON files in `.claude/state/slack-skill-pending.json` with file locking for concurrent access
- **Config**: YAML in `.claude/config/slack-skill.yaml`
- Slack bot token (xoxb-) for API calls
- Slack app token (xapp-) for Socket Mode event listening
- Optional user alias mappings (username → email/slack-id)
- Configurable timeout (default 24 hours)

**Slack Integration:**
- Use Slack Bolt SDK for API access and event listening
- **Socket Mode**: Real-time event delivery without public webhooks
- User lookup via `users.list` + fuzzy matching on display name/real name
- Fallback to email-based lookup if configured in aliases
- Send DMs via `chat.postMessage` to user's DM channel
- Listen for `message.im` events to capture responses
- Filter bot's own messages to prevent loops

**Timeout Handling:**
- Default: 24 hours (86400 seconds)
- Store `expires_at` timestamp in state
- `/slack-skill:check` command reports status: pending/answered/expired
- Expired requests don't block - return "No response received" status

## Implementation Plan

### Phase 1: Project Foundation

1. **Initialize Gradle/Kotlin multi-module project**
   - Root `settings.gradle.kts` with three modules: `shared`, `cli`, `server`
   - Shared `buildSrc` for common dependencies
   - `cli/build.gradle.kts` - application plugin for CLI tools
   - `server/build.gradle.kts` - application plugin for daemon service
   - Dependencies: Slack Bolt SDK, kotlinx.serialization, SnakeYAML, SLF4J/Logback

2. **Create directory structure**
   - `shared/src/main/kotlin/com/claude/slack/shared/` (config, state, models)
   - `cli/src/main/kotlin/com/claude/slack/cli/`
   - `server/src/main/kotlin/com/claude/slack/server/`
   - `.claude/commands/slack-skill/`
   - `.claude/config/`
   - `.claude/state/`
   - `.claude/logs/`
   - `scripts/` (installation, service management)

### Phase 2: Shared Infrastructure

3. **Build configuration manager** (shared module)
   - Read `.claude/config/slack-skill.yaml`
   - Validate: `slack_bot_token`, `slack_app_token`, `user_aliases`, `timeout_hours`
   - Graceful error handling with helpful messages

4. **Build state manager** (shared module)
   - Read/write `.claude/state/slack-skill-pending.json`
   - Data model: `ConsultationRequest(id, slackUserId, slackUsername, question, createdAt, expiresAt, status, response, respondedAt)`
   - Atomic file operations with file locking
   - Thread-safe operations (server + CLI concurrent access)

5. **Build Slack client wrapper** (shared module)
   - Initialize Bolt SDK app with tokens
   - User lookup service (fuzzy match by name/email)
   - DM sender service
   - Conversation history fetcher
   - Error handling for rate limits, auth failures

### Phase 3: Event Server

6. **Build Slack event listener**
   - Use Slack Bolt Socket Mode (no public URL needed)
   - Subscribe to `message.im` events (DM messages to bot)
   - Filter: only messages from users to bot (not bot's own messages)
   - Extract message text, user ID, timestamp

7. **Build response handler**
   - Match incoming DM to pending consultation requests by user ID
   - Update state file: set response text, timestamp, status=ANSWERED
   - Log successful response capture

8. **Build server lifecycle management**
   - Graceful startup (load config, connect to Slack)
   - Optional health check endpoint
   - Graceful shutdown (flush logs, close connections)
   - Signal handling (SIGTERM, SIGINT)
   - Logging to file: `.claude/logs/slack-skill-server.log`

9. **Create service management scripts**
   - `scripts/install-service.sh` (detect OS, install systemd/launchd)
   - `scripts/start-server.sh`
   - `scripts/stop-server.sh`
   - `scripts/server-status.sh`
   - Service files: `slack-skill-server.service` (systemd), `com.claude.slack-skill.plist` (launchd)

### Phase 4: CLI Application

10. **Build `ask` command**
    - Parse: `ask @username "question"`
    - Generate request ID (UUID)
    - Resolve Slack user ID
    - Send DM with formatted question
    - Store state (PENDING, 24h expiration)
    - Output: Request ID and status message

11. **Build `check` command**
    - Parse: `check [request-id]` (optional, defaults to latest)
    - Load state, check status: PENDING/ANSWERED/EXPIRED
    - Pretty-print response if answered
    - Exit code: 0=answered, 1=pending, 2=expired

12. **Build `list` command**
    - Show all pending/answered requests (last 7 days)
    - Tabular format with ID, user, status, age

13. **Build `config` command**
    - Validate config file exists
    - Test Slack tokens (API auth check)
    - Show sanitized config (hide tokens)

14. **Build main CLI entrypoint**
    - Command routing (ask/check/list/config/help)
    - Error handling with helpful messages
    - Exit codes

### Phase 5: Bash Wrapper Skills

15. **Create `/slack-skill:ask` skill**
    - First-run: Check if server installed/running, auto-install if needed
    - Invoke: CLI with parsed arguments
    - Display request ID

16. **Create `/slack-skill:check` skill**
    - Invoke CLI check command
    - Format output for Claude

17. **Create `/slack-skill:list` skill**
    - Show pending consultations

18. **Create `/slack-skill:config` skill**
    - Validate configuration

19. **Create `/slack-skill:server` skill**
    - Subcommands: start, stop, status, restart, install
    - Wraps service management scripts

### Phase 6: Installation & Documentation

20. **Build auto-installer**
    - `./gradlew installDist` for both CLI and server
    - Copy service files to appropriate locations
    - Prompt for Slack tokens if config missing
    - Install and start server

21. **Create README.md**
    - Setup instructions (Slack app creation, token acquisition)
    - Usage examples
    - Troubleshooting guide

22. **Create ARCHITECTURE.md**
    - System design diagram
    - Component interaction flow
    - State management details

### Acceptance Criteria

#### Functional Requirements

**FR1: Slack User Consultation**
- ✅ User runs `/slack-skill:ask @username "question text"`
- ✅ System resolves Slack username to user ID (fuzzy match)
- ✅ Question sent as DM to resolved Slack user
- ✅ Request ID returned to Claude user
- ✅ State stored with PENDING status

**FR2: Response Capture**
- ✅ Slack user replies to bot DM
- ✅ Server automatically captures response
- ✅ State updated to ANSWERED with response text
- ✅ Response available within 5 seconds of Slack user reply

**FR3: Response Retrieval**
- ✅ User runs `/slack-skill:check [request-id]`
- ✅ System returns status: PENDING/ANSWERED/EXPIRED
- ✅ If ANSWERED: Full response text displayed
- ✅ If PENDING: Time remaining shown
- ✅ If EXPIRED: Timeout message shown

**FR4: Timeout Handling**
- ✅ Requests expire after 24 hours (configurable)
- ✅ Check command recognizes expired requests
- ✅ Expired requests don't block future operations

**FR5: Server Management**
- ✅ Server starts automatically on system boot
- ✅ Server recovers from Slack API disconnections
- ✅ `/slack-skill:server status` shows running/stopped state
- ✅ Server can be manually started/stopped/restarted

**FR6: Configuration**
- ✅ Config file validated on first run
- ✅ Clear error messages if tokens missing/invalid
- ✅ `/slack-skill:config` validates Slack API connectivity

#### Non-Functional Requirements

**NFR1: Reliability**
- Server maintains 99% uptime during normal operations
- State file operations are atomic (no corruption on crash)
- Server reconnects automatically after network interruptions

**NFR2: Performance**
- Ask command completes within 3 seconds
- Check command completes within 1 second
- Server processes incoming messages within 2 seconds

**NFR3: Usability**
- First-time setup takes < 10 minutes
- Error messages are actionable ("Missing token in config" vs "Error 401")
- CLI help text provides usage examples

**NFR4: Maintainability**
- Comprehensive logging (INFO for operations, ERROR for failures)
- Log rotation (max 10MB per file, keep last 5)
- Modular architecture (shared, cli, server clearly separated)

## Additional Context

### Dependencies

**Core:**
- Slack Bolt SDK for Java: `com.slack.api:bolt` (MIT license) - API + Socket Mode
- Kotlin standard library 1.9+
- Gradle 8.x+ for build management

**Serialization:**
- kotlinx.serialization for JSON state management
- SnakeYAML for YAML config parsing

**Logging:**
- SLF4J API
- Logback for implementation (with rotation support)

**Testing:**
- Kotlin Test / JUnit 5
- MockK for mocking
- WireMock for API mocking
- Testcontainers (optional, for isolated environments)

### Testing Strategy

#### Test Pyramid Structure
- **Unit Tests (60%)**: Individual component behavior
- **Integration Tests (30%)**: Component interactions
- **E2E Tests (10%)**: Full system validation

#### 1. Unit Tests

**Shared Module:**
- `ConfigManagerTest` - YAML parsing, validation, missing file handling
- `StateManagerTest` - JSON read/write, atomic operations, concurrent access
- `ConsultationRequestTest` - Data model validation, expiration logic
- `SlackClientWrapperTest` - Mocked Slack API calls

**Server Module:**
- `ResponseHandlerTest` - Message matching logic, state updates
- `EventListenerTest` - Message filtering (ignore bot messages)

**CLI Module:**
- `AskCommandTest` - Argument parsing, request creation
- `CheckCommandTest` - Status detection, output formatting
- `ListCommandTest` - Request filtering, table formatting

**Tools:** Kotlin Test / JUnit 5, MockK

#### 2. Integration Tests

**IT1: CLI ↔ State File**
- Ask command writes valid state
- Check command reads and interprets state correctly
- List command filters expired requests

**IT2: Server ↔ Slack API**
- Server connects via Socket Mode
- Server receives DM events
- Server ignores non-DM messages
- Uses WireMock for Slack API mocking

**IT3: CLI ↔ Slack API**
- Ask command resolves users and sends DMs
- Config command validates tokens
- Uses WireMock for API mocking

**IT4: End-to-End State Flow**
- Ask → State PENDING → Server updates → Check returns ANSWERED
- Uses in-memory Slack mock

**Tools:** Testcontainers, WireMock

#### 3. End-to-End Tests

**E2E1: Happy Path**
1. Start server
2. Run `/slack-skill:ask @testuser "What is X?"`
3. Simulate Slack user reply via test bot
4. Run `/slack-skill:check`
5. Assert: Response captured and displayed

**E2E2: Timeout Scenario**
1. Create request with short timeout
2. Wait for expiration
3. Run check command
4. Assert: EXPIRED status

**E2E3: Server Restart**
1. Create pending request
2. Stop server, start server
3. Simulate response
4. Assert: Response still captured (state persistence)

**Tools:** Real Slack workspace (test workspace), Docker Compose

#### 4. Manual Testing Checklist

**Installation:**
- First-run on macOS
- First-run on Linux
- Service auto-start

**Operations:**
- Valid/invalid username resolution
- User alias resolution
- DM delivery
- Response capture
- Server start/stop/restart
- Status checks
- Configuration validation

**Edge Cases:**
- Multiple simultaneous asks
- Same user, multiple questions
- Response to wrong bot (ignored)
- Network disconnection recovery
- State file corruption recovery

#### 5. Testing Phases

**Phase 1: Development Testing**
- Write unit tests alongside components
- Target: 80%+ unit test coverage

**Phase 2: Integration Testing**
- After core components complete
- Set up test Slack workspace
- Run integration suite in CI/CD

**Phase 3: E2E & Manual Testing**
- Before release
- Full installation on fresh systems
- Real-world usage scenarios

**CI/CD:** GitHub Actions for automated testing on push/PR

### Notes

**Implementation Notes:**
- First skill built by user - prioritize clear code structure and comprehensive documentation
- Multi-module architecture enables separation of concerns and independent testing
- Socket Mode eliminates need for public webhooks (simpler deployment)
- File-based state with locking allows both server and CLI to safely access state
- Service management scripts should detect OS automatically (macOS vs Linux)

**Future Enhancements:**
- ✅ `/slack-skill:list` to show pending requests (included in v1)
- ✅ `/slack-skill:config` to validate Slack bot token (included in v1)
- ⏭️ Interactive Slack app with buttons for quick responses (v2)
- ⏭️ Support for group DMs or channel mentions (v2)
- ⏭️ Web dashboard for managing consultations (v2)
- ⏭️ GraalVM native compilation for faster startup (v2)
- ⏭️ Response threading for multi-turn conversations (v2)

**Critical Success Factors:**
- Clear setup documentation (Slack app creation is non-trivial for first-timers)
- Robust error handling with actionable error messages
- Server reliability (auto-reconnect, graceful degradation)
- State file integrity (atomic writes, corruption recovery)
