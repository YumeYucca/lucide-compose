@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import LucideMetadataGenerator.generate as generateMetadata


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.valkyrie)
    id("maven-publish")
}

group = "moe.alex3236"
version = "0.1.0-SNAPSHOT"

kotlin {
    android {
        namespace = "moe.alex3236.compose.lucide"
        compileSdk = 35
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

val lucidePackage = "moe.alex3236.compose.lucide"
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
    }

    repositories {
        mavenLocal()
    }
}
