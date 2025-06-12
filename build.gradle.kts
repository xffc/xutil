plugins {
    application

    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.shadow)
}

group = "fun.xffc"
version = "1.0.0"

application.mainClass = "$group.$name.MainKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    implementation(libs.telegram)

    implementation(libs.ktor.network)

    implementation(libs.kache)

    implementation(libs.adventure.api)
    implementation(libs.adventure.gson)
    implementation(libs.adventure.plain)
}

tasks.jar {
    destinationDirectory = file("$rootDir/build")
}

sourceSets.main {
    kotlin.srcDir("src")
}

kotlin {
    jvmToolchain(21)
}