---
name: slack-skill:server
description: Manage the Slack event server (start/stop/status)
---

Manage the background server that listens for Slack responses.

## Usage

```bash
/slack-skill:server [start|stop|status|restart|install]
```

## Commands

- **start**: Start the server in the background
- **stop**: Stop the running server
- **status**: Check if the server is running
- **restart**: Stop and start the server
- **install**: Install the server as a system service (systemd/launchd)

## Examples

```bash
/slack-skill:server status
/slack-skill:server start
/slack-skill:server stop
```

## Implementation

When this skill is invoked with a subcommand, execute the corresponding script:

```bash
case "$1" in
  start)
    ./scripts/start-server.sh
    ;;
  stop)
    ./scripts/stop-server.sh
    ;;
  status)
    ./scripts/server-status.sh
    ;;
  restart)
    ./scripts/stop-server.sh && ./scripts/start-server.sh
    ;;
  install)
    ./scripts/install-service.sh
    ;;
  *)
    echo "Usage: /slack-skill:server [start|stop|status|restart|install]"
    exit 2
    ;;
esac
```

The server must be running to capture Slack responses in real-time.
