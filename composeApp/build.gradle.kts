import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization")
}

val localSupabaseProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

val localSupabaseUrl = localSupabaseProperties.getProperty("SUPABASE_URL").orEmpty()
val localSupabaseKey = localSupabaseProperties.getProperty("SUPABASE_KEY").orEmpty()

fun supabaseSetting(name: String, localValue: String) =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(localValue)

val supabaseUrl = supabaseSetting("SUPABASE_URL", localSupabaseUrl)
val supabaseKey = supabaseSetting("SUPABASE_KEY", localSupabaseKey)

val generateSupabaseConfig by tasks.registering {
    val outputDirectory = layout.buildDirectory.dir("generated/supabase/commonMain")
    outputs.dir(outputDirectory)
    inputs.property("SUPABASE_URL", supabaseUrl)
    inputs.property("SUPABASE_KEY", supabaseKey)

    doLast {
        val url = inputs.properties["SUPABASE_URL"]?.toString()?.trim().orEmpty()
        val key = inputs.properties["SUPABASE_KEY"]?.toString()?.trim().orEmpty()
        check(url.isNotEmpty()) {
            "Missing SUPABASE_URL. Set it in local.properties, as a Gradle property, or as an environment variable."
        }
        check(key.isNotEmpty()) {
            "Missing SUPABASE_KEY. Set it in local.properties, as a Gradle property, or as an environment variable."
        }

        val escapedUrl = url.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        val escapedKey = key.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        val configFile = outputs.files.singleFile
            .resolve("org/example/padeltips/data/SupabaseConfig.kt")
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            package org.example.padeltips.data

            internal object SupabaseConfig {
                const val URL = "$escapedUrl"
                const val KEY = "$escapedKey"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSupabaseConfig)

            dependencies {

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)

                implementation(project.dependencies.platform("io.github.jan-tennert.supabase:bom:3.0.1"))

                // Lo que necesitas de Supabase
                implementation("io.github.jan-tennert.supabase:postgrest-kt") // Para la base de datos
                implementation("io.github.jan-tennert.supabase:auth-kt")   // Para el Login (Auth)

                // Motor base de Ktor
                implementation("io.ktor:ktor-client-core:3.0.0")
            }
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.8.2")
            implementation("io.ktor:ktor-client-okhttp:3.0.0")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.0.0")
        }
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}

android {
    namespace = "org.example.padeltips"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.padeltips"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core)
    debugImplementation(compose.uiTooling)
}
