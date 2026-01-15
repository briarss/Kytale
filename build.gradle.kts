plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0"
    `maven-publish`
    id("hytale-mod") version "0.+"
}

group = "aster.amo"
version = "1.1.0"
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
    relocate("kotlin", "aster.amo.kytale.libs.kotlin")
    relocate("kotlinx", "aster.amo.kytale.libs.kotlinx")
    relocate("org.intellij", "aster.amo.kytale.libs.intellij")
    relocate("org.jetbrains.annotations", "aster.amo.kytale.libs.jetbrains.annotations")

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
    publications {
        create<MavenPublication>("maven") {
            groupId = "aster.amo"
            artifactId = "kytale"
            version = project.version.toString()

            artifact(tasks.shadowJar)
            artifact(tasks.named("sourcesJar"))

            pom {
                name.set("Kytale")
                description.set("Kotlin framework for Hytale server plugin development")
                url.set("https://github.com/AmoAster/Kytale")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "PokeSkies"
            url = uri("https://maven.pokeskies.com/releases")
            credentials {
                username = project.findProperty("pokeskiesUsername") as String? ?: System.getenv("POKESKIES_USERNAME")
                password = project.findProperty("pokeskiesPassword") as String? ?: System.getenv("POKESKIES_PASSWORD")
            }
            authentication {
                create("basic", BasicAuthentication::class.java)
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

// Task to compile UI definitions to .ui files
tasks.register<JavaExec>("compileUi") {
    group = "build"
    description = "Compile Kytale UI DSL definitions to .ui files"

    dependsOn("compileKotlin")

    val mainSourceSet = sourceSets.main.get()
    classpath = mainSourceSet.compileClasspath.plus(mainSourceSet.output)

    mainClass.set("aster.amo.kytale.ui.dsl.UiScanner")

    val outputDir = file("src/main/resources/Common/UI/Custom/Pages")
    args(outputDir.absolutePath, "aster.amo.kytale.ui.test")

    doFirst {
        println("Compiling UI definitions to: ${outputDir.absolutePath}")
    }
}
