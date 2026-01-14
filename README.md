# Slack Consultation Skill for Claude Code

Enable Claude Code to request asynchronous input from subject matter experts via Slack direct messages.

## Features

- **Asynchronous Consultations**: Send questions to Slack users and retrieve responses later
- **Real-time Response Capture**: Background server listens for Slack replies via Socket Mode
- **User Resolution**: Fuzzy matching on usernames, emails, and custom aliases
- **Automatic Timeout**: Configurable expiration (default 24 hours)
- **Cross-platform**: Works on macOS and Linux with systemd/launchd support

## Quick Start

### 1. Prerequisites

- Java 17+ installed
- A Slack workspace with admin access to create apps
- Claude Code installed

### 2. Create Slack App

1. Go to https://api.slack.com/apps
2. Click "Create New App" → "From scratch"
3. Name: "Claude Consultation Bot", select your workspace

**Enable Socket Mode:**
- Navigate to "Socket Mode" in sidebar
- Toggle "Enable Socket Mode" to ON
- Generate App Token with scope `connections:write`
- Copy the `xapp-...` token

**Configure Bot Permissions:**
- Navigate to "OAuth & Permissions"
- Add Bot Token Scopes:
  - `chat:write`
  - `users:read`
  - `im:read`
  - `im:write`
  - `im:history`
- Click "Install to Workspace"
- Copy the `xoxb-...` Bot User OAuth Token

**Enable Events:**
- Navigate to "Event Subscriptions"
- Toggle "Enable Events" to ON
- Subscribe to bot event: `message.im`
- Save Changes

### 3. Install

```bash
cd /path/to/claude-slack-skill
./scripts/install.sh
```

The installer will:
- Build the project
- Create a configuration template
- Validate your Slack tokens
- Optionally install the server as a system service

### 4. Configuration

Edit `.claude/config/slack-skill.yaml`:

```yaml
slack_bot_token: "xoxb-your-actual-token-here"
slack_app_token: "xapp-your-actual-token-here"
timeout_hours: 24
user_aliases:
  john: "U123ABC"              # Slack user ID
  jane: "jane.doe@company.com" # Email
log_level: INFO
```

Validate configuration:

```bash
./gradlew :cli:run --args="config"
```

### 5. Start the Server

**Option A: As a service (auto-starts on boot)**

Already done if you chose option 1 during installation.

**Option B: Manually**

```bash
./scripts/start-server.sh
```

Check status:

```bash
./scripts/server-status.sh
```

## Usage from Claude Code

### Ask a Question

```
/slack-skill:ask @john "What's the production database password?"
```

Claude will:
1. Resolve the Slack user
2. Send a DM with your question
3. Return a request ID

### Check for Response

```
/slack-skill:check
```

Or check a specific request:

```
/slack-skill:check 550e8400
```

### List All Requests

```
/slack-skill:list
```

### Validate Configuration

```
/slack-skill:config
```

### Manage Server

```
/slack-skill:server status
/slack-skill:server start
/slack-skill:server stop
/slack-skill:server restart
```

## CLI Usage (Direct)

You can also use the CLI directly:

```bash
# Ask a question
./gradlew :cli:run --args="ask @john 'What is X?'"

# Check response
./gradlew :cli:run --args="check"

# List requests
./gradlew :cli:run --args="list"

# Validate config
./gradlew :cli:run --args="config"
```

## Troubleshooting

### "User not found"

- Check spelling of username
- Try using email instead: `user@company.com`
- Add an alias in `.claude/config/slack-skill.yaml`:

```yaml
user_aliases:
  john: "U123ABC"  # Get Slack ID from user profile
```

### "Server not running"

```bash
./scripts/server-status.sh
./scripts/start-server.sh
```

Check logs:

```bash
tail -f .claude/logs/slack-skill-server.log
```

### "Invalid token"

- Verify tokens in `.claude/config/slack-skill.yaml`
- Ensure Socket Mode is enabled in Slack app settings
- Regenerate tokens if needed

### "No response captured"

- Ensure the server is running (`./scripts/server-status.sh`)
- Check server logs for errors
- Verify the Slack user replied to the bot's DM (not a new message)

## Project Structure

```
slack-skill/
├── shared/          # Shared infrastructure
│   ├── config/      # Configuration management
│   ├── state/       # State file operations
│   ├── models/      # Data models
│   └── slack/       # Slack API wrapper
├── cli/             # CLI application
│   └── commands/    # ask, check, list, config
├── server/          # Event server
│   ├── listener/    # Slack event listener
│   └── handler/     # Response handler
├── scripts/         # Service management scripts
└── .claude/
    ├── commands/slack-skill/  # Skill wrappers
    ├── config/               # Configuration
    ├── state/                # Request state
    └── logs/                 # Server logs
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed technical documentation.

## License

MIT

## Support

For issues or questions:
- File an issue on GitHub
- Check logs: `.claude/logs/slack-skill-server.log`
- Run: `./gradlew :cli:run --args="config"` to validate setup
