package com.claude.slack.shared.slack

import com.claude.slack.shared.models.SlackSkillConfig
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.users.UsersListResponse
import com.slack.api.model.User
import org.slf4j.LoggerFactory

class SlackClientWrapper(private val config: SlackSkillConfig) {

    private val logger = LoggerFactory.getLogger(SlackClientWrapper::class.java)
    private val slack = Slack.getInstance()
    private val client: MethodsClient = slack.methods(config.slackBotToken)

    /**
     * Resolve a Slack user by username, email, or alias.
     * Supports fuzzy matching on display name and real name.
     */
    fun resolveUser(identifier: String): User? {
        // Check if it's an alias first
        val aliasResolution = config.userAliases[identifier]
        if (aliasResolution != null) {
            logger.debug("Resolved alias '$identifier' to '$aliasResolution'")
            return resolveUser(aliasResolution) // Recursive call with alias value
        }

        // If it starts with U (Slack user ID), look up directly
        if (identifier.startsWith("U") && identifier.length == 9) {
            return getUserById(identifier)
        }

        // List all users and search
        val users = listAllUsers()

        // Exact match on display name, real name, or email
        val exactMatch = users.firstOrNull {
            it.profile?.displayName?.equals(identifier, ignoreCase = true) == true ||
            it.profile?.realName?.equals(identifier, ignoreCase = true) == true ||
            it.profile?.email?.equals(identifier, ignoreCase = true) == true ||
            it.name?.equals(identifier.removePrefix("@"), ignoreCase = true) == true
        }

        if (exactMatch != null) {
            logger.info("Exact match found for '$identifier': ${exactMatch.id}")
            return exactMatch
        }

        // Fuzzy match on display name or real name
        val fuzzyMatch = users.firstOrNull {
            it.profile?.displayName?.contains(identifier, ignoreCase = true) == true ||
            it.profile?.realName?.contains(identifier, ignoreCase = true) == true
        }

        if (fuzzyMatch != null) {
            logger.info("Fuzzy match found for '$identifier': ${fuzzyMatch.id}")
            return fuzzyMatch
        }

        logger.warn("No user found for identifier: $identifier")
        return null
    }

    /**
     * Send a direct message to a Slack user.
     */
    fun sendDirectMessage(userId: String, message: String): ChatPostMessageResponse {
        logger.info("Sending DM to user $userId")

        val response = client.chatPostMessage { req ->
            req.channel(userId)
                .text(message)
                .mrkdwn(true)
        }

        if (!response.isOk) {
            logger.error("Failed to send DM: ${response.error}")
            throw SlackApiException("Failed to send DM to $userId: ${response.error}")
        }

        logger.info("DM sent successfully to $userId")
        return response
    }

    /**
     * Test the Slack API connection and token validity.
     */
    fun testConnection(): Boolean {
        return try {
            val response = client.authTest { req -> req }
            if (response.isOk) {
                logger.info("Slack connection test successful. Bot user: ${response.userId}")
                true
            } else {
                logger.error("Slack connection test failed: ${response.error}")
                false
            }
        } catch (e: Exception) {
            logger.error("Slack connection test failed: ${e.message}", e)
            false
        }
    }

    private fun listAllUsers(): List<User> {
        logger.debug("Fetching all users from Slack")

        val response = client.usersList { req ->
            req.limit(1000) // Max per page
        }

        if (!response.isOk) {
            logger.error("Failed to list users: ${response.error}")
            throw SlackApiException("Failed to list users: ${response.error}")
        }

        val users = response.members.filter {
            !it.isBot && !it.isDeleted && !it.isRestricted
        }

        logger.debug("Retrieved ${users.size} active users")
        return users
    }

    private fun getUserById(userId: String): User? {
        logger.debug("Fetching user by ID: $userId")

        val response = client.usersInfo { req ->
            req.user(userId)
        }

        if (!response.isOk) {
            logger.warn("Failed to get user by ID $userId: ${response.error}")
            return null
        }

        return response.user
    }
}

class SlackApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
