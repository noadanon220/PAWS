import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.danono.paws"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.danono.paws"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "DOG_API_KEY",
            "\"${project.findProperty("DOG_API_KEY")}\""
        )
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.material.v1110)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    //AuthUI:
    implementation("com.firebaseui:firebase-ui-auth:9.0.0")

    //Realtime DB:
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    // Firestore:
    implementation(libs.firebase.firestore)

    // Storage:
    implementation("com.google.firebase:firebase-storage")

    // glide:
    implementation(libs.glide)

}