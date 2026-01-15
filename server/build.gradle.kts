plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared"))
}

application {
    mainClass.set("com.claude.slack.server.ServerKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
