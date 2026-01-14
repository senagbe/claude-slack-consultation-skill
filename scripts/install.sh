#!/bin/bash
set -e

# Auto-installer for Slack Consultation Skill

echo "========================================"
echo "  Slack Consultation Skill Installer"
echo "========================================"
echo

cd "$(dirname "$0")/.."
PROJECT_DIR=$(pwd)

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    echo "Please install Java 17+ and try again"
    exit 1
fi

echo "✓ Java is installed"

# Build the project
echo
echo "Building project..."
./gradlew build --quiet
echo "✓ Project built successfully"

# Install distributions
echo
echo "Installing CLI and server distributions..."
./gradlew installDist --quiet
echo "✓ Distributions installed"

# Check for config file
echo
echo "Checking configuration..."

CONFIG_FILE=".claude/config/slack-skill.yaml"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found"
    echo
    echo "Creating template configuration at $CONFIG_FILE..."

    mkdir -p .claude/config

    cat > "$CONFIG_FILE" <<EOF
# Slack Consultation Skill Configuration

# Get these tokens from https://api.slack.com/apps
slack_bot_token: "xoxb-your-bot-token-here"
slack_app_token: "xapp-your-app-token-here"

# Timeout for consultation requests (hours)
timeout_hours: 24

# User aliases (optional) - map friendly names to Slack IDs or emails
user_aliases:
  # john: "U123ABC"
  # jane: "jane.doe@company.com"

# Logging level (DEBUG, INFO, WARN, ERROR)
log_level: INFO
EOF

    echo "✓ Template configuration created"
    echo
    echo "⚠️  IMPORTANT: You must edit $CONFIG_FILE and add your Slack tokens"
    echo
    echo "To get your tokens:"
    echo "  1. Go to https://api.slack.com/apps"
    echo "  2. Create a new app or select an existing one"
    echo "  3. Enable Socket Mode and create an app token (xapp-...)"
    echo "  4. Install the app to your workspace and get the bot token (xoxb-...)"
    echo "  5. Add both tokens to $CONFIG_FILE"
    echo
    echo "For detailed instructions, see README.md"
    echo
    read -p "Press Enter to continue after updating the configuration..."
fi

# Validate configuration
echo
echo "Validating configuration..."
if ./gradlew :cli:run --args="config" --quiet --console=plain; then
    echo "✓ Configuration is valid"
else
    echo "✗ Configuration validation failed"
    echo "Please fix the errors and run this installer again"
    exit 1
fi

# Offer to install as service
echo
echo "Server installation options:"
echo "  1. Install as system service (auto-start on boot)"
echo "  2. Manual start only (use ./scripts/start-server.sh)"
echo

read -p "Choose option [1/2]: " choice

case "$choice" in
    1)
        ./scripts/install-service.sh
        ;;
    2)
        echo "Skipping service installation"
        echo "You can install later with: ./scripts/install-service.sh"
        ;;
    *)
        echo "Invalid choice, skipping service installation"
        ;;
esac

# Final summary
echo
echo "========================================"
echo "  Installation Complete!"
echo "========================================"
echo
echo "Next steps:"
echo "  1. Start the server (if not installed as service):"
echo "     ./scripts/start-server.sh"
echo
echo "  2. Test the CLI:"
echo "     ./gradlew :cli:run --args=\"config\""
echo
echo "  3. Use from Claude Code:"
echo "     /slack-skill:ask @username \"Your question\""
echo "     /slack-skill:check"
echo "     /slack-skill:list"
echo
echo "For help:"
echo "  ./gradlew :cli:run --args=\"help\""
echo "  or see README.md"
echo
