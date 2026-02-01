package com.claude.slack.server.health

import com.claude.slack.shared.models.ServerHeartbeat
import com.claude.slack.shared.state.StateManagerInterface
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant

class HeartbeatService(
    private val stateManager: StateManagerInterface,
    private val intervalSeconds: Int = 30
) {
    private val logger = LoggerFactory.getLogger(HeartbeatService::class.java)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        logger.info("Starting heartbeat service (interval: ${intervalSeconds}s)")

        // Write initial heartbeat with starting status
        writeHeartbeat(ServerHeartbeat.STATUS_STARTING)

        job = scope.launch {
            while (isActive) {
                try {
                    delay(intervalSeconds * 1000L)
                    writeHeartbeat(ServerHeartbeat.STATUS_HEALTHY)
                } catch (e: CancellationException) {
                    logger.debug("Heartbeat service cancelled")
                    throw e
                } catch (e: Exception) {
                    logger.error("Failed to write heartbeat: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        logger.info("Stopping heartbeat service")
        try {
            writeHeartbeat(ServerHeartbeat.STATUS_STOPPING)
        } catch (e: Exception) {
            logger.warn("Failed to write stopping heartbeat: ${e.message}")
        }
        job?.cancel()
        scope.cancel()
    }

    private fun writeHeartbeat(status: String) {
        val heartbeat = ServerHeartbeat(
            timestamp = Instant.now(),
            status = status
        )
        stateManager.writeHeartbeat(heartbeat)
        logger.debug("Heartbeat written: $status at ${heartbeat.timestamp}")
    }
}
