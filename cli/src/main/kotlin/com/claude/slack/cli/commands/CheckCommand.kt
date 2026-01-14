package com.claude.slack.cli.commands

import com.claude.slack.shared.models.Status
import com.claude.slack.shared.state.StateManager
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class CheckCommand(private val stateManager: StateManager) {

    private val logger = LoggerFactory.getLogger(CheckCommand::class.java)

    fun execute(args: List<String>): Int {
        return try {
            val requestId = if (args.isNotEmpty()) {
                args[0]
            } else {
                // Get latest pending request
                val latest = stateManager.getLatestPendingRequest()
                if (latest == null) {
                    System.err.println("No pending consultation requests found")
                    System.err.println("Tip: Create a request with `/slack-skill:ask`")
                    return 1
                }
                latest.id
            }

            val request = stateManager.getRequest(requestId)

            if (request == null) {
                System.err.println("Error: Request not found: $requestId")
                return 1
            }

            // Check if expired (and update if needed)
            if (request.isExpired() && request.status == Status.PENDING) {
                stateManager.updateRequest(requestId) { it.markExpired() }
                return printExpired(requestId, request.expiresAt)
            }

            return when (request.status) {
                Status.ANSWERED -> printAnswered(request.slackUsername, request.response!!, request.respondedAt!!)
                Status.PENDING -> printPending(request.slackUsername, request.createdAt, request.expiresAt)
                Status.EXPIRED -> printExpired(requestId, request.expiresAt)
            }

        } catch (e: Exception) {
            logger.error("Failed to execute check command: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            1
        }
    }

    private fun printAnswered(username: String, response: String, respondedAt: Instant): Int {
        val ago = formatDuration(Duration.between(respondedAt, Instant.now()))

        println()
        println("✓ Response received from $username")
        println("Responded: $ago ago")
        println()
        println("─" * 50)
        println(response)
        println("─" * 50)
        println()

        return 0 // Success - answered
    }

    private fun printPending(username: String, createdAt: Instant, expiresAt: Instant): Int {
        val askedAgo = formatDuration(Duration.between(createdAt, Instant.now()))
        val remaining = formatDuration(Duration.between(Instant.now(), expiresAt))

        println()
        println("⏳ Awaiting response from $username")
        println("Asked: $askedAgo ago")
        println("Expires: in $remaining")
        println()

        return 1 // Pending
    }

    private fun printExpired(requestId: String, expiresAt: Instant): Int {
        val expiredAgo = formatDuration(Duration.between(expiresAt, Instant.now()))

        println()
        println("⏱  Request expired: $requestId")
        println("Expired: $expiredAgo ago")
        println()
        println("Tip: Create a new request with `/slack-skill:ask`")
        println()

        return 2 // Expired
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${duration.seconds}s"
        }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
