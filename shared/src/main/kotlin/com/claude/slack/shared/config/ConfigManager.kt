package com.claude.slack.shared.config

import com.claude.slack.shared.models.SlackSkillConfig
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

class ConfigManager(private val configPath: String = ".claude/config/slack-skill.yaml") {

    fun loadConfig(): SlackSkillConfig {
        val configFile = File(configPath)

        if (!configFile.exists()) {
            throw ConfigException(
                "Configuration file not found: $configPath\n" +
                "Please create this file with your Slack tokens.\n" +
                "See README.md for setup instructions."
            )
        }

        return try {
            val yaml = Yaml()
            @Suppress("UNCHECKED_CAST")
            val configMap = FileInputStream(configFile).use { input ->
                yaml.load(input) as? Map<String, Any>
            } ?: throw ConfigException("Empty configuration file: $configPath")

            val config = SlackSkillConfig(
                slackBotToken = configMap["slack_bot_token"]?.toString()
                    ?: throw ConfigException("Missing required field: slack_bot_token"),
                slackAppToken = configMap["slack_app_token"]?.toString()
                    ?: throw ConfigException("Missing required field: slack_app_token"),
                timeoutHours = (configMap["timeout_hours"] as? Number)?.toInt() ?: 24,
                userAliases = parseUserAliases(configMap["user_aliases"]),
                logLevel = configMap["log_level"]?.toString()?.uppercase() ?: "INFO",
                mongoConnectionString = getEnvOrConfig(
                    "MONGODB_CONNECTION_STRING",
                    configMap["mongo_connection_string"]?.toString(),
                    "mongodb://localhost:27017"
                ),
                mongoDatabase = getEnvOrConfig(
                    "MONGODB_DATABASE",
                    configMap["mongo_database"]?.toString(),
                    "slack_skill"
                ),
                healthPort = (configMap["health_port"] as? Number)?.toInt() ?: 8080,
                heartbeatIntervalSeconds = (configMap["heartbeat_interval_seconds"] as? Number)?.toInt() ?: 30,
                heartbeatStaleThresholdSeconds = (configMap["heartbeat_stale_threshold_seconds"] as? Number)?.toInt() ?: 90
            )

            config.validate()
            config

        } catch (e: ConfigException) {
            throw e
        } catch (e: Exception) {
            throw ConfigException("Failed to parse configuration file: ${e.message}", e)
        }
    }

    private fun getEnvOrConfig(envVar: String, configValue: String?, default: String): String {
        return System.getenv(envVar) ?: configValue ?: default
    }

    private fun parseUserAliases(aliases: Any?): Map<String, String> {
        return when (aliases) {
            is Map<*, *> -> aliases.mapKeys { it.key.toString() }
                .mapValues { it.value.toString() }
            null -> emptyMap()
            else -> throw ConfigException("user_aliases must be a map of alias -> slack_id/email")
        }
    }
}

class ConfigException(message: String, cause: Throwable? = null) : Exception(message, cause)
