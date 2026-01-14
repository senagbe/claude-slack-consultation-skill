plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Slack Bolt SDK (exposed to dependent modules)
    api("com.slack.api:bolt:1.37.0")
    api("com.slack.api:bolt-socket-mode:1.37.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // SnakeYAML for config parsing
    implementation("org.yaml:snakeyaml:2.2")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
