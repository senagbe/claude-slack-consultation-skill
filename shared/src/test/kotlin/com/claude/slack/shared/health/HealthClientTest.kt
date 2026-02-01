package com.claude.slack.shared.health

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HealthClientTest {

    @Test
    fun `should detect server not running`() {
        // Use a port that's unlikely to have anything running
        val healthClient = HealthClient("http://localhost:59999")

        val result = healthClient.checkHealth()

        assertTrue(result is HealthClient.HealthCheckResult.ServerNotRunning)
    }

    @Test
    fun `should return error for heartbeat when server not running`() {
        val healthClient = HealthClient("http://localhost:59999")

        val result = healthClient.getHeartbeat()

        assertTrue(result is HealthClient.HeartbeatResult.Error)
    }

    @Test
    fun `should report server not ready when not running`() {
        val healthClient = HealthClient("http://localhost:59999")

        val isReady = healthClient.isServerReady()

        assertTrue(!isReady)
    }
}
