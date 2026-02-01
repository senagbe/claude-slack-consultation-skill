package com.claude.slack.shared.health

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ConnectException
import java.time.Instant
import java.util.concurrent.TimeUnit

class HealthClient(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val logger = LoggerFactory.getLogger(HealthClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    sealed class HealthCheckResult {
        data class Healthy(val status: String, val details: Map<String, String>) : HealthCheckResult()
        data class Unhealthy(val status: String, val details: Map<String, String>) : HealthCheckResult()
        data object ServerNotRunning : HealthCheckResult()
        data class Error(val message: String) : HealthCheckResult()
    }

    sealed class HeartbeatResult {
        data class Success(
            val timestamp: Instant,
            val status: String,
            val isStale: Boolean
        ) : HeartbeatResult()
        data object NotFound : HeartbeatResult()
        data class Error(val message: String) : HeartbeatResult()
    }

    @Serializable
    private data class HealthResponse(
        val status: String,
        val timestamp: String,
        val details: Map<String, String> = emptyMap()
    )

    @Serializable
    private data class HeartbeatResponse(
        val timestamp: String,
        val status: String,
        val isStale: Boolean
    )

    fun checkHealth(): HealthCheckResult {
        val request = Request.Builder()
            .url("$baseUrl/health")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return HealthCheckResult.Error("Empty response body")
                val healthResponse = json.decodeFromString<HealthResponse>(body)

                when (healthResponse.status) {
                    "healthy", "starting" -> HealthCheckResult.Healthy(
                        status = healthResponse.status,
                        details = healthResponse.details
                    )
                    else -> HealthCheckResult.Unhealthy(
                        status = healthResponse.status,
                        details = healthResponse.details
                    )
                }
            }
        } catch (e: ConnectException) {
            logger.debug("Server not running: ${e.message}")
            HealthCheckResult.ServerNotRunning
        } catch (e: IOException) {
            logger.debug("Connection error: ${e.message}")
            HealthCheckResult.ServerNotRunning
        } catch (e: Exception) {
            logger.error("Health check failed: ${e.message}", e)
            HealthCheckResult.Error(e.message ?: "Unknown error")
        }
    }

    fun getHeartbeat(): HeartbeatResult {
        val request = Request.Builder()
            .url("$baseUrl/health/heartbeat")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: return HeartbeatResult.Error("Empty response body")
                        val heartbeatResponse = json.decodeFromString<HeartbeatResponse>(body)
                        HeartbeatResult.Success(
                            timestamp = Instant.parse(heartbeatResponse.timestamp),
                            status = heartbeatResponse.status,
                            isStale = heartbeatResponse.isStale
                        )
                    }
                    404 -> HeartbeatResult.NotFound
                    else -> HeartbeatResult.Error("Unexpected status code: ${response.code}")
                }
            }
        } catch (e: ConnectException) {
            logger.debug("Server not running: ${e.message}")
            HeartbeatResult.Error("Server not running")
        } catch (e: IOException) {
            logger.debug("Connection error: ${e.message}")
            HeartbeatResult.Error("Connection error: ${e.message}")
        } catch (e: Exception) {
            logger.error("Heartbeat check failed: ${e.message}", e)
            HeartbeatResult.Error(e.message ?: "Unknown error")
        }
    }

    fun isServerReady(): Boolean {
        return when (val result = checkHealth()) {
            is HealthCheckResult.Healthy -> result.status == "healthy"
            else -> false
        }
    }
}
