package com.claude.slack.shared.slack

import com.claude.slack.shared.models.SlackSkillConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class SlackClientWrapperTest {

    // Note: These are unit tests for basic functionality.
    // Full integration tests with real Slack API require valid tokens
    // and should be run separately with WireMock or manual testing.

    @Test
    fun `should create wrapper with valid config`() {
        val config = SlackSkillConfig(
            slackBotToken = "xoxb-test-token",
            slackAppToken = "xapp-test-token"
        )

        val wrapper = SlackClientWrapper(config)

        assertNotNull(wrapper)
    }

    // Additional integration tests would go here with WireMock
    // or against a real test Slack workspace
}
