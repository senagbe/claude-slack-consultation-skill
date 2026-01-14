package com.claude.slack.cli.commands

import com.claude.slack.shared.config.ConfigManager
import com.claude.slack.shared.slack.SlackClientWrapper
import org.slf4j.LoggerFactory

class ConfigCommand(private val configManager: ConfigManager) {

    private val logger = LoggerFactory.getLogger(ConfigCommand::class.java)

    fun execute(args: List<String>): Int {
        return try {
            println("Validating configuration...")
            println()

            // Load config
            val config = configManager.loadConfig()
            println("✓ Configuration file loaded: .claude/config/slack-skill.yaml")

            // Show sanitized tokens
            println()
            println("Configuration:")
            println("─" * 50)
            println("  Bot Token: ${sanitizeToken(config.slackBotToken)}")
            println("  App Token: ${sanitizeToken(config.slackAppToken)}")
            println("  Timeout: ${config.timeoutHours} hours")
            println("  Log Level: ${config.logLevel}")
            println("  User Aliases: ${config.userAliases.size} defined")
            println("─" * 50)
            println()

            // Test Slack connection
            println("Testing Slack API connection...")
            val slackClient = SlackClientWrapper(config)
            val connectionOk = slackClient.testConnection()

            if (connectionOk) {
                println("✓ Slack connection successful")
                println()
                println("Ready to use!")
                0
            } else {
                System.err.println("✗ Slack connection failed")
                System.err.println("Check your tokens and try again")
                1
            }

        } catch (e: Exception) {
            logger.error("Failed to validate config: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            1
        }
    }

    private fun sanitizeToken(token: String): String {
        return if (token.length > 8) {
            "${token.take(8)}****"
        } else {
            "****"
        }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
