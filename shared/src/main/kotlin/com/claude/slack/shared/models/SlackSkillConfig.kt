package com.claude.slack.shared.models

data class SlackSkillConfig(
    val slackBotToken: String,
    val slackAppToken: String,
    val timeoutHours: Int = 24,
    val userAliases: Map<String, String> = emptyMap(),
    val logLevel: String = "INFO",
    val mongoConnectionString: String = "mongodb://localhost:27017",
    val mongoDatabase: String = "slack_skill",
    val healthPort: Int = 8080,
    val heartbeatIntervalSeconds: Int = 30,
    val heartbeatStaleThresholdSeconds: Int = 90
) {
    fun validate() {
        require(slackBotToken.startsWith("xoxb-")) {
            "Invalid slack_bot_token: must start with 'xoxb-'"
        }
        require(slackAppToken.startsWith("xapp-")) {
            "Invalid slack_app_token: must start with 'xapp-'"
        }
        require(timeoutHours > 0) {
            "timeout_hours must be greater than 0"
        }
        require(logLevel in listOf("DEBUG", "INFO", "WARN", "ERROR")) {
            "log_level must be one of: DEBUG, INFO, WARN, ERROR"
        }
        require(healthPort in 1..65535) {
            "health_port must be between 1 and 65535"
        }
        require(heartbeatIntervalSeconds > 0) {
            "heartbeat_interval_seconds must be greater than 0"
        }
        require(heartbeatStaleThresholdSeconds > heartbeatIntervalSeconds) {
            "heartbeat_stale_threshold_seconds must be greater than heartbeat_interval_seconds"
        }
    }
}
