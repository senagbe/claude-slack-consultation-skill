package com.claude.slack.server.listener

import com.claude.slack.shared.models.SlackSkillConfig
import com.claude.slack.server.handler.ResponseHandler
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.MessageEvent
import org.slf4j.LoggerFactory

class SlackEventListener(
    private val config: SlackSkillConfig,
    private val responseHandler: ResponseHandler
) {
    private val logger = LoggerFactory.getLogger(SlackEventListener::class.java)
    private var socketModeApp: SocketModeApp? = null

    fun start() {
        logger.info("Starting Slack event listener with Socket Mode")

        val appConfig = AppConfig.builder()
            .singleTeamBotToken(config.slackBotToken)
            .build()

        val app = App(appConfig)

        // Subscribe to direct message events
        app.event(MessageEvent::class.java) { payload, ctx ->
            try {
                val event = payload.event

                // Filter: Only process messages sent TO the bot (not FROM the bot)
                if (event.botId != null) {
                    logger.debug("Ignoring message from bot: ${event.botId}")
                    ctx.ack()
                    return@event ctx.ack()
                }

                // Filter: Only process DM messages (channel type = "im")
                if (event.channelType != "im") {
                    logger.debug("Ignoring non-DM message in channel: ${event.channel}")
                    ctx.ack()
                    return@event ctx.ack()
                }

                logger.info("Received DM from user ${event.user}: ${event.text}")

                // Handle the response
                responseHandler.handleUserResponse(event.user, event.text)

                ctx.ack()
            } catch (e: Exception) {
                logger.error("Error processing message event: ${e.message}", e)
                ctx.ack()
            }
        }

        // Start Socket Mode connection
        socketModeApp = SocketModeApp(config.slackAppToken, app)
        socketModeApp?.startAsync()

        logger.info("Slack event listener started successfully")
    }

    fun stop() {
        logger.info("Stopping Slack event listener")
        socketModeApp?.stop()
        logger.info("Slack event listener stopped")
    }

    fun isRunning(): Boolean {
        return socketModeApp != null
    }
}
