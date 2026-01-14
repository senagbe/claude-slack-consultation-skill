#!/bin/bash
set -e

# Install Slack consultation server as a system service

cd "$(dirname "$0")/.."
PROJECT_DIR=$(pwd)

detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    else
        echo "unknown"
    fi
}

install_macos() {
    echo "Installing as launchd service on macOS..."

    cat > ~/Library/LaunchAgents/com.claude.slack-skill.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.claude.slack-skill</string>
    <key>ProgramArguments</key>
    <array>
        <string>$PROJECT_DIR/server/build/install/server/bin/server</string>
    </array>
    <key>WorkingDirectory</key>
    <string>$PROJECT_DIR</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>$PROJECT_DIR/.claude/logs/slack-skill-server.log</string>
    <key>StandardErrorPath</key>
    <string>$PROJECT_DIR/.claude/logs/slack-skill-server-error.log</string>
</dict>
</plist>
EOF

    launchctl load ~/Library/LaunchAgents/com.claude.slack-skill.plist
    echo "✓ Service installed and started"
    echo "  Control with: launchctl [start|stop|list] com.claude.slack-skill"
}

install_linux() {
    echo "Installing as systemd service on Linux..."

    sudo tee /etc/systemd/system/slack-skill-server.service > /dev/null <<EOF
[Unit]
Description=Slack Consultation Skill Event Server
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$PROJECT_DIR
ExecStart=$PROJECT_DIR/server/build/install/server/bin/server
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable slack-skill-server
    sudo systemctl start slack-skill-server

    echo "✓ Service installed and started"
    echo "  Control with: sudo systemctl [start|stop|status] slack-skill-server"
}

# Main installation
OS=$(detect_os)

echo "Detected OS: $OS"
echo

# Build server first
echo "Building server..."
./gradlew :server:installDist --quiet
echo "✓ Server built"
echo

case "$OS" in
    macos)
        install_macos
        ;;
    linux)
        install_linux
        ;;
    *)
        echo "Error: Unsupported operating system: $OSTYPE"
        echo "Manual installation required"
        exit 1
        ;;
esac

echo
echo "✓ Installation complete"
