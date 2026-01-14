package com.claude.slack.cli

import com.claude.slack.shared.config.ConfigManager
import com.claude.slack.shared.state.StateManager
import com.claude.slack.cli.commands.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(2)
    }

    val command = args[0]
    val commandArgs = args.drop(1)

    // Initialize shared components
    val configManager = ConfigManager()
    val stateManager = StateManager()

    val exitCode = try {
        when (command) {
            "ask" -> AskCommand(configManager, stateManager).execute(commandArgs)
            "check" -> CheckCommand(stateManager).execute(commandArgs)
            "list" -> ListCommand(stateManager).execute(commandArgs)
            "config" -> ConfigCommand(configManager).execute(commandArgs)
            "help", "--help", "-h" -> {
                printUsage()
                0
            }
            else -> {
                System.err.println("Unknown command: $command")
                printUsage()
                2
            }
        }
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace(System.err)
        1
    }

    exitProcess(exitCode)
}

private fun printUsage() {
    println("""
        Slack Consultation Skill - CLI

        Usage:
          ask <@user|email> <question>    Send a consultation request to a Slack user
          check [request-id]               Check status of a request (latest if no ID provided)
          list                             List all recent consultation requests
          config                           Validate configuration and test Slack connection
          help                             Show this help message

        Examples:
          ask @john "What's the production database password?"
          check 550e8400
          list
          config

        Configuration:
          Edit .claude/config/slack-skill.yaml to set your Slack tokens and preferences.

        For more information, see README.md
    """.trimIndent())
}
