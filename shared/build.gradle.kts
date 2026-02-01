plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Slack Bolt SDK (exposed to dependent modules)
    api("com.slack.api:bolt:1.37.0")
    api("com.slack.api:bolt-socket-mode:1.37.0")

    // WebSocket support for Socket Mode
    implementation("javax.websocket:javax.websocket-api:1.1")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.17")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // SnakeYAML for config parsing
    implementation("org.yaml:snakeyaml:2.2")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // MongoDB driver
    implementation("org.mongodb:mongodb-driver-kotlin-sync:4.11.1")

    // OkHttp for health client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testcontainers for MongoDB integration tests
    testImplementation("org.testcontainers:mongodb:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}
