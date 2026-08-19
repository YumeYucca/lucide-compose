@file:OptIn(ExperimentalWasmDsl::class)

import br.com.devsrsouza.svg2compose.IconNameTransformer
import br.com.devsrsouza.svg2compose.Svg2Compose
import br.com.devsrsouza.svg2compose.VectorType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.*
import LucideMetadataGenerator.generate as generateMetadata


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

// Keep the package coordinates stable for local and GitHub Packages publication.
group = providers.gradleProperty("group").orElse("com.github.YumeYucca").get()
version = providers.gradleProperty("version").orElse("1.0.0-SNAPSHOT").get()

kotlin {
    androidTarget()
    jvm("desktop")
    js(IR) {
        browser()
        nodejs()
    }
    applyDefaultHierarchyTemplate()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    linuxX64()

    wasmJs {
        browser()
    }


    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
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

android {
    namespace = "moe.alex3236.compose.lucide"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

buildscript {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.google.com")
        maven("https://jcenter.bintray.com")
    }
    dependencies {
        // Use the published JitPack release. The old -SNAPSHOT coordinate
        // expands to an invalid artifact name and prevents JitPack from
        // configuring the project before publication.
        classpath("com.github.DevSrSouza:svg-to-compose:0.11.0")
        classpath("com.google.guava:guava:23.0")
        classpath("com.android.tools:sdk-common:31.2.1")
        classpath("com.android.tools:common:31.2.1")
        classpath("com.squareup:kotlinpoet:1.14.2")
        classpath("org.ogce:xpp3:1.1.6")
    }
}

val generateCompose = tasks.register("generateCompose") {
    group = "generation"
    description = "Generate Compose icons from SVG files"

    val assetsDir = file("lucide/icons")
    val generatedSrcDir = layout.buildDirectory.dir("generated/kotlin").get().asFile

    inputs.dir(assetsDir)
    outputs.dir(generatedSrcDir)

    doLast {
        // Clean the output directory before generating
        generatedSrcDir.deleteRecursively()
        generatedSrcDir.mkdirs()

        Svg2Compose.parse(
            applicationIconPackage = "moe.alex3236.compose.lucide",
            accessorName = "Lucide",
            outputSourceDirectory = generatedSrcDir,
            vectorsDirectory = assetsDir,
            type = VectorType.SVG,
            allAssetsPropertyName = "AllIcons",
            generatePreview = false,
            iconNameTransformer = object : IconNameTransformer {
                override fun invoke(iconName: String, group: String): String {
                    return iconName.split("-").joinToString("") {
                        it.replaceFirstChar { it ->
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }
                    }
                }
            }
        )

        generateMetadata(
            assetsDir = assetsDir,
            srcDir = generatedSrcDir,
            basePackage = "moe.alex3236.compose.lucide",
        )
    }
}

// Ensure icons are generated
tasks.matching { it.name.contains("compile", ignoreCase = true) }.configureEach {
    dependsOn(generateCompose)
}

tasks.matching { it.name.lowercase().endsWith("sourcesjar") }.configureEach {
    dependsOn(generateCompose)
}

publishing {
    publications {
        // Kotlin Multiplatform creates the platform publications after the
        // targets are configured. Apply the project coordinates to every one.
        withType<MavenPublication> {
            groupId = project.group.toString()
            version = project.version.toString()
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
