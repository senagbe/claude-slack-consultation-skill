package com.claude.slack.shared.models

import kotlinx.serialization.Serializable as KSerializable
import java.time.Instant
import java.util.UUID

@KSerializable
data class ConsultationRequest(
    val id: String = UUID.randomUUID().toString(),
    val slackUserId: String,
    val slackUsername: String,
    val question: String,
    @KSerializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @KSerializable(with = InstantSerializer::class)
    val expiresAt: Instant,
    val status: Status = Status.PENDING,
    val response: String? = null,
    @KSerializable(with = InstantSerializer::class)
    val respondedAt: Instant? = null
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun withResponse(responseText: String): ConsultationRequest {
        return copy(
            status = Status.ANSWERED,
            response = responseText,
            respondedAt = Instant.now()
        )
    }

    fun markExpired(): ConsultationRequest {
        return copy(status = Status.EXPIRED)
    }
}

@KSerializable
enum class Status {
    PENDING,
    ANSWERED,
    EXPIRED
}
