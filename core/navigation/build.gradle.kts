import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.shuaiqiu.fuckets100.core.navigation"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:settings"))
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    api(libs.androidx.navigation3.runtime)
    implementation(libs.miuix.navigation3)
    implementation(libs.androidx.navigationevent)
    implementation(libs.kotlinx.coroutines.android)
}

dependencies {
    testImplementation(libs.junit)
}
