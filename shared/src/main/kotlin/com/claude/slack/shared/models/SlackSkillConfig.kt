package com.claude.slack.shared.models

data class SlackSkillConfig(
    val slackBotToken: String,
    val slackAppToken: String,
    val timeoutHours: Int = 24,
    val userAliases: Map<String, String> = emptyMap(),
    val logLevel: String = "INFO"
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
    }
}
