plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

allprojects {
    group = "com.claude.slack"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        val implementation by configurations
        val testImplementation by configurations

        // Kotlin stdlib
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

        // Logging
        implementation("org.slf4j:slf4j-api:2.0.9")
        implementation("ch.qos.logback:logback-classic:1.4.14")

        // Testing
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
        testImplementation("io.mockk:mockk:1.13.8")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
