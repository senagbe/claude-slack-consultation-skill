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
}
