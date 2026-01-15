# Integration Test for Slack Consultation Skill

This test verifies that the skill works end-to-end within Claude Code conversations.

## Test Scenario

We'll simulate a realistic use case where Claude Code needs information from a subject matter expert (you) to proceed with a task.

## Test Steps

### 1. Initial Setup

Ensure the server is running:
```bash
./scripts/server-status.sh
```

If not running, start it:
```bash
./scripts/start-server.sh
```

### 2. Test from Claude Code CLI

Run this test in a Claude Code conversation:

**Prompt to Claude Code:**
```
I need you to test the Slack consultation skill integration. Here's the test:

1. Send me a consultation request asking: "What is the database connection string for the staging environment?"
2. Wait for me to reply in Slack
3. Once you receive my response, create a file called test-result.txt containing the response I provided
4. Confirm the test completed successfully

Use the slack-skill:ask and slack-skill:check commands to do this.
```

### 3. Expected Flow

1. **Claude Code sends the question via Slack:**
   - You should receive a DM from "Claude Consultation" bot
   - The message should contain: "What is the database connection string for the staging environment?"

2. **You reply in Slack:**
   - Reply to the bot's message with: `postgres://staging:password@db.staging.example.com:5432/app_db`

3. **Claude Code retrieves the response:**
   - Claude should detect the response (may need to manually run `/slack-skill:check`)
   - Claude should create `test-result.txt` with your response

4. **Verification:**
   - Check that `test-result.txt` exists and contains your Slack response
   - Verify the server logs show the message was received: `tail -50 .claude/logs/slack-skill-server.log`

### 4. Success Criteria

✓ Consultation request sent successfully
✓ Message appears in Slack DM
✓ Reply can be sent from Slack (no "turned off" message)
✓ Server captures the reply (check logs)
✓ Claude Code retrieves the response
✓ Claude Code uses the response to complete the task

## Automated Test

Alternatively, you can run this automated test script:

```bash
#!/bin/bash
set -e

echo "=== Slack Consultation Skill Integration Test ==="
echo

# Step 1: Send consultation request
echo "Step 1: Sending consultation request..."
REQUEST_OUTPUT=$(./gradlew :cli:run --args="ask @sena \"INTEGRATION TEST: Reply with 'TEST_PASSED_$(date +%s)'\"" --quiet --console=plain 2>&1)
REQUEST_ID=$(echo "$REQUEST_OUTPUT" | grep -oE '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}' | head -1)

echo "✓ Request sent: $REQUEST_ID"
echo

# Step 2: Wait for user to reply
echo "Step 2: Please reply to the message in Slack..."
echo "Press Enter after you've replied in Slack"
read -p ""

# Step 3: Check for response
echo "Step 3: Checking for response..."
CHECK_OUTPUT=$(./gradlew :cli:run --args="check $REQUEST_ID" --quiet --console=plain 2>&1)

if echo "$CHECK_OUTPUT" | grep -q "Response received"; then
    echo "✓ Response captured successfully"
    echo
    echo "Response content:"
    echo "$CHECK_OUTPUT" | sed -n '/──────────────────────────────────────────────────/,/──────────────────────────────────────────────────/p' | grep -v "────"
    echo
    echo "=== TEST PASSED ==="
else
    echo "✗ No response received"
    echo "=== TEST FAILED ==="
    exit 1
fi
```

Save this as `test-integration.sh`, make it executable (`chmod +x test-integration.sh`), and run it.

## Troubleshooting Test Failures

### Response not captured
- Check server is running: `./scripts/server-status.sh`
- Check server logs: `tail -100 .claude/logs/slack-skill-server.log | grep -i "received\|error"`
- Verify you replied to the bot's message (not sent a new message)

### Cannot reply in Slack
- See "Sending messages to this app has been turned off" troubleshooting in README.md
- Verify Messages Tab is enabled in App Home
- Try closing and reopening the DM conversation

### Claude Code doesn't use the response
- This means the skill is working, but Claude isn't properly incorporating the response
- Verify the skill's wrapper scripts are correctly passing data back to Claude
- Check `.claude/commands/slack-skill/check.sh` is returning data correctly
