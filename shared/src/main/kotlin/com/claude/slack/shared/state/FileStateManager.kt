package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ConsultationState
import com.claude.slack.shared.models.ServerHeartbeat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class FileStateManager(
    private val statePath: String = ".claude/state/slack-skill-pending.json",
    private val heartbeatPath: String = ".claude/state/slack-skill-heartbeat.json"
) : StateManagerInterface {

    private val logger = LoggerFactory.getLogger(FileStateManager::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val rwLock = ReentrantReadWriteLock()

    fun readState(): ConsultationState {
        return rwLock.readLock().withLock {
            val stateFile = File(statePath)

            if (!stateFile.exists()) {
                logger.debug("State file not found, returning empty state")
                ConsultationState()
            } else {
                try {
                    withFileLock(stateFile, shared = true) { _ ->
                        val content = stateFile.readText()
                        if (content.isBlank()) {
                            ConsultationState()
                        } else {
                            json.decodeFromString<ConsultationState>(content)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to read state file: ${e.message}", e)
                    throw StateException("Failed to read state file: ${e.message}", e)
                }
            }
        }
    }

    fun writeState(state: ConsultationState) {
        rwLock.writeLock().withLock {
            try {
                val stateFile = File(statePath)
                stateFile.parentFile?.mkdirs()

                // Write to temp file first
                val tempFile = File("${statePath}.tmp")
                tempFile.writeText(json.encodeToString(state))

                // Atomic rename
                if (!tempFile.renameTo(stateFile)) {
                    // Fallback for cross-filesystem moves
                    tempFile.copyTo(stateFile, overwrite = true)
                    tempFile.delete()
                }

                logger.debug("State written successfully")
            } catch (e: Exception) {
                logger.error("Failed to write state file: ${e.message}", e)
                throw StateException("Failed to write state file: ${e.message}", e)
            }
        }
    }

    override fun addRequest(request: ConsultationRequest) {
        val currentState = readState()
        val newState = currentState.addRequest(request)
        writeState(newState)
        logger.info("Added consultation request: ${request.id} for user ${request.slackUsername}")
    }

    override fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest) {
        val currentState = readState()
        val newState = currentState.updateRequest(id, updater)
        writeState(newState)
        logger.info("Updated consultation request: $id")
    }

    override fun getRequest(id: String): ConsultationRequest? {
        return readState().findRequest(id)
    }

    override fun getLatestPendingRequest(): ConsultationRequest? {
        return readState().getLatestPending()
    }

    override fun getRequestByUser(slackUserId: String): ConsultationRequest? {
        return readState().findRequestByUser(slackUserId)
    }

    override fun getRequestsSince(since: java.time.Instant): List<ConsultationRequest> {
        return readState().requests
            .filter { it.createdAt.isAfter(since) }
            .sortedByDescending { it.createdAt }
    }

    override fun writeHeartbeat(heartbeat: ServerHeartbeat) {
        rwLock.writeLock().withLock {
            try {
                val heartbeatFile = File(heartbeatPath)
                heartbeatFile.parentFile?.mkdirs()
                heartbeatFile.writeText(json.encodeToString(heartbeat))
                logger.debug("Heartbeat written: ${heartbeat.timestamp}")
            } catch (e: Exception) {
                logger.error("Failed to write heartbeat: ${e.message}", e)
                throw StateException("Failed to write heartbeat: ${e.message}", e)
            }
        }
    }

    override fun getHeartbeat(): ServerHeartbeat? {
        return rwLock.readLock().withLock {
            val heartbeatFile = File(heartbeatPath)
            if (!heartbeatFile.exists()) {
                null
            } else {
                try {
                    val content = heartbeatFile.readText()
                    if (content.isBlank()) null else json.decodeFromString<ServerHeartbeat>(content)
                } catch (e: Exception) {
                    logger.error("Failed to read heartbeat: ${e.message}", e)
                    null
                }
            }
        }
    }

    private fun <T> withFileLock(file: File, shared: Boolean = false, block: (FileLock) -> T): T {
        return FileChannel.open(
            file.toPath(),
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE
        ).use { channel ->
            channel.lock(0L, Long.MAX_VALUE, shared).use { lock ->
                block(lock)
            }
        }
    }
}

@Deprecated("Use FileStateManager instead", ReplaceWith("FileStateManager(statePath)"))
typealias StateManager = FileStateManager

class StateException(message: String, cause: Throwable? = null) : Exception(message, cause)
