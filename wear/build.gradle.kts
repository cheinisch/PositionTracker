plugins {
    //alias(libs.plugins.android.application)
    id("com.android.application")
}

android {
    namespace = "de.heimfisch.positiontracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.heimfisch.positiontracker"
        minSdk = 30
        targetSdk = 35
        versionCode = 13
        versionName = "0.0.4.0"

        //wearAppUnbundled = true
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
    implementation("androidx.wear:wear:1.3.0") // für WearableActivity
    implementation("org.osmdroid:osmdroid-android:6.1.16")
}