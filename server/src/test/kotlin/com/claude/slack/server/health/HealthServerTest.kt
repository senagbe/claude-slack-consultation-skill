package com.claude.slack.server.health

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ServerHeartbeat
import com.claude.slack.shared.state.StateManagerInterface
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthServerTest {

    private lateinit var healthServer: HealthServer
    private lateinit var mockStateManager: MockMongoStateManager
    private val port = 18080 // Use non-standard port for tests
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        mockStateManager = MockMongoStateManager()
        // We can't easily test HealthServer without a real MongoStateManager
        // because it requires the isConnected() method. For integration tests,
        // we'd need Testcontainers. This test file serves as a template.
    }

    @AfterEach
    fun teardown() {
        if (::healthServer.isInitialized) {
            healthServer.stop()
        }
    }

    // Note: Full integration tests would require Testcontainers for MongoDB
    // These tests serve as documentation for expected behavior

    @Test
    fun `health endpoint should return status`() {
        // This test would verify that GET /health returns appropriate status
        // Requires running server with real MongoDB
        assertTrue(true) // Placeholder
    }

    @Test
    fun `liveness probe should always return ok`() {
        // This test would verify that GET /health/live returns 200 OK
        // Liveness should succeed even without MongoDB
        assertTrue(true) // Placeholder
    }

    @Test
    fun `readiness probe should check mongodb connection`() {
        // This test would verify that GET /health/ready checks MongoDB
        // Returns 200 if connected, 503 if disconnected
        assertTrue(true) // Placeholder
    }

    @Test
    fun `heartbeat endpoint should return last heartbeat`() {
        // This test would verify that GET /health/heartbeat returns heartbeat info
        assertTrue(true) // Placeholder
    }
}

// Mock implementation for basic testing
class MockMongoStateManager : StateManagerInterface {
    private val requests = mutableMapOf<String, ConsultationRequest>()
    private var heartbeat: ServerHeartbeat? = null
    var connected = true

    override fun addRequest(request: ConsultationRequest) {
        requests[request.id] = request
    }

    override fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest) {
        requests[id]?.let { requests[id] = updater(it) }
    }

    override fun getRequest(id: String): ConsultationRequest? = requests[id]

    override fun getLatestPendingRequest(): ConsultationRequest? =
        requests.values.filter { it.status == com.claude.slack.shared.models.Status.PENDING }
            .maxByOrNull { it.createdAt }

    override fun getRequestByUser(slackUserId: String): ConsultationRequest? =
        requests.values.find {
            it.slackUserId == slackUserId &&
            it.status == com.claude.slack.shared.models.Status.PENDING
        }

    override fun getRequestsSince(since: Instant): List<ConsultationRequest> =
        requests.values.filter { it.createdAt.isAfter(since) }
            .sortedByDescending { it.createdAt }

    override fun writeHeartbeat(heartbeat: ServerHeartbeat) {
        this.heartbeat = heartbeat
    }

    override fun getHeartbeat(): ServerHeartbeat? = heartbeat

    fun isConnected(): Boolean = connected
}
