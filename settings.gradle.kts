
rootProject.name = "openapi-notion"

pluginManagement {

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    resolutionStrategy {
        val kotlinVersion: String by settings
        val shadowVersion: String by settings

        plugins {
            kotlin("jvm") version kotlinVersion
            kotlin("plugin.serialization") version kotlinVersion
            id("com.github.johnrengelman.shadow") version shadowVersion
            id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
        }
    }
}
