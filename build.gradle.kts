plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0"
    `maven-publish`
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
    // Kotlin standard library and reflection
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)

    // Kotlin coroutines
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.jdk8)

    // Kotlin serialization
    api(libs.kotlinx.serialization.json)

    // Compile-only annotations
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

kotlin {
    jvmToolchain(javaVersion)
}

tasks.shadowJar {
    archiveClassifier.set("")

    // Relocate Kotlin to avoid conflicts with other plugins
    relocate("kotlin", "aster.amo.hykot.libs.kotlin")
    relocate("kotlinx", "aster.amo.hykot.libs.kotlinx")
    relocate("org.intellij", "aster.amo.hykot.libs.intellij")
    relocate("org.jetbrains.annotations", "aster.amo.hykot.libs.jetbrains.annotations")

    // Minimize JAR by removing unused classes
    minimize {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
    }

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),
        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),
        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
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
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] =
            providers.environmentVariable("COMMIT_SHA_SHORT")
                .map { "${version}-${it}" }
                .getOrElse(version.toString())
    }
}

publishing {
    repositories {
        // Configure publishing repositories here
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks.shadowJar)
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
