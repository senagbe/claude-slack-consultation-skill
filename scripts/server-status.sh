#!/bin/bash

# Check if the Slack consultation server is running

cd "$(dirname "$0")/.."

if [ ! -f ".claude/state/server.pid" ]; then
    echo "Server is NOT running (no PID file)"
    exit 1
fi

SERVER_PID=$(cat .claude/state/server.pid)

if ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "✓ Server is RUNNING (PID: $SERVER_PID)"
    exit 0
else
    echo "✗ Server is NOT running (stale PID file)"
    rm -f .claude/state/server.pid
    exit 1
fi
