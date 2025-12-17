plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jv.stellariumapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jv.stellariumapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 1. ALL ARCHITECTURES
        // Ensures native libraries are kept for all platforms, including PC emulators.
        ndk {
            abiFilters.add("armeabi-v7a") // ARM 32-bit (Old phones)
            abiFilters.add("arm64-v8a") // ARM 64-bit (Modern phones)
            abiFilters.add("x86")       // Intel 32-bit (Emulators)
            abiFilters.add("x86_64")    // Intel 64-bit (Emulators)
        }
    }

    // 2. SPLITS & UNIVERSAL APK
    // This instructs Gradle to build separate optimized APKs for each architecture
    // AND a single "Universal" APK that contains everything.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true // Creates app-universal-debug.apk
        }
    }

    buildTypes {
        // 3. OPTIMIZED DEBUGGING
        getByName("debug") {
            // High Optimization: Removes unused code (R8)
            isMinifyEnabled = true 
            
            // High Optimization: Removes unused resources (drastically reduces size)
            isShrinkResources = true
            
            // Uses the aggressive optimization proguard rules
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Ensures you can still attach a debugger (though variables may be renamed)
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // With isMinifyEnabled=true, this library will be optimized heavily
    implementation("androidx.compose.material:material-icons-extended") 
}