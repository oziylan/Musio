import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.musio"
    compileSdk = 35

    val localPropertiesFile = project.rootProject.file("local.properties")
    val properties = Properties().apply {
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        } else {
            throw GradleException("local.properties file not found!")
        }
    }

    defaultConfig {
        applicationId = "com.example.musio"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SPOTIFY_CLIENT_ID", properties["SPOTIFY_CLIENT_ID"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", properties["SPOTIFY_CLIENT_SECRET"]?.toString()?.let { "\"$it\"" } ?: "\"\"")

        manifestPlaceholders["redirectHostName"] = "callback"
        manifestPlaceholders["redirectSchemeName"] = "com.example.musio"

        buildConfigField("String", "FIREBASE_API_KEY", properties["FIREBASE_API_KEY"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
        // Pour Google Maps
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("google.maps.api.key", "")
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
        buildConfig = true
        viewBinding = true
        compose = false
    }

    sourceSets["main"].jniLibs.srcDirs("libs")
}
dependencies {
    // Firebase
    implementation((platform("com.google.firebase:firebase-bom:33.12.0")))
    implementation(("com.google.firebase:firebase-auth"))
    implementation(("com.google.firebase:firebase-firestore"))
    implementation(("com.google.firebase:firebase-core:20.1.0"))
    implementation (("com.google.android.gms:play-services-auth:21.3.0"))
    implementation (("com.google.android.gms:play-services-location:21.0.1"))
    implementation (("com.google.firebase:firebase-analytics"))
    implementation (("com.google.android.gms:play-services-base:18.3.0"))

    // AndroidX
    implementation(("androidx.appcompat:appcompat:1.4.1"))
    implementation(("com.google.android.material:material:1.5.0"))
    implementation(("androidx.activity:activity-ktx:1.5.0"))
    implementation(("androidx.constraintlayout:constraintlayout:2.1.3"))
    implementation(("androidx.core:core-ktx:1.7.0"))
    implementation(("androidx.annotation:annotation:1.8.0"))
    implementation(("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"))
    implementation(("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"))

    // Tests
    testImplementation(("junit:junit:4.13.2"))
    androidTestImplementation(("androidx.test.ext:junit:1.1.3"))
    androidTestImplementation(("androidx.test.espresso:espresso-core:3.4.0"))

    // Maps SDK for Android
    implementation(("com.google.android.gms:play-services-maps:19.0.0"))
    implementation (("com.google.maps.android:android-maps-utils:2.3.0"))

    // Spotify (files .aar)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("spotify-app-remote-release-0.8.0.aar"))))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("spotify-auth-release-2.1.0.aar"))))

    // Requêtes réseau
    implementation(("com.squareup.okhttp3:okhttp:4.9.1"))
    implementation(("com.squareup.picasso:picasso:2.71828"))
    implementation(("com.squareup.retrofit2:retrofit:2.9.0"))
    implementation(("com.squareup.retrofit2:converter-gson:2.9.0"))
    implementation(("com.google.code.gson:gson:2.8.8"))

    // ViewModel
    implementation (("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"))
    implementation (("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2"))

    //
    implementation (("com.google.android.material:material:1.9.0"))

    implementation (("de.hdodenhof:circleimageview:3.1.0"))

}

// Apply Google Plugin service
apply(plugin = "com.google.gms.google-services")

