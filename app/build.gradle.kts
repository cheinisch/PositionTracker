plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "de.heimfisch.positiontracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.heimfisch.positiontracker"
        minSdk = 29
        targetSdk = 35
        versionCode = 13
        versionName = "0.0.4.0"

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
    buildFeatures {
        viewBinding = true
    }

    bundle {
        storeArchive {
            enable = true
        }
    }

    //dynamicFeatures = [":wear"] // Dynamische Feature Abhängigkeit
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.play.services.maps)

    implementation ("org.osmdroid:osmdroid-android:6.1.20")
    implementation(libs.play.services.oss.licenses) // OSM

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(project(":wear")) // Abhängigkeit zum Wear OS Model
}