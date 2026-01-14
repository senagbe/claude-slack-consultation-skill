package com.claude.slack.shared.models

import kotlinx.serialization.Serializable as KSerializable

@KSerializable
data class ConsultationState(
    val requests: List<ConsultationRequest> = emptyList()
) {
    fun addRequest(request: ConsultationRequest): ConsultationState {
        return copy(requests = requests + request)
    }

    fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest): ConsultationState {
        val updatedRequests = requests.map { if (it.id == id) updater(it) else it }
        return copy(requests = updatedRequests)
    }

    fun findRequest(id: String): ConsultationRequest? {
        return requests.find { it.id == id }
    }

    fun findRequestByUser(slackUserId: String): ConsultationRequest? {
        return requests.filter { it.slackUserId == slackUserId && it.status == Status.PENDING }
            .maxByOrNull { it.createdAt }
    }

    fun getLatestPending(): ConsultationRequest? {
        return requests.filter { it.status == Status.PENDING }
            .maxByOrNull { it.createdAt }
    }
}
