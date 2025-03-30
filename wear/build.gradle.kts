plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // Kotlin Plugin hinzufügen
}

android {
    namespace = "de.heimfisch.positiontracker.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.heimfisch.positiontracker.wear"
        minSdk = 30
        targetSdk = 35
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


}

dependencies {
    //implementation(project(":app"))
    implementation("androidx.wear:wear:1.3.0")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.google.android.gms:play-services-wearable:17.1.0") // Data Layer API
}