import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

group = "io.paoloconte"
version = "1.6.2"

repositories {
    mavenCentral()
}

dependencies {
    val notionSdkVersion: String by project
    val swaggerParserVersion: String by project
    val logbackVersion: String by project
    val kamlVersion: String by project
    val kotlinSerializationVersion: String by project
    val cliktVersion: String by project

    implementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
    implementation("com.github.seratch:notion-sdk-jvm-core:$notionSdkVersion")
    implementation("com.github.seratch:notion-sdk-jvm-slf4j:$notionSdkVersion")
    implementation("com.github.seratch:notion-sdk-jvm-httpclient:${notionSdkVersion}")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.charleskorn.kaml:kaml:$kamlVersion")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("app.jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "io.paoloconte.MainKt"))
        }
    }
}


tasks.test {
    useJUnitPlatform()
}
