package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ServerHeartbeat
import java.time.Instant

interface StateManagerInterface {
    fun addRequest(request: ConsultationRequest)
    fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest)
    fun getRequest(id: String): ConsultationRequest?
    fun getLatestPendingRequest(): ConsultationRequest?
    fun getRequestByUser(slackUserId: String): ConsultationRequest?
    fun getRequestsSince(since: Instant): List<ConsultationRequest>
    fun writeHeartbeat(heartbeat: ServerHeartbeat)
    fun getHeartbeat(): ServerHeartbeat?
}
