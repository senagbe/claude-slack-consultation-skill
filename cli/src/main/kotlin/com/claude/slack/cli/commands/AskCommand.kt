package com.claude.slack.cli.commands

import com.claude.slack.shared.config.ConfigManager
import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.slack.SlackClientWrapper
import com.claude.slack.shared.state.StateManager
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class AskCommand(
    private val configManager: ConfigManager,
    private val stateManager: StateManager
) {
    private val logger = LoggerFactory.getLogger(AskCommand::class.java)

    fun execute(args: List<String>): Int {
        if (args.size < 2) {
            System.err.println("Usage: ask <@username|email> <question>")
            System.err.println("Example: ask @john \"What's the production database password?\"")
            return 2
        }

        val userIdentifier = args[0].removePrefix("@")
        val question = args.drop(1).joinToString(" ").trim('"')

        if (question.isBlank()) {
            System.err.println("Error: Question cannot be empty")
            return 2
        }

        return try {
            // Load config
            val config = configManager.loadConfig()

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

            println("✓ Resolved to: ${user.profile?.realName ?: user.name} (${user.id})")

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

            println("✓ Question sent successfully")

            // Store state
            stateManager.addRequest(request)
            println("✓ Consultation request created: ${request.id}")

            // Output summary
            println()
            println("=" * 50)
            println("Request ID: ${request.id}")
            println("User: ${user.profile?.realName} (@${user.name})")
            println("Expires: ${request.expiresAt} (${config.timeoutHours}h)")
            println("=" * 50)
            println()
            println("Use `/slack-skill:check ${request.id}` to retrieve the response.")

            0 // Success
        } catch (e: Exception) {
            logger.error("Failed to execute ask command: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            1
        }
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
