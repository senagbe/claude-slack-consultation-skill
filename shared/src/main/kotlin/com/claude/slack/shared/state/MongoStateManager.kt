package com.claude.slack.shared.state

import com.claude.slack.shared.models.ConsultationRequest
import com.claude.slack.shared.models.ServerHeartbeat
import com.claude.slack.shared.models.Status
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.slf4j.LoggerFactory
import java.time.Instant

class MongoStateManager(
    connectionString: String = "mongodb://localhost:27017",
    databaseName: String = "slack_skill"
) : StateManagerInterface, AutoCloseable {

    private val logger = LoggerFactory.getLogger(MongoStateManager::class.java)
    private val client: MongoClient = MongoClients.create(connectionString)
    private val database: MongoDatabase = client.getDatabase(databaseName)
    private val requestsCollection: MongoCollection<Document> = database.getCollection("consultation_requests")
    private val heartbeatCollection: MongoCollection<Document> = database.getCollection("heartbeat")

    init {
        createIndexes()
    }

    private fun createIndexes() {
        try {
            // Unique index on id
            requestsCollection.createIndex(
                Indexes.ascending("id"),
                IndexOptions().unique(true)
            )

            // Compound index on slackUserId + status for getRequestByUser queries
            requestsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("slackUserId"),
                    Indexes.ascending("status")
                )
            )

            // Descending index on createdAt for getLatestPendingRequest queries
            requestsCollection.createIndex(
                Indexes.descending("createdAt")
            )

            logger.info("MongoDB indexes created successfully")
        } catch (e: Exception) {
            logger.warn("Index creation failed (may already exist): ${e.message}")
        }
    }

    override fun addRequest(request: ConsultationRequest) {
        val document = requestToDocument(request)
        requestsCollection.insertOne(document)
        logger.info("Added consultation request: ${request.id} for user ${request.slackUsername}")
    }

    override fun updateRequest(id: String, updater: (ConsultationRequest) -> ConsultationRequest) {
        val existingRequest = getRequest(id)
        if (existingRequest != null) {
            val updatedRequest = updater(existingRequest)
            val document = requestToDocument(updatedRequest)
            requestsCollection.replaceOne(
                Filters.eq("id", id),
                document
            )
            logger.info("Updated consultation request: $id")
        } else {
            logger.warn("Cannot update non-existent request: $id")
        }
    }

    override fun getRequest(id: String): ConsultationRequest? {
        val document = requestsCollection.find(Filters.eq("id", id)).first()
        return document?.let { documentToRequest(it) }
    }

    override fun getLatestPendingRequest(): ConsultationRequest? {
        val document = requestsCollection
            .find(Filters.eq("status", Status.PENDING.name))
            .sort(Sorts.descending("createdAt"))
            .first()
        return document?.let { documentToRequest(it) }
    }

    override fun getRequestByUser(slackUserId: String): ConsultationRequest? {
        val document = requestsCollection
            .find(
                Filters.and(
                    Filters.eq("slackUserId", slackUserId),
                    Filters.eq("status", Status.PENDING.name)
                )
            )
            .sort(Sorts.descending("createdAt"))
            .first()
        return document?.let { documentToRequest(it) }
    }

    override fun getRequestsSince(since: Instant): List<ConsultationRequest> {
        return requestsCollection
            .find(Filters.gte("createdAt", since.toString()))
            .sort(Sorts.descending("createdAt"))
            .map { documentToRequest(it) }
            .toList()
    }

    override fun writeHeartbeat(heartbeat: ServerHeartbeat) {
        val document = Document()
            .append("_id", "server_heartbeat")
            .append("timestamp", heartbeat.timestamp.toString())
            .append("status", heartbeat.status)

        heartbeatCollection.replaceOne(
            Filters.eq("_id", "server_heartbeat"),
            document,
            ReplaceOptions().upsert(true)
        )
        logger.debug("Heartbeat written: ${heartbeat.timestamp}")
    }

    override fun getHeartbeat(): ServerHeartbeat? {
        val document = heartbeatCollection.find(Filters.eq("_id", "server_heartbeat")).first()
        return document?.let {
            ServerHeartbeat(
                timestamp = Instant.parse(it.getString("timestamp")),
                status = it.getString("status")
            )
        }
    }

    fun isConnected(): Boolean {
        return try {
            database.runCommand(Document("ping", 1))
            true
        } catch (e: Exception) {
            logger.error("MongoDB connection check failed: ${e.message}")
            false
        }
    }

    override fun close() {
        client.close()
        logger.info("MongoDB connection closed")
    }

    private fun requestToDocument(request: ConsultationRequest): Document {
        return Document()
            .append("id", request.id)
            .append("slackUserId", request.slackUserId)
            .append("slackUsername", request.slackUsername)
            .append("question", request.question)
            .append("createdAt", request.createdAt.toString())
            .append("expiresAt", request.expiresAt.toString())
            .append("status", request.status.name)
            .append("response", request.response)
            .append("respondedAt", request.respondedAt?.toString())
    }

    private fun documentToRequest(document: Document): ConsultationRequest {
        return ConsultationRequest(
            id = document.getString("id"),
            slackUserId = document.getString("slackUserId"),
            slackUsername = document.getString("slackUsername"),
            question = document.getString("question"),
            createdAt = Instant.parse(document.getString("createdAt")),
            expiresAt = Instant.parse(document.getString("expiresAt")),
            status = Status.valueOf(document.getString("status")),
            response = document.getString("response"),
            respondedAt = document.getString("respondedAt")?.let { Instant.parse(it) }
        )
    }
}
