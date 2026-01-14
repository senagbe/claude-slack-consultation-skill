package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ConsultationState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class StateManager(private val statePath: String = ".claude/state/slack-skill-pending.json") {

    private val logger = LoggerFactory.getLogger(StateManager::class.java)
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

    fun addRequest(request: ConsultationRequest) {
        val currentState = readState()
        val newState = currentState.addRequest(request)
        writeState(newState)
        logger.info("Added consultation request: ${request.id} for user ${request.slackUsername}")
    }

    fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest) {
        val currentState = readState()
        val newState = currentState.updateRequest(id, updater)
        writeState(newState)
        logger.info("Updated consultation request: $id")
    }

    fun getRequest(id: String): ConsultationRequest? {
        return readState().findRequest(id)
    }

    fun getLatestPendingRequest(): ConsultationRequest? {
        return readState().getLatestPending()
    }

    fun getRequestByUser(slackUserId: String): ConsultationRequest? {
        return readState().findRequestByUser(slackUserId)
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

class StateException(message: String, cause: Throwable? = null) : Exception(message, cause)
