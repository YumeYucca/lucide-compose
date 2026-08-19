@file:OptIn(ExperimentalWasmDsl::class)

import br.com.devsrsouza.svg2compose.IconNameTransformer
import br.com.devsrsouza.svg2compose.Svg2Compose
import br.com.devsrsouza.svg2compose.VectorType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*
import LucideMetadataGenerator.generate as generateMetadata


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

// Publish the latest development build under one stable snapshot coordinate.
group = "io.github.YumeYucca"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(24)

    android {
        namespace = "io.github.yumeyucca.lucide"
        compileSdk = 36
        minSdk = 21
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    applyDefaultHierarchyTemplate()

    sourceSets {
        getByName("commonMain") {
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
        // The generator is published separately and is only needed while
        // producing the Compose ImageVector sources.
        classpath(libs.svg.to.compose)
        classpath(libs.guava)
        classpath(libs.sdk.common)
        classpath(libs.common)
        classpath(libs.kotlinpoet)
        classpath(libs.xpp3)
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
            applicationIconPackage = "io.github.yumeyucca.lucide",
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
            basePackage = "io.github.yumeyucca.lucide",
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
