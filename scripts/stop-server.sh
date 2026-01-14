#!/bin/bash
set -e

# Stop the Slack consultation server

cd "$(dirname "$0")/.."

if [ ! -f ".claude/state/server.pid" ]; then
    echo "Server PID file not found"
    echo "Server may not be running"
    exit 0
fi

SERVER_PID=$(cat .claude/state/server.pid)

if ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "Stopping server (PID: $SERVER_PID)..."
    kill $SERVER_PID

    # Wait for graceful shutdown
    sleep 2

    # Force kill if still running
    if ps -p $SERVER_PID > /dev/null 2>&1; then
        echo "Force stopping server..."
        kill -9 $SERVER_PID
    fi

    rm -f .claude/state/server.pid
    echo "✓ Server stopped"
else
    echo "Server (PID: $SERVER_PID) is not running"
    rm -f .claude/state/server.pid
fi
