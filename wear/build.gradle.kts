plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "de.heimfisch.positiontracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.heimfisch.positiontracker"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        wearAppUnbundled = true
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

    implementation(libs.play.services.wearable)
}