plugins {
    // Android application plugin from libs.versions.toml
    alias(libs.plugins.android.application)

    // Firebase / Google Services plugin (version is defined in top-level build.gradle)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.ridergurdianx"
    compileSdk = 34   // Use an SDK version you have installed (34 is safe for now)

    defaultConfig {
        applicationId = "com.example.ridergurdianx"
        minSdk = 24
        targetSdk = 34   // Match this to a installed SDK; 34 is stable
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // keep simple for now
            isMinifyEnabled = false
        }
    }

    // Java compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Optional but useful if you want to use it in layouts
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX / Material (from your version catalog)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ---------- Firebase ----------
    // BOM: manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    // Firestore (for SOS contacts / data storage)
    implementation("com.google.firebase:firebase-firestore")

    // Analytics (optional but good for tracking usage)
    implementation("com.google.firebase:firebase-analytics")

    // Auth (for login / signup in future)
    implementation("com.google.firebase:firebase-auth")

    // ---------- Google Play Services ----------
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Fused Location Provider (for GPS / live location)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    implementation("com.google.firebase:firebase-database")
    implementation("androidx.core:core-ktx:1.12.0") // you likely already have core

    implementation("com.google.maps.android:android-maps-utils:2.2.5")
    implementation("com.google.android.libraries.places:places:3.5.0")


}
