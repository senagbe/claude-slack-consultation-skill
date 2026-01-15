---
name: slack-skill:ask
description: Send a consultation request to a Slack user
---

Send a consultation request to a subject matter expert via Slack direct message.

## Usage

```bash
/slack-skill:ask @username "Your question here"
/slack-skill:ask user@email.com "Your question here"
```

## Examples

```bash
/slack-skill:ask @john "What's the production database password?"
/slack-skill:ask security@company.com "Can we deploy to prod today?"
```

## Implementation

When this skill is invoked, execute the CLI ask command with --wait flag:

```bash
./gradlew :cli:run --args="ask --wait --timeout MINUTES $@" --quiet --console=plain
```

**Parsing timeout from user input:**
- Extract the timeout duration from the user's message
- Look for phrases like "wait X minutes", "timeout of X minutes", "wait up to X minutes"
- If no timeout specified, use default: `--timeout 10`
- If user specifies timeout, use: `--timeout X` where X is the number of minutes

**Examples:**
- User: "Ask @john for the password" → Use `--timeout 10` (default)
- User: "Ask @john for the password, wait 5 minutes" → Use `--timeout 5`
- User: "Ask @john for approval, timeout 15 minutes" → Use `--timeout 15`
- User: "Ask @john, I can wait up to 20 minutes" → Use `--timeout 20`

The CLI will:
1. Resolve the Slack user by username or email
2. Send a DM with the question
3. Wait for the user to reply (blocks for up to timeout minutes, default 10)
4. Return the response content when received
5. Exit with code 0 if response received, 1 if timeout, 2 if expired

**IMPORTANT:** This command blocks while waiting for a response. Claude Code will be paused until:
- The user replies in Slack (response returned immediately)
- Timeout expires (can check manually later with `/slack-skill:check`)

**After executing this skill:**
- If exit code is 0: Parse the response from stdout (between the ══ separator lines) and use it to complete the task
- If exit code is 1: Inform user the response hasn't arrived yet and provide the request ID for manual checking
- If exit code is 2: Inform user the request expired
