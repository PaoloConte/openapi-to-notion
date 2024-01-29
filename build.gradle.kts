import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "io.paoloconte"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val notionSdkVersion: String by project
    val swaggerParserVersion: String by project
    val logbackVersion: String by project

    implementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
    implementation("com.github.seratch:notion-sdk-jvm-core:$notionSdkVersion")
    implementation("com.github.seratch:notion-sdk-jvm-slf4j:$notionSdkVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
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

kotlin {
    jvmToolchain(11)
}