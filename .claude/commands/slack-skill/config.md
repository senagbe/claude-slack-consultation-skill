---
name: slack-skill:config
description: Validate Slack configuration and test API connection
---

Validate the configuration file and test connectivity to Slack API.

## Usage

```bash
/slack-skill:config
```

## Implementation

When this skill is invoked, execute the CLI config command:

```bash
./gradlew :cli:run --args="config" --quiet --console=plain
```

This will:
1. Load and validate `.claude/config/slack-skill.yaml`
2. Show sanitized configuration (tokens masked)
3. Test Slack API connection with bot/app tokens
4. Report success or failure

If configuration is missing or invalid, shows helpful error messages.
