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
        versionCode = 11
        versionName = "0.0.3.6"

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
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation(libs.play.services.maps)

    implementation ("org.osmdroid:osmdroid-android:6.1.16")
    implementation(libs.play.services.oss.licenses) // OSM

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}