package com.claude.slack.server

import com.claude.slack.shared.config.ConfigManager
import com.claude.slack.shared.state.MongoStateManager
import com.claude.slack.server.handler.ResponseHandler
import com.claude.slack.server.health.HeartbeatService
import com.claude.slack.server.health.HealthServer
import com.claude.slack.server.listener.SlackEventListener
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val server = SlackConsultationServer()

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    try {
        server.start()

        // Keep the server running
        Thread.currentThread().join()
    } catch (e: InterruptedException) {
        server.logger.info("Server interrupted, shutting down...")
        server.stop()
    } catch (e: Exception) {
        server.logger.error("Fatal error in server: ${e.message}", e)
        server.stop()
        exitProcess(1)
    }
}

class SlackConsultationServer {

    val logger = LoggerFactory.getLogger(SlackConsultationServer::class.java)

    private var eventListener: SlackEventListener? = null
    private var stateManager: MongoStateManager? = null
    private var heartbeatService: HeartbeatService? = null
    private var healthServer: HealthServer? = null
    private var isRunning = false

    fun start() {
        logger.info("=" * 50)
        logger.info("Starting Slack Consultation Skill Server")
        logger.info("=" * 50)

        try {
            // Load configuration
            logger.info("Loading configuration...")
            val configManager = ConfigManager()
            val config = configManager.loadConfig()
            logger.info("Configuration loaded successfully")

            // Initialize MongoDB state manager
            logger.info("Initializing MongoDB state manager...")
            stateManager = MongoStateManager(
                connectionString = config.mongoConnectionString,
                databaseName = config.mongoDatabase
            )
            logger.info("MongoDB state manager initialized")

            // Initialize heartbeat service
            logger.info("Initializing heartbeat service...")
            heartbeatService = HeartbeatService(stateManager!!, config.heartbeatIntervalSeconds)
            heartbeatService?.start()
            logger.info("Heartbeat service started")

            // Initialize health server
            logger.info("Initializing health server...")
            healthServer = HealthServer(
                port = config.healthPort,
                stateManager = stateManager!!,
                heartbeatStaleThresholdSeconds = config.heartbeatStaleThresholdSeconds
            )
            healthServer?.start()
            logger.info("Health server started on port ${config.healthPort}")

            // Initialize response handler
            logger.info("Initializing response handler...")
            val responseHandler = ResponseHandler(stateManager!!)
            logger.info("Response handler initialized")

            // Initialize and start event listener
            logger.info("Initializing Slack event listener...")
            eventListener = SlackEventListener(config, responseHandler)
            eventListener?.start()
            logger.info("Slack event listener started")

            isRunning = true

            logger.info("=" * 50)
            logger.info("Server started successfully")
            logger.info("Health endpoint: http://localhost:${config.healthPort}/health")
            logger.info("Listening for Slack consultation responses...")
            logger.info("=" * 50)

        } catch (e: Exception) {
            logger.error("Failed to start server: ${e.message}", e)
            throw e
        }
    }

    fun stop() {
        if (!isRunning) {
            return
        }

        logger.info("Stopping server...")

        try {
            eventListener?.stop()
            logger.info("Event listener stopped")

            heartbeatService?.stop()
            logger.info("Heartbeat service stopped")

            healthServer?.stop()
            logger.info("Health server stopped")

            stateManager?.close()
            logger.info("State manager closed")

            isRunning = false

            logger.info("Server stopped successfully")
        } catch (e: Exception) {
            logger.error("Error during server shutdown: ${e.message}", e)
        }
    }

    fun isRunning(): Boolean = isRunning

    private operator fun String.times(count: Int): String = this.repeat(count)
}
