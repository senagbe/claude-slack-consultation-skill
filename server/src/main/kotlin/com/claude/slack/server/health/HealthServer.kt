package com.claude.slack.server.health

import com.claude.slack.shared.models.ServerHeartbeat
import com.claude.slack.shared.state.MongoStateManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class HealthServer(
    private val port: Int,
    private val stateManager: MongoStateManager,
    private val heartbeatStaleThresholdSeconds: Int
) {
    private val logger = LoggerFactory.getLogger(HealthServer::class.java)
    private var server: ApplicationEngine? = null

    @Serializable
    data class HealthResponse(
        val status: String,
        val timestamp: String,
        val details: Map<String, String> = emptyMap()
    )

    @Serializable
    data class HeartbeatResponse(
        val timestamp: String,
        val status: String,
        val isStale: Boolean
    )

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json()
            }

            routing {
                get("/health") {
                    val mongoConnected = withContext(Dispatchers.IO) {
                        stateManager.isConnected()
                    }
                    val heartbeat = withContext(Dispatchers.IO) {
                        stateManager.getHeartbeat()
                    }

                    val status = when {
                        !mongoConnected -> "unhealthy"
                        heartbeat == null -> "starting"
                        heartbeat.isStale(heartbeatStaleThresholdSeconds) -> "degraded"
                        else -> "healthy"
                    }

                    val statusCode = when (status) {
                        "healthy" -> HttpStatusCode.OK
                        "starting" -> HttpStatusCode.OK
                        "degraded" -> HttpStatusCode.OK
                        else -> HttpStatusCode.ServiceUnavailable
                    }

                    call.respond(
                        statusCode,
                        HealthResponse(
                            status = status,
                            timestamp = java.time.Instant.now().toString(),
                            details = mapOf(
                                "mongodb" to if (mongoConnected) "connected" else "disconnected",
                                "heartbeat" to (heartbeat?.status ?: "none")
                            )
                        )
                    )
                }

                get("/health/live") {
                    call.respond(
                        HttpStatusCode.OK,
                        HealthResponse(
                            status = "ok",
                            timestamp = java.time.Instant.now().toString()
                        )
                    )
                }

                get("/health/ready") {
                    val mongoConnected = withContext(Dispatchers.IO) {
                        stateManager.isConnected()
                    }

                    if (mongoConnected) {
                        call.respond(
                            HttpStatusCode.OK,
                            HealthResponse(
                                status = "ready",
                                timestamp = java.time.Instant.now().toString(),
                                details = mapOf("mongodb" to "connected")
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            HealthResponse(
                                status = "not_ready",
                                timestamp = java.time.Instant.now().toString(),
                                details = mapOf("mongodb" to "disconnected")
                            )
                        )
                    }
                }

                get("/health/heartbeat") {
                    val heartbeat = withContext(Dispatchers.IO) {
                        stateManager.getHeartbeat()
                    }

                    if (heartbeat != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            HeartbeatResponse(
                                timestamp = heartbeat.timestamp.toString(),
                                status = heartbeat.status,
                                isStale = heartbeat.isStale(heartbeatStaleThresholdSeconds)
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            HealthResponse(
                                status = "not_found",
                                timestamp = java.time.Instant.now().toString()
                            )
                        )
                    }
                }
            }
        }

        server?.start(wait = false)
        logger.info("Health server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        logger.info("Health server stopped")
    }
}
