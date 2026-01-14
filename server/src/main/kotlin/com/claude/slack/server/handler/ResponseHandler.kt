package com.claude.slack.server.handler

import com.claude.slack.shared.state.StateManager
import org.slf4j.LoggerFactory

class ResponseHandler(private val stateManager: StateManager) {

    private val logger = LoggerFactory.getLogger(ResponseHandler::class.java)

    /**
     * Handle an incoming response from a Slack user.
     * Match it to a pending consultation request and update state.
     */
    fun handleUserResponse(slackUserId: String, responseText: String) {
        logger.info("Processing response from user: $slackUserId")

        try {
            // Find the most recent pending request for this user
            val pendingRequest = stateManager.getRequestByUser(slackUserId)

            if (pendingRequest == null) {
                logger.warn("No pending consultation request found for user: $slackUserId")
                logger.debug("This might be an unsolicited message. Ignoring.")
                return
            }

            logger.info("Matched response to consultation request: ${pendingRequest.id}")

            // Update the request with the response
            stateManager.updateRequest(pendingRequest.id) { request ->
                request.withResponse(responseText)
            }

            logger.info("Successfully captured response for request: ${pendingRequest.id}")

        } catch (e: Exception) {
            logger.error("Failed to handle user response from $slackUserId: ${e.message}", e)
        }
    }
}
