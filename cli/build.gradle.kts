plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared"))
}

application {
    mainClass.set("com.claude.slack.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
