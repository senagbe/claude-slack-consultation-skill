---
name: slack-skill:list
description: List all recent Slack consultation requests
---

List all consultation requests from the last 7 days, showing their status.

## Usage

```bash
/slack-skill:list
```

## Implementation

When this skill is invoked, execute the CLI list command:

```bash
./gradlew :cli:run --args="list" --quiet --console=plain
```

Shows a table with:
- Request ID (short)
- User
- Status (ANSWERED, PENDING, EXPIRED)
- Time since asked
- Response time (if answered)
