plugins {
    id("dynamicforms.android.application")
    id("dynamicforms.compose")
    id("dynamicforms.hilt")
    id("dynamicforms.detekt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lfr.dynamicforms"

    defaultConfig {
        applicationId = "com.lfr.dynamicforms"
        versionCode = 1
        versionName = "1.0"

        // Default to Fly.io backend; override in debug to use local backend
        buildConfigField("String", "BASE_URL", "\"https://dynamicforms-lfr.fly.dev/\"")
        buildConfigField("Boolean", "USE_MOCK", "false")
    }

    buildTypes {
        debug {
            // Point to real Ktor backend (emulator localhost)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            buildConfigField("Boolean", "USE_MOCK", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Modules
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:form-wizard"))
    implementation(project(":feature:form-list"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Serialization (for Json provider in NetworkModule)
    implementation(libs.kotlinx.serialization.json)

    // Networking (for NetworkModule)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Room (for DatabaseModule)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // WorkManager (for DI wiring)
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Timber
    implementation(libs.timber)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
