# Technical Specification
## Slack Consultation Skill for Claude Code

---

**Document Version:** 1.0
**Status:** Ready for Implementation
**Date:** January 14, 2026
**Author:** Sena (with BMad Master)
**Architecture:** Hybrid CLI + Event Server
**Technology Stack:** Kotlin, Gradle, Slack Bolt SDK, systemd/launchd

---

<div style="page-break-after: always;"></div>

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Solution Overview](#3-solution-overview)
4. [Project Scope](#4-project-scope)
   - 4.1 [In Scope](#41-in-scope)
   - 4.2 [Out of Scope](#42-out-of-scope)
5. [Architecture & Design](#5-architecture--design)
   - 5.1 [System Architecture](#51-system-architecture)
   - 5.2 [Technical Stack](#52-technical-stack)
   - 5.3 [State Management](#53-state-management)
   - 5.4 [Slack Integration](#54-slack-integration)
6. [Implementation Plan](#6-implementation-plan)
   - 6.1 [Phase 1: Project Foundation](#61-phase-1-project-foundation)
   - 6.2 [Phase 2: Shared Infrastructure](#62-phase-2-shared-infrastructure)
   - 6.3 [Phase 3: Event Server](#63-phase-3-event-server)
   - 6.4 [Phase 4: CLI Application](#64-phase-4-cli-application)
   - 6.5 [Phase 5: Bash Wrapper Skills](#65-phase-5-bash-wrapper-skills)
   - 6.6 [Phase 6: Installation & Documentation](#66-phase-6-installation--documentation)
7. [Acceptance Criteria](#7-acceptance-criteria)
   - 7.1 [Functional Requirements](#71-functional-requirements)
   - 7.2 [Non-Functional Requirements](#72-non-functional-requirements)
8. [Testing Strategy](#8-testing-strategy)
   - 8.1 [Unit Tests](#81-unit-tests)
   - 8.2 [Integration Tests](#82-integration-tests)
   - 8.3 [End-to-End Tests](#83-end-to-end-tests)
   - 8.4 [Manual Testing](#84-manual-testing)
   - 8.5 [CI/CD Pipeline](#85-cicd-pipeline)
9. [Dependencies](#9-dependencies)
10. [Risk Assessment](#10-risk-assessment)
11. [Future Enhancements](#11-future-enhancements)
12. [Appendices](#12-appendices)

---

<div style="page-break-after: always;"></div>

## 1. Executive Summary

This technical specification outlines the design and implementation plan for a **Slack Consultation Skill** that enables Claude Code to request asynchronous input from subject matter experts via Slack direct messages.

### Key Features

- **Multi-module Kotlin application** with CLI and background event server
- **Real-time response capture** using Slack Socket Mode (no webhooks required)
- **File-based state management** with concurrent access safety
- **Cross-platform service management** (systemd/launchd)
- **Auto-installation** with dependency resolution

### Project Metrics

| Metric | Value |
|--------|-------|
| **Implementation Phases** | 6 |
| **Total Tasks** | 22 |
| **Modules** | 3 (shared, cli, server) |
| **Estimated Setup Time** | < 10 minutes |
| **Target Test Coverage** | 80%+ |
| **Default Timeout** | 24 hours |

### Architecture Decision

After evaluating multiple approaches, the **hybrid architecture** (user-triggered CLI + background event server) was selected for its completeness and professional solution delivery, despite increased operational complexity.

---

<div style="page-break-after: always;"></div>

## 2. Problem Statement

### Current State

When Claude Code requires external input from subject matter experts during a conversation, the workflow is interrupted. There is no built-in mechanism for:

- Requesting asynchronous consultation from humans
- Delivering questions via users' preferred communication channels
- Automatically capturing and retrieving responses
- Managing timeout scenarios gracefully

### Impact

This limitation forces users to:

1. Manually reach out to experts outside the Claude Code environment
2. Wait for responses while keeping context in mind
3. Manually relay information back to Claude
4. Restart or continue interrupted conversations

### User Need

A seamless integration that allows Claude Code to:

- Send consultation requests to Slack users
- Automatically capture responses when available
- Retrieve responses without blocking the conversation
- Handle timeouts gracefully (24-hour default)

---

<div style="page-break-after: always;"></div>

## 3. Solution Overview

### High-Level Description

A Claude Code skill that enables **Slack-based consultation** with the following flow:

```
┌──────────────┐
│ Claude User  │
└──────┬───────┘
       │ /slack-skill:ask @expert "question"
       ▼
┌──────────────┐     ┌─────────────────┐
│   CLI Tool   │────▶│  Slack API      │
└──────┬───────┘     └────────┬────────┘
       │                      │
       │ Store State          │ DM Sent
       ▼                      ▼
┌──────────────┐     ┌─────────────────┐
│  State File  │     │  Slack User     │
└──────┬───────┘     └────────┬────────┘
       ▲                      │
       │ Update Response      │ Replies
       │                      ▼
┌──────────────┐     ┌─────────────────┐
│ Event Server │◀────│  Socket Mode    │
└──────────────┘     └─────────────────┘
       │
       │ /slack-skill:check
       ▼
┌──────────────┐
│ Claude User  │
│ (Response)   │
└──────────────┘
```

### Core Components

1. **Bash Wrapper Skills** - User-facing commands integrated with Claude Code
2. **Kotlin CLI Application** - Handles ask, check, list, config commands
3. **Kotlin Event Server** - Background daemon listening for Slack responses
4. **Shared Infrastructure** - Configuration, state management, Slack client
5. **Service Management** - systemd/launchd for daemon lifecycle

### Key Benefits

- ✅ **Non-blocking**: Consultations don't halt Claude conversations
- ✅ **Automatic**: Responses captured without manual intervention
- ✅ **Reliable**: 99% uptime target with auto-reconnection
- ✅ **Simple**: No public webhooks, uses Slack Socket Mode
- ✅ **Cross-platform**: Works on macOS and Linux

---

<div style="page-break-after: always;"></div>

## 4. Project Scope

### 4.1 In Scope

#### Core Functionality

- Multi-module Kotlin project (shared, cli, server)
- Bash wrapper skills: `/slack-skill:ask`, `/slack-skill:check`, `/slack-skill:list`, `/slack-skill:config`, `/slack-skill:server`
- Kotlin CLI with Slack Bolt SDK integration
- Background event server using Slack Socket Mode
- Real-time response capture when Slack user replies to bot DM

#### Technical Features

- Slack user resolution via API (fuzzy name/email matching)
- DM delivery to resolved Slack users
- File-based state management with concurrent access safety
- 24-hour configurable timeout with expiration tracking
- Service management (systemd/launchd) with auto-start on boot
- Auto-installation of dependencies and services
- Configuration file: `.claude/config/slack-skill.yaml`

### 4.2 Out of Scope

#### Excluded from v1

- Autonomous Claude invocation (server only captures responses)
- Multi-channel notifications or group DMs
- Agent-to-agent consultation (human-only in v1)
- Slack workspace management or admin features
- Response threading or full conversation history
- Integration with other platforms (Discord, Teams, etc.)
- Native binary compilation (GraalVM)
- Web dashboard or GUI for managing consultations

#### Rationale

These features add significant complexity and are not essential for the core use case. They may be considered for v2 after validating the core functionality.

---

<div style="page-break-after: always;"></div>

## 5. Architecture & Design

### 5.1 System Architecture

#### Architecture Decision

**Selected: Hybrid Architecture (CLI + Background Server)**

**Alternatives Considered:**

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| **A) User-triggered only** | Simple, no daemon | Must poll for responses | Rejected |
| **B) Hybrid (selected)** | Complete, real-time | Daemon management | ✅ **Selected** |
| **C) Webhook-based** | Industry standard | Requires public URL | Rejected |

#### Module Structure

```
slack-skill/
├── shared/
│   ├── config/           # Configuration management
│   ├── state/            # State file operations
│   ├── models/           # Data models
│   └── slack/            # Slack client wrapper
├── cli/
│   ├── commands/         # Ask, check, list, config
│   └── Main.kt           # CLI entrypoint
├── server/
│   ├── listener/         # Slack event listener
│   ├── handler/          # Response handler
│   └── Server.kt         # Server entrypoint
├── scripts/
│   ├── install-service.sh
│   ├── start-server.sh
│   ├── stop-server.sh
│   └── server-status.sh
└── .claude/
    ├── commands/slack-skill/  # Bash wrapper skills
    ├── config/               # YAML config
    ├── state/                # JSON state
    └── logs/                 # Server logs
```

### 5.2 Technical Stack

#### Language & Build

- **Language**: Kotlin 1.9+
- **Build Tool**: Gradle 8.x with wrapper
- **Execution**: Standard JVM (no native compilation in v1)

**Rationale**: User preference for JVM comfort, excellent tooling, strong type safety.

#### Core Dependencies

| Dependency | Version | Purpose | License |
|------------|---------|---------|---------|
| Slack Bolt SDK | Latest | API + Socket Mode | MIT |
| kotlinx.serialization | Latest | JSON state management | Apache 2.0 |
| SnakeYAML | Latest | YAML config parsing | Apache 2.0 |
| SLF4J + Logback | Latest | Logging with rotation | MIT / EPL |

#### Testing Dependencies

| Dependency | Purpose |
|------------|---------|
| Kotlin Test / JUnit 5 | Unit testing |
| MockK | Mocking framework |
| WireMock | HTTP API mocking |
| Testcontainers | Isolated test environments (optional) |

### 5.3 State Management

#### State File Location

`.claude/state/slack-skill-pending.json`

#### Data Model

```kotlin
data class ConsultationRequest(
    val id: String,              // UUID
    val slackUserId: String,     // U123ABC
    val slackUsername: String,   // @expert
    val question: String,        // Question text
    val createdAt: Instant,      // Timestamp
    val expiresAt: Instant,      // createdAt + timeout
    val status: Status,          // PENDING, ANSWERED, EXPIRED
    val response: String?,       // Response text (null if pending)
    val respondedAt: Instant?    // Response timestamp
)

enum class Status {
    PENDING,
    ANSWERED,
    EXPIRED
}
```

#### Concurrency Safety

- **File Locking**: Atomic operations with OS-level locks
- **Write Strategy**: Write to temp file, atomic rename
- **Read Strategy**: Read with retry logic (handle lock contention)
- **Access Pattern**: CLI reads/writes, Server writes only

### 5.4 Slack Integration

#### Socket Mode

**Why Socket Mode?**

- ✅ No public URL required
- ✅ No webhook infrastructure
- ✅ Real-time event delivery
- ✅ Simplified deployment
- ❌ Requires running process (acceptable trade-off)

#### Required Slack Tokens

1. **Bot Token** (`xoxb-...`): API calls (send DMs, lookup users)
2. **App Token** (`xapp-...`): Socket Mode connection

#### User Resolution Flow

```
Input: @username or email
    ↓
Call: users.list API
    ↓
Fuzzy Match: display_name, real_name
    ↓
Fallback: Check config aliases (username → email/slack-id)
    ↓
Output: Slack User ID (U123ABC) or Error
```

#### Event Handling

**Subscribe to**: `message.im` events

**Filter Logic**:
- ✅ User → Bot messages
- ❌ Bot → User messages (prevent loops)
- ❌ Messages not in DM context

---

<div style="page-break-after: always;"></div>

## 6. Implementation Plan

### 6.1 Phase 1: Project Foundation

#### Task 1: Initialize Gradle/Kotlin Multi-Module Project

**Deliverables**:
- Root `settings.gradle.kts` with three modules: `shared`, `cli`, `server`
- Shared `buildSrc` for common dependency management
- `cli/build.gradle.kts` with application plugin
- `server/build.gradle.kts` with application plugin
- Dependencies: Slack Bolt SDK, kotlinx.serialization, SnakeYAML, SLF4J/Logback

**Validation**: `./gradlew projects` shows all three modules

#### Task 2: Create Directory Structure

**Deliverables**:
- `shared/src/main/kotlin/com/claude/slack/shared/`
- `cli/src/main/kotlin/com/claude/slack/cli/`
- `server/src/main/kotlin/com/claude/slack/server/`
- `.claude/commands/slack-skill/`
- `.claude/config/`
- `.claude/state/`
- `.claude/logs/`
- `scripts/`

**Validation**: Directory tree matches specification

---

### 6.2 Phase 2: Shared Infrastructure

#### Task 3: Build Configuration Manager (Shared Module)

**Responsibilities**:
- Read `.claude/config/slack-skill.yaml`
- Validate required fields: `slack_bot_token`, `slack_app_token`, `user_aliases`, `timeout_hours`
- Provide graceful error handling with actionable messages

**Validation**: Unit tests for missing config, invalid tokens, valid parsing

#### Task 4: Build State Manager (Shared Module)

**Responsibilities**:
- Read/write `.claude/state/slack-skill-pending.json`
- Implement `ConsultationRequest` data model
- Atomic file operations with OS-level locking
- Thread-safe operations (CLI and server concurrent access)

**Validation**: Unit tests for concurrent reads/writes, atomic operations

#### Task 5: Build Slack Client Wrapper (Shared Module)

**Responsibilities**:
- Initialize Bolt SDK app with tokens
- User lookup service (fuzzy match by name/email)
- DM sender service (`chat.postMessage`)
- Conversation history fetcher (for polling fallback)
- Error handling for rate limits, auth failures

**Validation**: Integration tests with mocked Slack API (WireMock)

---

### 6.3 Phase 3: Event Server

#### Task 6: Build Slack Event Listener

**Responsibilities**:
- Use Slack Bolt SDK Socket Mode
- Subscribe to `message.im` events
- Filter: only user → bot messages (exclude bot's own messages)
- Extract: message text, user ID, timestamp

**Validation**: Integration tests with mocked Socket Mode events

#### Task 7: Build Response Handler

**Responsibilities**:
- Match incoming DM to pending consultation requests by user ID
- Update state file: set response text, timestamp, status=ANSWERED
- Log successful response capture (INFO level)

**Validation**: Integration tests for state updates, matching logic

#### Task 8: Build Server Lifecycle Management

**Responsibilities**:
- Graceful startup (load config, connect to Slack)
- Optional health check endpoint (HTTP on localhost:8080)
- Graceful shutdown (flush logs, close connections)
- Signal handling (SIGTERM, SIGINT)
- Logging to file: `.claude/logs/slack-skill-server.log`

**Validation**: Manual testing for startup/shutdown, signal handling

#### Task 9: Create Service Management Scripts

**Deliverables**:
- `scripts/install-service.sh` (detect OS, install systemd/launchd)
- `scripts/start-server.sh`
- `scripts/stop-server.sh`
- `scripts/server-status.sh`
- Service files:
  - `slack-skill-server.service` (systemd)
  - `com.claude.slack-skill.plist` (launchd)

**Validation**: Manual testing on macOS and Linux

---

### 6.4 Phase 4: CLI Application

#### Task 10: Build `ask` Command

**Syntax**: `ask @username "question"`

**Responsibilities**:
- Parse arguments
- Generate request ID (UUID)
- Resolve Slack user ID via shared Slack client
- Send DM with formatted question
- Store state (PENDING, 24h expiration)
- Output: Request ID and status message

**Validation**: Unit tests for parsing, integration tests for end-to-end flow

#### Task 11: Build `check` Command

**Syntax**: `check [request-id]` (optional, defaults to latest)

**Responsibilities**:
- Load state file
- Check status: PENDING/ANSWERED/EXPIRED
- Pretty-print response if answered
- Exit codes: 0=answered, 1=pending, 2=expired

**Validation**: Unit tests for status detection, output formatting

#### Task 12: Build `list` Command

**Syntax**: `list`

**Responsibilities**:
- Show all pending/answered requests (last 7 days)
- Tabular format with ID, user, status, age

**Validation**: Unit tests for filtering, formatting

#### Task 13: Build `config` Command

**Syntax**: `config`

**Responsibilities**:
- Validate config file exists
- Test Slack tokens (API auth check)
- Show sanitized config (hide tokens, show `xoxb-****`)

**Validation**: Integration tests with mocked Slack API

#### Task 14: Build Main CLI Entrypoint

**Responsibilities**:
- Command routing (ask/check/list/config/help)
- Error handling with helpful messages
- Exit codes (0=success, 1=error, 2=usage error)

**Validation**: Integration tests for routing, error handling

---

### 6.5 Phase 5: Bash Wrapper Skills

#### Task 15: Create `/slack-skill:ask` Skill

**Location**: `.claude/commands/slack-skill/ask.md`

**Responsibilities**:
- First-run check: Is server installed/running? Auto-install if needed
- Invoke CLI: `./gradlew :cli:run --args="ask $@"`
- Display request ID to user

**Validation**: Manual testing in Claude Code

#### Task 16: Create `/slack-skill:check` Skill

**Location**: `.claude/commands/slack-skill/check.md`

**Responsibilities**:
- Invoke CLI check command
- Format output for Claude (markdown)

**Validation**: Manual testing in Claude Code

#### Task 17: Create `/slack-skill:list` Skill

**Location**: `.claude/commands/slack-skill/list.md`

**Responsibilities**:
- Invoke CLI list command
- Format output as table

**Validation**: Manual testing

#### Task 18: Create `/slack-skill:config` Skill

**Location**: `.claude/commands/slack-skill/config.md`

**Responsibilities**:
- Validate configuration
- Show status (tokens valid, server running)

**Validation**: Manual testing

#### Task 19: Create `/slack-skill:server` Skill

**Location**: `.claude/commands/slack-skill/server.md`

**Syntax**: `/slack-skill:server [start|stop|status|restart|install]`

**Responsibilities**:
- Wrap service management scripts
- Show friendly output

**Validation**: Manual testing for all subcommands

---

### 6.6 Phase 6: Installation & Documentation

#### Task 20: Build Auto-Installer

**Location**: `scripts/install.sh`

**Responsibilities**:
- Run `./gradlew installDist` for CLI and server
- Copy service files to system locations
- Prompt for Slack tokens if config missing
- Install and start server
- Provide next steps

**Validation**: Fresh system installation tests

#### Task 21: Create README.md

**Sections**:
1. Overview
2. Prerequisites (Slack app setup)
3. Installation
4. Configuration (token acquisition guide)
5. Usage examples
6. Troubleshooting
7. Architecture overview

**Validation**: Follow README on fresh system

#### Task 22: Create ARCHITECTURE.md

**Sections**:
1. System design diagram (Mermaid or ASCII)
2. Component interaction flow
3. State management details
4. Slack integration specifics
5. Extension points

**Validation**: Technical review

---

<div style="page-break-after: always;"></div>

## 7. Acceptance Criteria

### 7.1 Functional Requirements

#### FR1: Slack User Consultation

**Criteria**:
- ✅ User runs `/slack-skill:ask @username "question text"`
- ✅ System resolves Slack username to user ID (fuzzy match on display name/real name)
- ✅ Question sent as DM to resolved Slack user
- ✅ Request ID returned to Claude user (UUID format)
- ✅ State stored with PENDING status

**Test Method**: E2E test with real Slack workspace

#### FR2: Response Capture

**Criteria**:
- ✅ Slack user replies to bot DM
- ✅ Server automatically captures response within 5 seconds
- ✅ State updated to ANSWERED with response text and timestamp
- ✅ No manual intervention required

**Test Method**: E2E test with simulated Slack reply

#### FR3: Response Retrieval

**Criteria**:
- ✅ User runs `/slack-skill:check [request-id]`
- ✅ System returns status: PENDING/ANSWERED/EXPIRED
- ✅ If ANSWERED: Full response text displayed in markdown
- ✅ If PENDING: Time remaining shown (e.g., "23h 45m remaining")
- ✅ If EXPIRED: Timeout message shown

**Test Method**: Integration tests for each status

#### FR4: Timeout Handling

**Criteria**:
- ✅ Requests expire after 24 hours (configurable via YAML)
- ✅ Check command recognizes expired requests
- ✅ Expired requests don't block future operations
- ✅ State file remains valid with expired entries

**Test Method**: Integration test with short timeout (60s)

#### FR5: Server Management

**Criteria**:
- ✅ Server starts automatically on system boot (systemd/launchd)
- ✅ Server recovers from Slack API disconnections within 30 seconds
- ✅ `/slack-skill:server status` shows accurate running/stopped state
- ✅ Server can be manually started/stopped/restarted via skill

**Test Method**: Manual testing on macOS and Linux

#### FR6: Configuration

**Criteria**:
- ✅ Config file validated on first run
- ✅ Clear error messages if tokens missing (e.g., "Missing slack_bot_token in .claude/config/slack-skill.yaml")
- ✅ Clear error messages if tokens invalid (e.g., "Invalid Slack token: auth failed")
- ✅ `/slack-skill:config` validates Slack API connectivity

**Test Method**: Unit tests + manual testing

### 7.2 Non-Functional Requirements

#### NFR1: Reliability

**Criteria**:
- Server maintains 99% uptime during normal operations
- State file operations are atomic (no corruption on crash)
- Server reconnects automatically after network interruptions
- Logs capture all errors for debugging

**Test Method**: Chaos testing (kill server, network interruption, disk full)

#### NFR2: Performance

**Criteria**:
- Ask command completes within 3 seconds (95th percentile)
- Check command completes within 1 second (95th percentile)
- Server processes incoming messages within 2 seconds
- Server memory usage < 256MB

**Test Method**: Performance benchmarks with JMeter or Gatling

#### NFR3: Usability

**Criteria**:
- First-time setup takes < 10 minutes (including Slack app creation)
- Error messages are actionable (explain what to do next)
- CLI help text provides usage examples for each command
- README provides complete setup guide

**Test Method**: User testing with fresh system

#### NFR4: Maintainability

**Criteria**:
- Comprehensive logging (INFO for operations, ERROR for failures)
- Log rotation (max 10MB per file, keep last 5)
- Modular architecture (shared, cli, server clearly separated)
- Unit test coverage > 80%
- Code follows Kotlin style guide

**Test Method**: Code review, test coverage report

---

<div style="page-break-after: always;"></div>

## 8. Testing Strategy

### 8.1 Unit Tests

**Target Coverage**: 60% of test pyramid (80%+ code coverage)

#### Shared Module Tests

| Test Class | Coverage |
|------------|----------|
| `ConfigManagerTest` | YAML parsing, validation, missing file handling |
| `StateManagerTest` | JSON read/write, atomic operations, concurrent access |
| `ConsultationRequestTest` | Data model validation, expiration logic, serialization |
| `SlackClientWrapperTest` | Mocked Slack API calls, error handling |

#### Server Module Tests

| Test Class | Coverage |
|------------|----------|
| `ResponseHandlerTest` | Message matching logic, state updates, edge cases |
| `EventListenerTest` | Message filtering, bot message exclusion |

#### CLI Module Tests

| Test Class | Coverage |
|------------|----------|
| `AskCommandTest` | Argument parsing, request creation, error handling |
| `CheckCommandTest` | Status detection, output formatting, exit codes |
| `ListCommandTest` | Request filtering, table formatting, date handling |
| `ConfigCommandTest` | Validation logic, sanitization |

**Tools**: Kotlin Test / JUnit 5, MockK for mocking

**Execution**: `./gradlew test` with coverage report

### 8.2 Integration Tests

**Target Coverage**: 30% of test pyramid

#### IT1: CLI ↔ State File

**Scenarios**:
- Ask command writes valid state to file
- Check command reads and interprets state correctly
- List command filters expired requests
- Concurrent CLI operations don't corrupt state

**Setup**: Temporary file system, mocked Slack API

#### IT2: Server ↔ Slack API

**Scenarios**:
- Server connects via Socket Mode successfully
- Server receives DM events and processes them
- Server ignores non-DM messages
- Server handles rate limiting gracefully

**Setup**: WireMock for Slack API mocking

#### IT3: CLI ↔ Slack API

**Scenarios**:
- Ask command resolves users and sends DMs
- Config command validates tokens
- User resolution handles fuzzy matches

**Setup**: WireMock for Slack API mocking

#### IT4: End-to-End State Flow

**Scenarios**:
- Ask → State PENDING → Server updates → Check returns ANSWERED
- Multiple pending requests managed correctly

**Setup**: In-memory Slack mock, shared state file

**Tools**: Testcontainers for isolated environments, WireMock

**Execution**: `./gradlew integrationTest`

### 8.3 End-to-End Tests

**Target Coverage**: 10% of test pyramid

#### E2E1: Happy Path

**Steps**:
1. Start server via service management script
2. Run `/slack-skill:ask @testuser "What is X?"`
3. Verify DM sent to Slack (inspect via API)
4. Simulate Slack user reply via test bot
5. Run `/slack-skill:check`
6. Assert: Response captured and displayed correctly

**Environment**: Real Slack test workspace

#### E2E2: Timeout Scenario

**Steps**:
1. Create request with short timeout (60s)
2. Wait 61 seconds (or mock time)
3. Run `/slack-skill:check`
4. Assert: EXPIRED status returned

**Environment**: Real Slack test workspace

#### E2E3: Server Restart

**Steps**:
1. Create pending request
2. Stop server via `/slack-skill:server stop`
3. Start server via `/slack-skill:server start`
4. Simulate Slack response
5. Assert: Response still captured (state persistence validated)

**Environment**: Real Slack test workspace

**Tools**: Real Slack workspace, Docker Compose for reproducible environments

**Execution**: `./scripts/run-e2e-tests.sh`

### 8.4 Manual Testing

#### Installation Checklist

- [ ] First-run on clean macOS system (Monterey+)
- [ ] First-run on clean Linux system (Ubuntu 22.04+)
- [ ] Service installation completes without errors
- [ ] Service auto-starts on boot
- [ ] README instructions are accurate

#### Operations Checklist

- [ ] Valid username resolution (@john → U123ABC)
- [ ] Invalid username handling (clear error message)
- [ ] User alias resolution (john@company.com → U123ABC)
- [ ] DM delivery to Slack (verify in Slack UI)
- [ ] Response capture (reply in Slack, verify via check)
- [ ] Server start/stop/restart via skill commands
- [ ] Status check accuracy (`/slack-skill:server status`)
- [ ] Configuration validation (`/slack-skill:config`)

#### Edge Cases Checklist

- [ ] Multiple simultaneous asks (state doesn't corrupt)
- [ ] Same user, multiple questions (all tracked independently)
- [ ] Response to wrong bot (ignored, doesn't corrupt state)
- [ ] Network disconnection during send (graceful error)
- [ ] Network disconnection recovery (server reconnects)
- [ ] State file corruption recovery (backup/restore)
- [ ] Disk full scenario (logs error, doesn't crash)
- [ ] Invalid Slack tokens (clear error at startup)

### 8.5 CI/CD Pipeline

#### GitHub Actions Workflow

**Trigger**: Push to `main` or `develop`, pull requests

**Jobs**:

1. **Build**
   - Checkout code
   - Setup JDK 17
   - Run `./gradlew build`
   - Upload build artifacts

2. **Unit Tests**
   - Run `./gradlew test`
   - Generate coverage report (JaCoCo)
   - Fail if coverage < 80%
   - Upload test results

3. **Integration Tests**
   - Run `./gradlew integrationTest`
   - Use Testcontainers for isolation
   - Upload test results

4. **Linting**
   - Run `./gradlew ktlintCheck`
   - Fail if style violations found

5. **Security Scan**
   - Run dependency vulnerability scan
   - Fail if critical vulnerabilities found

6. **Build Distributions**
   - Run `./gradlew installDist`
   - Package for macOS (tar.gz)
   - Package for Linux (tar.gz)
   - Upload as release artifacts

**Success Criteria**: All jobs pass before merge allowed

---

<div style="page-break-after: always;"></div>

## 9. Dependencies

### Core Dependencies

| Group ID | Artifact ID | Version | License | Purpose |
|----------|-------------|---------|---------|---------|
| com.slack.api | bolt | 1.37+ | MIT | Slack API + Socket Mode |
| org.jetbrains.kotlin | kotlin-stdlib | 1.9+ | Apache 2.0 | Kotlin standard library |
| org.jetbrains.kotlinx | kotlinx-serialization-json | 1.6+ | Apache 2.0 | JSON state management |
| org.yaml | snakeyaml | 2.2+ | Apache 2.0 | YAML config parsing |
| org.slf4j | slf4j-api | 2.0+ | MIT | Logging API |
| ch.qos.logback | logback-classic | 1.4+ | EPL 1.0 | Logging implementation |

### Testing Dependencies

| Group ID | Artifact ID | Version | License | Purpose |
|----------|-------------|---------|---------|---------|
| org.jetbrains.kotlin | kotlin-test-junit5 | 1.9+ | Apache 2.0 | Unit testing |
| io.mockk | mockk | 1.13+ | Apache 2.0 | Mocking framework |
| com.github.tomakehurst | wiremock | 3.3+ | Apache 2.0 | HTTP API mocking |
| org.testcontainers | testcontainers | 1.19+ | MIT | Isolated test environments |

### Build Dependencies

| Group ID | Artifact ID | Version | Purpose |
|----------|-------------|---------|---------|
| org.gradle | gradle-wrapper | 8.5+ | Build automation |
| org.jetbrains.kotlin | kotlin-gradle-plugin | 1.9+ | Kotlin compilation |

### Dependency Management

- **Gradle Version Catalogs** for centralized dependency versions
- **Dependabot** for automated dependency updates
- **OWASP Dependency Check** for vulnerability scanning

---

<div style="page-break-after: always;"></div>

## 10. Risk Assessment

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Slack API Rate Limiting** | Medium | High | Implement exponential backoff, queue management |
| **State File Corruption** | Low | High | Atomic writes, file locking, backup strategy |
| **Server Memory Leak** | Low | Medium | Memory profiling, automated restart on threshold |
| **Socket Mode Disconnection** | Medium | Medium | Auto-reconnect with exponential backoff |
| **Concurrent State Access** | Medium | High | File locking, test concurrent scenarios |

### Operational Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Service Fails to Start on Boot** | Low | High | Extensive testing on both platforms, fallback to manual start |
| **User Misconfigures Tokens** | High | Medium | Clear validation, helpful error messages, setup wizard |
| **Disk Full (Logs)** | Low | Low | Log rotation, max file size limits |
| **Network Outage** | Medium | Low | Graceful degradation, queue pending sends |

### User Experience Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Complex Slack App Setup** | High | High | Detailed README with screenshots, video tutorial |
| **Confusing Error Messages** | Medium | Medium | User-friendly error messages with next steps |
| **Timeout Too Short/Long** | Medium | Low | Make timeout configurable, good default (24h) |

### Project Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Scope Creep** | Medium | High | Strict adherence to v1 scope, defer features to v2 |
| **Underestimated Complexity** | Medium | Medium | Buffer time in phases, MVP approach |
| **Testing Slack App Creation** | Low | Medium | Create test workspace early, document process |

---

<div style="page-break-after: always;"></div>

## 11. Future Enhancements

### Included in v1 ✅

- `/slack-skill:list` to show all pending requests
- `/slack-skill:config` to validate Slack bot token
- Service management (start/stop/status/restart)
- Configurable timeout (not hardcoded 24h)

### Planned for v2 ⏭️

#### Enhanced User Experience

- **Interactive Slack App**
  - Buttons for quick responses (Yes/No, Option A/B/C)
  - Rich formatting with Slack blocks
  - Conversation threading

- **Multi-turn Conversations**
  - Follow-up questions in same thread
  - Conversation history tracking
  - Context preservation

#### Extended Platform Support

- **Group DMs and Channels**
  - Send to multiple experts simultaneously
  - Channel mentions (@channel-name)
  - Poll-style questions

- **Other Messaging Platforms**
  - Discord integration
  - Microsoft Teams integration
  - Email fallback

#### Performance & Scalability

- **GraalVM Native Compilation**
  - Faster startup times (< 1s)
  - Lower memory footprint (< 50MB)
  - Easier distribution (single binary)

- **Database Backend**
  - Replace JSON file with SQLite/PostgreSQL
  - Better concurrency handling
  - Query capabilities

#### Monitoring & Observability

- **Web Dashboard**
  - View all pending/answered consultations
  - Analytics (response times, most consulted experts)
  - Admin features (force expire, delete)

- **Metrics & Alerting**
  - Prometheus metrics export
  - Grafana dashboards
  - Alert on server downtime

#### Advanced Features

- **Response Templates**
  - Pre-defined question templates
  - Auto-formatting for common questions

- **Expert Scheduling**
  - Check expert availability (Slack status)
  - Time zone awareness
  - Escalation to backup expert

---

<div style="page-break-after: always;"></div>

## 12. Appendices

### Appendix A: Configuration File Example

```yaml
# .claude/config/slack-skill.yaml

slack_bot_token: "xoxb-your-bot-token-here"
slack_app_token: "xapp-your-app-token-here"

timeout_hours: 24

user_aliases:
  john: "U123ABC"
  jane: "jane.doe@company.com"
  security: "U789XYZ"

# Optional: Custom logging level (INFO, DEBUG, WARN, ERROR)
log_level: INFO
```

### Appendix B: State File Example

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
    },
    {
      "id": "660e9511-f30c-52e5-b827-557766551111",
      "slackUserId": "U456DEF",
      "slackUsername": "@jane",
      "question": "Can we deploy to prod today?",
      "createdAt": "2026-01-14T14:30:00Z",
      "expiresAt": "2026-01-15T14:30:00Z",
      "status": "PENDING",
      "response": null,
      "respondedAt": null
    }
  ]
}
```

### Appendix C: Service File Examples

#### systemd (Linux)

```ini
# /etc/systemd/system/slack-skill-server.service

[Unit]
Description=Slack Consultation Skill Event Server
After=network.target

[Service]
Type=simple
User=your-username
WorkingDirectory=/home/your-username/slack-skill
ExecStart=/home/your-username/slack-skill/server/build/install/server/bin/server
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

#### launchd (macOS)

```xml
<!-- ~/Library/LaunchAgents/com.claude.slack-skill.plist -->

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.claude.slack-skill</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/your-username/slack-skill/server/build/install/server/bin/server</string>
    </array>
    <key>WorkingDirectory</key>
    <string>/Users/your-username/slack-skill</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/Users/your-username/.claude/logs/slack-skill-server.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/your-username/.claude/logs/slack-skill-server-error.log</string>
</dict>
</plist>
```

### Appendix D: Usage Examples

#### Example 1: Ask a Question

```bash
$ /slack-skill:ask @security "What's the API key for Stripe production?"

✓ Question sent to @security (John Smith)
Request ID: 550e8400-e29b-41d4-a716-446655440000
Expires: 2026-01-15 10:00:00 (24h)

Use `/slack-skill:check 550e8400-e29b-41d4-a716-446655440000` to retrieve response.
```

#### Example 2: Check Response (Answered)

```bash
$ /slack-skill:check 550e8400-e29b-41d4-a716-446655440000

✓ Response received from @security (John Smith)
Responded: 2026-01-14 10:15:32 (15 minutes ago)

---
Check 1Password vault 'Production Credentials' under Stripe → Production API Key
---
```

#### Example 3: Check Response (Pending)

```bash
$ /slack-skill:check 660e9511-f30c-52e5-b827-557766551111

⏳ Awaiting response from @jane (Jane Doe)
Asked: 2026-01-14 14:30:00 (1 hour ago)
Expires: 2026-01-15 14:30:00 (23h remaining)
```

#### Example 4: List All Requests

```bash
$ /slack-skill:list

┌──────────────┬──────────┬───────────┬──────────────┬────────────┐
│ ID (short)   │ User     │ Status    │ Asked        │ Response   │
├──────────────┼──────────┼───────────┼──────────────┼────────────┤
│ 550e8400     │ @security│ ANSWERED  │ 2h ago       │ 15m later  │
│ 660e9511     │ @jane    │ PENDING   │ 1h ago       │ -          │
│ 770f0622     │ @devops  │ EXPIRED   │ 26h ago      │ -          │
└──────────────┴──────────┴───────────┴──────────────┴────────────┘

3 requests in last 7 days
```

### Appendix E: Slack App Setup Guide

#### Step 1: Create Slack App

1. Go to https://api.slack.com/apps
2. Click "Create New App" → "From scratch"
3. App Name: "Claude Consultation Bot"
4. Workspace: Select your workspace
5. Click "Create App"

#### Step 2: Enable Socket Mode

1. Navigate to "Socket Mode" in sidebar
2. Toggle "Enable Socket Mode" to ON
3. Generate App Token:
   - Token Name: "Claude Skill"
   - Scopes: `connections:write`
4. Copy token (starts with `xapp-`)
5. Save to `.claude/config/slack-skill.yaml`

#### Step 3: Configure Bot Permissions

1. Navigate to "OAuth & Permissions"
2. Add Bot Token Scopes:
   - `chat:write` (send messages)
   - `users:read` (lookup users)
   - `im:read` (read DMs)
   - `im:write` (send DMs)
   - `im:history` (read DM history)
3. Scroll up, click "Install to Workspace"
4. Authorize the app
5. Copy Bot User OAuth Token (starts with `xoxb-`)
6. Save to `.claude/config/slack-skill.yaml`

#### Step 4: Enable Event Subscriptions

1. Navigate to "Event Subscriptions"
2. Toggle "Enable Events" to ON
3. Subscribe to bot events:
   - `message.im` (DM messages)
4. Save Changes

#### Step 5: Test Connection

```bash
$ /slack-skill:config

✓ Configuration valid
✓ Bot token authenticated (xoxb-****)
✓ App token authenticated (xapp-****)
✓ Slack connection successful
✓ Server status: RUNNING

Ready to use!
```

---

### Appendix F: Troubleshooting Guide

#### Issue: "Missing slack_bot_token in config"

**Cause**: Configuration file not created or incomplete

**Solution**:
1. Create `.claude/config/slack-skill.yaml`
2. Follow Appendix E to get tokens
3. Add tokens to config file
4. Run `/slack-skill:config` to validate

---

#### Issue: "Server not running"

**Cause**: Background server hasn't been started

**Solution**:
```bash
$ /slack-skill:server start
Starting Slack consultation server...
✓ Server started successfully (PID: 12345)
```

---

#### Issue: "User not found: @username"

**Cause**: User doesn't exist or fuzzy match failed

**Solution**:
1. Check username spelling
2. Try full name: `@John Smith`
3. Add alias to config:
   ```yaml
   user_aliases:
     john: "john.smith@company.com"
   ```

---

#### Issue: "Socket mode connection failed"

**Cause**: Invalid app token or network issue

**Solution**:
1. Verify app token in config (starts with `xapp-`)
2. Regenerate token in Slack App settings
3. Check server logs: `.claude/logs/slack-skill-server.log`
4. Restart server: `/slack-skill:server restart`

---

### Appendix G: Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       CLAUDE CODE                               │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Bash Wrapper Skills (.claude/commands/slack-skill/)    │  │
│  │  - ask.md                                                 │  │
│  │  - check.md                                              │  │
│  │  - list.md                                               │  │
│  │  - config.md                                             │  │
│  │  - server.md                                             │  │
│  └─────────────────────────┬────────────────────────────────┘  │
│                            │ invokes                            │
│                            ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Kotlin CLI Application (cli/)                          │  │
│  │  - AskCommand                                            │  │
│  │  - CheckCommand                                          │  │
│  │  - ListCommand                                           │  │
│  │  - ConfigCommand                                         │  │
│  └─────────────────────────┬────────────────────────────────┘  │
└────────────────────────────┼────────────────────────────────────┘
                             │
                             │ uses
                             ▼
        ┌────────────────────────────────────────────┐
        │  Shared Infrastructure (shared/)           │
        │  - ConfigManager                           │
        │  - StateManager                            │
        │  - SlackClientWrapper                      │
        │  - ConsultationRequest (data model)        │
        └─────────────┬──────────────┬───────────────┘
                      │              │
        reads/writes  │              │ uses
                      ▼              ▼
        ┌──────────────────┐  ┌──────────────────────┐
        │  State File      │  │  Slack API           │
        │  (JSON)          │  │  - users.list        │
        │                  │  │  - chat.postMessage  │
        │  .claude/state/  │  │  - Socket Mode       │
        │  pending.json    │  └──────────┬───────────┘
        └──────────────────┘             │
                 ▲                       │ events
                 │                       ▼
                 │          ┌────────────────────────────┐
                 │          │  Kotlin Event Server       │
                 │          │  (server/)                 │
                 │          │  - SlackEventListener      │
                 └──────────┤  - ResponseHandler         │
         updates state      │  - Server Lifecycle Mgmt   │
                            └────────────────────────────┘
                                       ▲
                                       │ managed by
                                       │
                            ┌────────────────────────────┐
                            │  Service Management        │
                            │  - systemd (Linux)         │
                            │  - launchd (macOS)         │
                            │                            │
                            │  scripts/                  │
                            │  - install-service.sh      │
                            │  - start-server.sh         │
                            │  - stop-server.sh          │
                            └────────────────────────────┘
```

---

### Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-14 | Sena / BMad Master | Initial specification (complete) |

---

**END OF DOCUMENT**
