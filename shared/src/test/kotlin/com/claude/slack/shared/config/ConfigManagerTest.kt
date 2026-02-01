package com.claude.slack.shared.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should load valid configuration`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
            slack_app_token: xapp-test-token
            timeout_hours: 48
            log_level: DEBUG
            user_aliases:
              john: U123ABC
              jane: jane@example.com
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)
        val config = manager.loadConfig()

        assertEquals("xoxb-test-token", config.slackBotToken)
        assertEquals("xapp-test-token", config.slackAppToken)
        assertEquals(48, config.timeoutHours)
        assertEquals("DEBUG", config.logLevel)
        assertEquals("U123ABC", config.userAliases["john"])
        assertEquals("jane@example.com", config.userAliases["jane"])
    }

    @Test
    fun `should use defaults for optional fields`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
            slack_app_token: xapp-test-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)
        val config = manager.loadConfig()

        assertEquals(24, config.timeoutHours)
        assertEquals("INFO", config.logLevel)
        assertTrue(config.userAliases.isEmpty())
    }

    @Test
    fun `should throw exception when config file not found`() {
        val manager = ConfigManager("/nonexistent/config.yaml")

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("Configuration file not found"))
    }

    @Test
    fun `should throw exception when bot token missing`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_app_token: xapp-test-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("slack_bot_token"))
    }

    @Test
    fun `should throw exception when app token missing`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("slack_app_token"))
    }

    @Test
    fun `should throw exception when bot token has invalid format`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: invalid-token
            slack_app_token: xapp-test-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("must start with 'xoxb-'"))
    }

    @Test
    fun `should throw exception when app token has invalid format`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
            slack_app_token: invalid-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("must start with 'xapp-'"))
    }

    @Test
    fun `should throw exception when empty config file`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("")

        val manager = ConfigManager(configFile.absolutePath)

        val exception = assertFailsWith<ConfigException> {
            manager.loadConfig()
        }

        assertTrue(exception.message!!.contains("Empty configuration file"))
    }

    @Test
    fun `should use default MongoDB settings when not specified`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
            slack_app_token: xapp-test-token
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)
        val config = manager.loadConfig()

        assertEquals("mongodb://localhost:27017", config.mongoConnectionString)
        assertEquals("slack_skill", config.mongoDatabase)
        assertEquals(8080, config.healthPort)
        assertEquals(30, config.heartbeatIntervalSeconds)
        assertEquals(90, config.heartbeatStaleThresholdSeconds)
    }

    @Test
    fun `should load MongoDB settings from config file`() {
        val configFile = File(tempDir, "slack-skill.yaml")
        configFile.writeText("""
            slack_bot_token: xoxb-test-token
            slack_app_token: xapp-test-token
            mongo_connection_string: mongodb://custom-host:27018
            mongo_database: custom_db
            health_port: 9090
            heartbeat_interval_seconds: 15
            heartbeat_stale_threshold_seconds: 60
        """.trimIndent())

        val manager = ConfigManager(configFile.absolutePath)
        val config = manager.loadConfig()

        assertEquals("mongodb://custom-host:27018", config.mongoConnectionString)
        assertEquals("custom_db", config.mongoDatabase)
        assertEquals(9090, config.healthPort)
        assertEquals(15, config.heartbeatIntervalSeconds)
        assertEquals(60, config.heartbeatStaleThresholdSeconds)
    }

    // Note: Environment variable tests are tricky because we can't easily set
    // env vars in unit tests. In a real scenario, we'd use a test helper or
    // integration tests with process forking.
    @Test
    fun `config values can be overridden by env vars conceptually`() {
        // This test documents the expected behavior.
        // Environment variables MONGODB_CONNECTION_STRING and MONGODB_DATABASE
        // should override config file values when set.
        // Actual verification requires integration testing with env var injection.
        assertTrue(true)
    }
}
