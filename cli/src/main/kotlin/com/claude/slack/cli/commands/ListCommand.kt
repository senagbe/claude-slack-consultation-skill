package com.claude.slack.cli.commands

import com.claude.slack.shared.models.Status
import com.claude.slack.shared.state.StateManager
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class ListCommand(private val stateManager: StateManager) {

    private val logger = LoggerFactory.getLogger(ListCommand::class.java)

    fun execute(args: List<String>): Int {
        return try {
            val state = stateManager.readState()

            // Filter to last 7 days
            val cutoff = Instant.now().minus(7, ChronoUnit.DAYS)
            val recentRequests = state.requests.filter { it.createdAt.isAfter(cutoff) }
                .sortedByDescending { it.createdAt }

            if (recentRequests.isEmpty()) {
                println("No consultation requests in the last 7 days")
                return 0
            }

            println()
            println("Consultation Requests (Last 7 Days)")
            println("=" * 70)
            println()

            // Print table header
            println(String.format("%-12s %-15s %-10s %-15s %-15s",
                "ID (short)", "User", "Status", "Asked", "Response"))
            println("─" * 70)

            // Print rows
            for (request in recentRequests) {
                val shortId = request.id.take(8)
                val user = request.slackUsername
                val status = formatStatus(request.status)
                val asked = formatDuration(Duration.between(request.createdAt, Instant.now()))
                val response = if (request.respondedAt != null) {
                    formatDuration(Duration.between(request.createdAt, request.respondedAt))
                } else {
                    "-"
                }

                println(String.format("%-12s %-15s %-10s %-15s %-15s",
                    shortId, user, status, "$asked ago", response))
            }

            println()
            println("${recentRequests.size} requests in last 7 days")
            println()

            0 // Success

        } catch (e: Exception) {
            logger.error("Failed to execute list command: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            1
        }
    }

    private fun formatStatus(status: Status): String {
        return when (status) {
            Status.ANSWERED -> "✓ ANSWERED"
            Status.PENDING -> "⏳ PENDING"
            Status.EXPIRED -> "⏱  EXPIRED"
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return when {
            hours > 24 -> "${hours / 24}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${duration.seconds}s"
        }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
