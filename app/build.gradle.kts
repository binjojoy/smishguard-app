plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.titan.smishguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.titan.smishguard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    androidResources {
        noCompress += "tflite"
    }

    packaging {
        jniLibs {
            pickFirsts += "**/libtensorflowlite_jni.so"
            pickFirsts += "**/libtensorflowlite_flex_jni.so"
            pickFirsts += "**/libtensorflowlite_jni_gms_client.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // FIX: Use compileOnly so the compiler sees the Interpreter class,
    // but doesn't pack it (GMS will provide it at runtime).
    compileOnly(libs.tensorflow.lite)

    // Runtime engine via Google Play Services
    implementation(libs.tensorflow.lite.gms)
    implementation(libs.tensorflow.lite.gms.support)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}