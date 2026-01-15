plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("hytale-mod") version "0.+"
}

group = "aster.amo"
version = "1.0.0"
val javaVersion = 24

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
}

dependencies {
    // Depend on Kytale - provides Kotlin runtime
    implementation(project(":"))
    compileOnly(project(":hexweave"))

    // Compile-only annotations
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

kotlin {
    jvmToolchain(javaVersion)
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to "AmoAster",
        "plugin_maven_group" to project.group,
        "plugin_name" to "KytaleExample",
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),
        "plugin_description" to "Example plugin demonstrating Kytale features",
        "plugin_website" to "",
        "plugin_main_entrypoint" to "aster.amo.example.ExamplePlugin",
        "plugin_author" to "Kytale"
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }

    inputs.properties(replaceProperties)
}

hytale {

}

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = "KytaleExample"
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = version.toString()
    }
}
