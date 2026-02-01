package com.claude.slack.shared.models

import kotlinx.serialization.Serializable as KSerializable
import java.time.Instant

@KSerializable
data class ServerHeartbeat(
    @KSerializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val status: String
) {
    fun isStale(staleThresholdSeconds: Int): Boolean {
        return Instant.now().epochSecond - timestamp.epochSecond > staleThresholdSeconds
    }

    companion object {
        const val STATUS_HEALTHY = "healthy"
        const val STATUS_STARTING = "starting"
        const val STATUS_STOPPING = "stopping"
    }
}
