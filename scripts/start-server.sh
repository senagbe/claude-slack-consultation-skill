#!/bin/bash
set -e

# Start the Slack consultation server in the background

cd "$(dirname "$0")/.."

echo "Starting Slack Consultation Server..."

# Build the server if not already built
if [ ! -f "server/build/install/server/bin/server" ]; then
    echo "Server not built, building now..."
    ./gradlew :server:installDist --quiet
fi

# Start server in background
nohup server/build/install/server/bin/server >> .claude/logs/slack-skill-server.log 2>&1 &
SERVER_PID=$!

echo "Server started (PID: $SERVER_PID)"
echo $SERVER_PID > .claude/state/server.pid

echo "✓ Server is running"
echo "  Logs: .claude/logs/slack-skill-server.log"
echo "  PID file: .claude/state/server.pid"
