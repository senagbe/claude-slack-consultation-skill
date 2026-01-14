---
title: 'Slack Consultation Skill for Claude Code'
slug: 'slack-consultation-skill'
created: '2026-01-14'
status: 'in-progress'
stepsCompleted: [1]
tech_stack: ['Kotlin', 'Gradle', 'Slack Bolt SDK for Java', 'Bash']
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
- Bash wrapper skill: `/slack-skill:ask @username "question text"`
- Kotlin service (Gradle-based) with Slack Bolt SDK integration
- Slack user resolution via Slack API (users.list, search by name/email)
- Send question as Slack DM to resolved user
- File-based state management (`.claude/state/slack-skill-pending.json`)
- 24-hour timeout with expiration tracking
- Response retrieval command: `/slack-skill:check [request-id]`
- Auto-installation of dependencies on first run (Gradle wrapper)
- Configuration file: `.claude/config/slack-skill.yaml` (Slack bot token, optional user aliases)

**Out of Scope:**
- Background daemon / autonomous Claude invocation without user approval
- Multi-channel notifications (only DMs for now)
- Agent-to-agent consultation (human-only in v1)
- Slack workspace management or admin features
- Response threading or conversation history
- Integration with other messaging platforms (Discord, Teams, etc.)
- Native binary compilation (GraalVM) - future enhancement

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
- **User-triggered only** (Architecture A) - no background daemon
- Bash wrapper invokes Kotlin CLI application
- First-run auto-installation via `./gradlew installDist`

**State & Configuration:**
- **State**: JSON files in `.claude/state/slack-skill-pending.json`
- **Config**: YAML in `.claude/config/slack-skill.yaml`
- Slack bot token stored in config
- Optional user alias mappings in config

**Slack Integration:**
- Use Slack Bolt SDK for API access
- User lookup via `users.list` + fuzzy matching on display name/real name
- Fallback to email-based lookup if configured
- Send DMs via `chat.postMessage` to user's DM channel

**Timeout Handling:**
- Default: 24 hours (86400 seconds)
- Store `expires_at` timestamp in state
- `/slack-skill:check` command reports status: pending/answered/expired
- Expired requests don't block - return "No response received" status

## Implementation Plan

### Tasks

(To be filled in Step 2: Investigation)

### Acceptance Criteria

(To be filled in Step 3: Generate Spec)

## Additional Context

### Dependencies

- Slack Bolt SDK for Java: `com.slack.api:bolt` (MIT license)
- Kotlin standard library
- Gradle 8.x+ for build management
- Jackson or kotlinx.serialization for JSON handling
- SnakeYAML for YAML config parsing

### Testing Strategy

(To be defined in Step 2: Investigation)

### Notes

- First skill built by user - keep implementation simple and well-documented
- Consider adding `/slack-skill:list` to show pending requests
- Consider adding `/slack-skill:config` to validate Slack bot token
- Future: Add interactive Slack app with buttons for quick responses
