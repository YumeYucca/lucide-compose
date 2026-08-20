@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.gradle.api.publish.maven.MavenPublication
import LucideMetadataGenerator.generate as generateMetadata


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.valkyrie)
    id("maven-publish")
}

group = "io.github.YumeYucca"
version = "1.0.0-SNAPSHOT"

kotlin {
    android {
        namespace = "io.github.yumeyucca.lucide"
        compileSdk = 36
        minSdk = 21
    }
    jvm("desktop")
    js(IR) {
        browser()
        nodejs()
    }
    applyDefaultHierarchyTemplate()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
//    linuxX64()

    wasmJs {
        browser()
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(compose.runtime)
                compileOnly(compose.foundation)
                compileOnly(compose.ui)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.ui)
            }
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

val lucidePackage = "io.github.yumeyucca.lucide"
val valkyrieCommonMainOutput = layout.buildDirectory.dir("generated/sources/valkyrie/commonMain/kotlin")

valkyrie {
    packageName = lucidePackage
    // The Lucide checkout is the source of truth and remains outside src/commonMain.
    resourceDirectoryName = "../../lucide/icons"
    outputDirectory = layout.buildDirectory.dir("generated/sources/valkyrie")

    imageVector {
        useComposeColors = true
    }

    iconPack {
        name = "Lucide"
        targetSourceSet = "commonMain"
    }
}

tasks.matching {
    it.name.startsWith("generateValkyrieImageVector") &&
        it.name != "generateValkyrieImageVector" &&
        it.name != "generateValkyrieImageVectorCommonMain"
}.configureEach {
    enabled = false
}

val generateLucideMetadata = tasks.register("generateLucideMetadata") {
    group = "generation"
    description = "Generate Lucide metadata and the all-icons accessor"

    val assetsDir = file("lucide/icons")

    inputs.dir(assetsDir)
    outputs.file(
        valkyrieCommonMainOutput.map {
            it.file("${lucidePackage.replace('.', '/')}/LucideMetadata.generated.kt")
        },
    )
    dependsOn("generateValkyrieImageVectorCommonMain")

    doLast {
        generateMetadata(
            assetsDir = assetsDir,
            srcDir = valkyrieCommonMainOutput.get().asFile,
            basePackage = lucidePackage,
        )
    }
}

tasks.matching { it.name.contains("compile", ignoreCase = true) }.configureEach {
    dependsOn(generateLucideMetadata)
}

tasks.matching { it.name.lowercase().endsWith("sourcesjar") }.configureEach {
    dependsOn(generateLucideMetadata)
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = project.group.toString()
            version = project.version.toString()
            pom {
                name.set("Lucide Compose")
                description.set("Lucide icons for Kotlin Multiplatform Compose.")
                url.set("https://github.com/YumeYucca/lucide-compose")
                licenses {
                    license {
                        name.set("ISC License")
                        url.set("https://github.com/YumeYucca/lucide-compose/blob/Moe/LICENSE")
                    }
                }
                scm {
                    url.set("https://github.com/YumeYucca/lucide-compose")
                    connection.set("scm:git:https://github.com/YumeYucca/lucide-compose.git")
                    developerConnection.set("scm:git:ssh://git@github.com/YumeYucca/lucide-compose.git")
                }
            }
        }
    }

    repositories {
        mavenLocal()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/YumeYucca/lucide-compose")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
