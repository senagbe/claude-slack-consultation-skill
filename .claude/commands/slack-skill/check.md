---
name: slack-skill:check
description: Check the status of a Slack consultation request
---

Check if a consultation request has been answered, is still pending, or has expired.

## Usage

```bash
/slack-skill:check [request-id]
```

If no request ID is provided, checks the latest pending request.

## Examples

```bash
/slack-skill:check                    # Check latest request
/slack-skill:check 550e8400-e29b-41d4 # Check specific request
```

## Implementation

When this skill is invoked, execute the CLI check command:

```bash
./gradlew :cli:run --args="check $@" --quiet --console=plain
```

Exit codes:
- 0: Response received (answered)
- 1: Still pending
- 2: Request expired
