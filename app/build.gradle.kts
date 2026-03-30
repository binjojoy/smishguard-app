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

    // Modern syntax for keeping .tflite files uncompressed
    androidResources {
        noCompress += "tflite"
    }

    // Crucial for native library conflicts when using GMS
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Ensures the JNI bridge works correctly on your Xiaomi phone
            pickFirsts += "**/libtensorflowlite_jni.so"
            //pickFirsts += "**/libtensorflowlite_flex_jni.so"
            //pickFirsts += "**/libtensorflowlite_jni_gms_client.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // THE CRITICAL FIX: Changed from compileOnly to implementation.
    // This packs the 'Interpreter' and 'Options' classes into the APK,
    // solving the NoClassDefFoundError you saw on mobile.
    //implementation(libs.tensorflow.lite.api)
    implementation(libs.tensorflow.lite)

    // The GMS Runtime (The "Engine" on the phone)
    //implementation(libs.tensorflow.lite.gms)
    //implementation(libs.tensorflow.lite.gms.support)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}