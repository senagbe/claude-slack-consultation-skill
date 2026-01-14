package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should read empty state when file does not exist`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val state = manager.readState()

        assertTrue(state.requests.isEmpty())
    }

    @Test
    fun `should add and read consultation request`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val request = ConsultationRequest(
            id = "test-id-123",
            slackUserId = "U123ABC",
            slackUsername = "@john",
            question = "What is X?",
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )

        manager.addRequest(request)
        val retrieved = manager.getRequest("test-id-123")

        assertNotNull(retrieved)
        assertEquals("test-id-123", retrieved.id)
        assertEquals("U123ABC", retrieved.slackUserId)
        assertEquals("@john", retrieved.slackUsername)
        assertEquals("What is X?", retrieved.question)
        assertEquals(Status.PENDING, retrieved.status)
    }

    @Test
    fun `should update consultation request`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val request = ConsultationRequest(
            id = "test-id-123",
            slackUserId = "U123ABC",
            slackUsername = "@john",
            question = "What is X?",
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )

        manager.addRequest(request)
        manager.updateRequest("test-id-123") { it.withResponse("The answer is 42") }

        val updated = manager.getRequest("test-id-123")

        assertNotNull(updated)
        assertEquals(Status.ANSWERED, updated.status)
        assertEquals("The answer is 42", updated.response)
        assertNotNull(updated.respondedAt)
    }

    @Test
    fun `should get latest pending request`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val request1 = ConsultationRequest(
            id = "test-id-1",
            slackUserId = "U123ABC",
            slackUsername = "@john",
            question = "Question 1",
            createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            expiresAt = Instant.now().plus(22, ChronoUnit.HOURS)
        )

        val request2 = ConsultationRequest(
            id = "test-id-2",
            slackUserId = "U456DEF",
            slackUsername = "@jane",
            question = "Question 2",
            createdAt = Instant.now().minus(1, ChronoUnit.HOURS),
            expiresAt = Instant.now().plus(23, ChronoUnit.HOURS)
        )

        manager.addRequest(request1)
        manager.addRequest(request2)

        val latest = manager.getLatestPendingRequest()

        assertNotNull(latest)
        assertEquals("test-id-2", latest.id)
    }

    @Test
    fun `should get request by user`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val request = ConsultationRequest(
            id = "test-id-123",
            slackUserId = "U123ABC",
            slackUsername = "@john",
            question = "What is X?",
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )

        manager.addRequest(request)

        val retrieved = manager.getRequestByUser("U123ABC")

        assertNotNull(retrieved)
        assertEquals("test-id-123", retrieved.id)
    }

    @Test
    fun `should return null for non-existent request`() {
        val statePath = File(tempDir, "state.json").absolutePath
        val manager = StateManager(statePath)

        val retrieved = manager.getRequest("non-existent-id")

        assertNull(retrieved)
    }

    @Test
    fun `should persist state across manager instances`() {
        val statePath = File(tempDir, "state.json").absolutePath

        val manager1 = StateManager(statePath)
        val request = ConsultationRequest(
            id = "test-id-123",
            slackUserId = "U123ABC",
            slackUsername = "@john",
            question = "What is X?",
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )
        manager1.addRequest(request)

        // Create new manager instance
        val manager2 = StateManager(statePath)
        val retrieved = manager2.getRequest("test-id-123")

        assertNotNull(retrieved)
        assertEquals("test-id-123", retrieved.id)
    }
}
