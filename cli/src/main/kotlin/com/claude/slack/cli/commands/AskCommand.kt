package com.claude.slack.cli.commands

import com.claude.slack.shared.config.ConfigManager
import com.claude.slack.shared.health.HealthClient
import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.slack.SlackClientWrapper
import com.claude.slack.shared.state.StateManagerInterface
import com.claude.slack.shared.util.RetryHelper
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class AskCommand(
    private val configManager: ConfigManager,
    private val stateManager: StateManagerInterface
) {
    private val logger = LoggerFactory.getLogger(AskCommand::class.java)

    fun execute(args: List<String>): Int {
        if (args.size < 2) {
            System.err.println("Usage: ask [--wait] [--timeout MINUTES] <@username|email> <question>")
            System.err.println("Example: ask @john \"What's the production database password?\"")
            System.err.println("Options:")
            System.err.println("  --wait              Wait for response (blocks until answered or timeout)")
            System.err.println("  --timeout MINUTES   Timeout in minutes (default: 10)")
            return 2
        }

        // Parse flags
        val waitForResponse = args.contains("--wait")
        var timeoutMinutes = 10 // Default timeout

        // Parse --timeout flag
        val timeoutIndex = args.indexOf("--timeout")
        if (timeoutIndex != -1 && timeoutIndex + 1 < args.size) {
            try {
                timeoutMinutes = args[timeoutIndex + 1].toInt()
                if (timeoutMinutes <= 0) {
                    System.err.println("Error: Timeout must be a positive number")
                    return 2
                }
            } catch (e: NumberFormatException) {
                System.err.println("Error: Invalid timeout value: ${args[timeoutIndex + 1]}")
                return 2
            }
        }

        val filteredArgs = args.filter { it != "--wait" && it != "--timeout" &&
            (timeoutIndex == -1 || it != args[timeoutIndex + 1]) }

        if (filteredArgs.size < 2) {
            System.err.println("Error: Missing username or question")
            return 2
        }

        val userIdentifier = filteredArgs[0].removePrefix("@")
        val question = filteredArgs.drop(1).joinToString(" ").trim('"')

        if (question.isBlank()) {
            System.err.println("Error: Question cannot be empty")
            return 2
        }

        return try {
            // Load config
            val config = configManager.loadConfig()

            // Pre-flight health check with retry
            val healthClient = HealthClient("http://localhost:${config.healthPort}")

            if (!performHealthCheck(healthClient, config.heartbeatStaleThresholdSeconds)) {
                return 1
            }

            // Initialize Slack client
            val slackClient = SlackClientWrapper(config)

            // Resolve user
            println("Resolving Slack user: $userIdentifier...")
            val user = slackClient.resolveUser(userIdentifier)

            if (user == null) {
                System.err.println("Error: User not found: $userIdentifier")
                System.err.println("Tip: Try using the full name, email, or add an alias in .claude/config/slack-skill.yaml")
                return 1
            }

            println("Resolved to: ${user.profile?.realName ?: user.name} (${user.id})")

            // Create consultation request
            val request = ConsultationRequest(
                slackUserId = user.id,
                slackUsername = "@${user.name}",
                question = question,
                expiresAt = Instant.now().plus(config.timeoutHours.toLong(), ChronoUnit.HOURS)
            )

            // Send DM
            println("Sending question to ${user.profile?.realName}...")
            val message = buildMessage(question)
            slackClient.sendDirectMessage(user.id, message)

            println("Question sent successfully")

            // Store state
            stateManager.addRequest(request)
            println("Consultation request created: ${request.id}")

            // Output summary
            println()
            println("=" * 50)
            println("Request ID: ${request.id}")
            println("User: ${user.profile?.realName} (@${user.name})")
            println("Expires: ${request.expiresAt} (${config.timeoutHours}h)")
            println("=" * 50)
            println()

            // Wait for response if --wait flag is set
            if (waitForResponse) {
                return waitForResponse(request.id, user.profile?.realName ?: user.name, timeoutMinutes, healthClient, config.heartbeatStaleThresholdSeconds)
            }

            println("Use `/slack-skill:check ${request.id}` to retrieve the response.")

            0 // Success
        } catch (e: Exception) {
            logger.error("Failed to execute ask command: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            1
        }
    }

    private fun performHealthCheck(healthClient: HealthClient, staleThresholdSeconds: Int): Boolean {
        println("Checking server health...")

        val retryHelper = RetryHelper.forServerWait(
            maxAttempts = 5,
            initialDelayMs = 2000,
            onRetry = { attempt, delayMs, _ ->
                println("Server not ready, retrying in ${delayMs / 1000}s... (attempt ${attempt + 1}/5)")
            }
        )

        val result = retryHelper.executeWithCondition(
            condition = {
                when (val health = healthClient.checkHealth()) {
                    is HealthClient.HealthCheckResult.Healthy -> {
                        if (health.status == "starting") {
                            println("Server is starting up...")
                            false
                        } else {
                            true
                        }
                    }
                    is HealthClient.HealthCheckResult.Unhealthy -> {
                        println("Server unhealthy: ${health.status}")
                        false
                    }
                    is HealthClient.HealthCheckResult.ServerNotRunning -> {
                        println("Server not running")
                        false
                    }
                    is HealthClient.HealthCheckResult.Error -> {
                        println("Health check error: ${health.message}")
                        false
                    }
                }
            },
            block = { true }
        )

        return when (result) {
            is RetryHelper.RetryResult.Success -> {
                println("Server health check passed")
                true
            }
            is RetryHelper.RetryResult.Failure -> {
                System.err.println("Error: Server is not available after ${result.attempts} attempts")
                System.err.println("Please ensure the server is running: ./scripts/start-server.sh")
                false
            }
        }
    }

    private fun waitForResponse(
        requestId: String,
        userName: String,
        timeoutMinutes: Int,
        healthClient: HealthClient,
        staleThresholdSeconds: Int
    ): Int {
        val pollIntervalSeconds = 3 // Poll every 3 seconds
        val maxAttempts = (timeoutMinutes * 60) / pollIntervalSeconds
        var lastHeartbeatWarning = 0L

        println("Waiting for response from $userName (timeout: ${timeoutMinutes}m)...")
        println()

        for (attempt in 1..maxAttempts) {
            Thread.sleep(pollIntervalSeconds * 1000L)

            // Check heartbeat periodically (every 30 seconds)
            if (attempt % 10 == 0) {
                when (val heartbeat = healthClient.getHeartbeat()) {
                    is HealthClient.HeartbeatResult.Success -> {
                        if (heartbeat.isStale && System.currentTimeMillis() - lastHeartbeatWarning > 60000) {
                            System.err.println("Warning: Server heartbeat is stale. The server may have stopped.")
                            lastHeartbeatWarning = System.currentTimeMillis()
                        }
                    }
                    is HealthClient.HeartbeatResult.NotFound -> {
                        if (System.currentTimeMillis() - lastHeartbeatWarning > 60000) {
                            System.err.println("Warning: No heartbeat found. Server may not be running.")
                            lastHeartbeatWarning = System.currentTimeMillis()
                        }
                    }
                    is HealthClient.HeartbeatResult.Error -> {
                        logger.debug("Heartbeat check error: ${heartbeat.message}")
                    }
                }
            }

            val request = stateManager.getRequest(requestId)

            if (request == null) {
                System.err.println("Error: Request not found: $requestId")
                return 1
            }

            when {
                request.status == com.claude.slack.shared.models.Status.ANSWERED && request.response != null -> {
                    println("Response received from $userName")
                    println()
                    println("=" * 50)
                    println(request.response)
                    println("=" * 50)
                    return 0 // Success
                }
                request.isExpired() -> {
                    System.err.println("Request expired without response")
                    return 2
                }
                attempt % 10 == 0 -> {
                    // Print progress every 30 seconds
                    val elapsed = (attempt * pollIntervalSeconds) / 60
                    println("Still waiting... (${elapsed}m elapsed)")
                }
            }
        }

        // Timeout
        println()
        println("Timeout: No response received within ${timeoutMinutes} minutes")
        println("You can check later with: /slack-skill:check $requestId")
        return 1
    }

    private fun buildMessage(question: String): String {
        return """
            :wave: *Claude Code Consultation Request*

            $question

            _Reply to this message to send your response back to Claude._
        """.trimIndent()
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
