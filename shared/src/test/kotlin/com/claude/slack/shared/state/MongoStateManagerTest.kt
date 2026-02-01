package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ServerHeartbeat
import com.claude.slack.shared.models.Status
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoStateManagerTest {

    companion object {
        @Container
        @JvmStatic
        val mongoContainer = MongoDBContainer("mongo:6.0")
    }

    private lateinit var stateManager: MongoStateManager

    @BeforeAll
    fun setupContainer() {
        mongoContainer.start()
    }

    @AfterAll
    fun teardownContainer() {
        mongoContainer.stop()
    }

    @BeforeEach
    fun setup() {
        // Create a fresh database for each test
        val testDbName = "test_${UUID.randomUUID().toString().take(8)}"
        stateManager = MongoStateManager(
            connectionString = mongoContainer.connectionString,
            databaseName = testDbName
        )
    }

    @Test
    fun `should add and retrieve consultation request`() {
        val request = createTestRequest("test-id-1")

        stateManager.addRequest(request)
        val retrieved = stateManager.getRequest("test-id-1")

        assertNotNull(retrieved)
        assertEquals("test-id-1", retrieved.id)
        assertEquals("U123ABC", retrieved.slackUserId)
        assertEquals("@john", retrieved.slackUsername)
        assertEquals("What is X?", retrieved.question)
        assertEquals(Status.PENDING, retrieved.status)
    }

    @Test
    fun `should return null for non-existent request`() {
        val retrieved = stateManager.getRequest("non-existent-id")
        assertNull(retrieved)
    }

    @Test
    fun `should update consultation request`() {
        val request = createTestRequest("test-id-2")
        stateManager.addRequest(request)

        stateManager.updateRequest("test-id-2") { it.withResponse("The answer is 42") }

        val updated = stateManager.getRequest("test-id-2")
        assertNotNull(updated)
        assertEquals(Status.ANSWERED, updated.status)
        assertEquals("The answer is 42", updated.response)
        assertNotNull(updated.respondedAt)
    }

    @Test
    fun `should get latest pending request`() {
        val request1 = createTestRequest(
            id = "test-id-3",
            createdAt = Instant.now().minus(2, ChronoUnit.HOURS)
        )
        val request2 = createTestRequest(
            id = "test-id-4",
            createdAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )

        stateManager.addRequest(request1)
        stateManager.addRequest(request2)

        val latest = stateManager.getLatestPendingRequest()
        assertNotNull(latest)
        assertEquals("test-id-4", latest.id)
    }

    @Test
    fun `should not return answered request as latest pending`() {
        val request1 = createTestRequest(
            id = "test-id-5",
            createdAt = Instant.now().minus(2, ChronoUnit.HOURS)
        )
        val request2 = createTestRequest(
            id = "test-id-6",
            createdAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )

        stateManager.addRequest(request1)
        stateManager.addRequest(request2)

        // Answer the newer request
        stateManager.updateRequest("test-id-6") { it.withResponse("Answered") }

        val latest = stateManager.getLatestPendingRequest()
        assertNotNull(latest)
        assertEquals("test-id-5", latest.id) // Should be the older pending one
    }

    @Test
    fun `should get request by user`() {
        val request = createTestRequest(
            id = "test-id-7",
            slackUserId = "U_SPECIFIC"
        )
        stateManager.addRequest(request)

        val retrieved = stateManager.getRequestByUser("U_SPECIFIC")
        assertNotNull(retrieved)
        assertEquals("test-id-7", retrieved.id)
    }

    @Test
    fun `should return null when no pending request for user`() {
        val request = createTestRequest(
            id = "test-id-8",
            slackUserId = "U_ANSWERED"
        )
        stateManager.addRequest(request)
        stateManager.updateRequest("test-id-8") { it.withResponse("Done") }

        val retrieved = stateManager.getRequestByUser("U_ANSWERED")
        assertNull(retrieved)
    }

    @Test
    fun `should get requests since timestamp`() {
        val oldRequest = createTestRequest(
            id = "old-request",
            createdAt = Instant.now().minus(10, ChronoUnit.DAYS)
        )
        val recentRequest = createTestRequest(
            id = "recent-request",
            createdAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )

        stateManager.addRequest(oldRequest)
        stateManager.addRequest(recentRequest)

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS)
        val recentRequests = stateManager.getRequestsSince(cutoff)

        assertEquals(1, recentRequests.size)
        assertEquals("recent-request", recentRequests[0].id)
    }

    @Test
    fun `should write and read heartbeat`() {
        val heartbeat = ServerHeartbeat(
            timestamp = Instant.now(),
            status = ServerHeartbeat.STATUS_HEALTHY
        )

        stateManager.writeHeartbeat(heartbeat)
        val retrieved = stateManager.getHeartbeat()

        assertNotNull(retrieved)
        assertEquals(ServerHeartbeat.STATUS_HEALTHY, retrieved.status)
    }

    @Test
    fun `should update heartbeat on subsequent writes`() {
        val heartbeat1 = ServerHeartbeat(
            timestamp = Instant.now().minus(1, ChronoUnit.MINUTES),
            status = ServerHeartbeat.STATUS_STARTING
        )
        val heartbeat2 = ServerHeartbeat(
            timestamp = Instant.now(),
            status = ServerHeartbeat.STATUS_HEALTHY
        )

        stateManager.writeHeartbeat(heartbeat1)
        stateManager.writeHeartbeat(heartbeat2)

        val retrieved = stateManager.getHeartbeat()
        assertNotNull(retrieved)
        assertEquals(ServerHeartbeat.STATUS_HEALTHY, retrieved.status)
    }

    @Test
    fun `should return null for non-existent heartbeat`() {
        val retrieved = stateManager.getHeartbeat()
        assertNull(retrieved)
    }

    @Test
    fun `should report connection status`() {
        assertTrue(stateManager.isConnected())
    }

    private fun createTestRequest(
        id: String = UUID.randomUUID().toString(),
        slackUserId: String = "U123ABC",
        createdAt: Instant = Instant.now()
    ): ConsultationRequest {
        return ConsultationRequest(
            id = id,
            slackUserId = slackUserId,
            slackUsername = "@john",
            question = "What is X?",
            createdAt = createdAt,
            expiresAt = createdAt.plus(24, ChronoUnit.HOURS)
        )
    }
}
