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

When this skill is invoked, execute the CLI ask command:

```bash
./gradlew :cli:run --args="ask $@" --quiet --console=plain
```

The CLI will:
1. Resolve the Slack user by username or email
2. Send a DM with the question
3. Store the request with a unique ID
4. Return the request ID for later checking

Use `/slack-skill:check` to retrieve the response once the user replies.
