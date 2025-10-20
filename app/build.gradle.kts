


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.gestura"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gestura"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "LARA_ACCESS_KEY_ID", "\"7LSFSJI1FCKIKU2V583MBONTUQ\"")
        buildConfigField("String", "LARA_ACCESS_KEY_SECRET", "\"y3-Xkj0v7ckHc1rH0D1KHyKVRCkQ8PSWwiXDTBMjQgk\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        // Enable compose, which is necessary for the Compose compiler plugin
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // It's common to use 1.8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Add the compose compiler extension version, linking it to your Kotlin version
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // This version is compatible with Kotlin 2.0.0
    }
}

dependencies {
    // Correctly implement the Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Use aliases from libs.versions.toml for ALL dependencies
    // Remove pinned versions from all Compose artifacts
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.material3)

    // Use aliases for your other dependencies as well for consistency
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material) // from com.google.android.material
    implementation(libs.lara.sdk)

    // Test dependencies using aliases
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.espresso.core)
}

