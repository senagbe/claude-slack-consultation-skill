plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

val ktorVersion = "2.3.7"

dependencies {
    implementation(project(":shared"))

    // Ktor server for health endpoints
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}

application {
    mainClass.set("com.claude.slack.server.ServerKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
