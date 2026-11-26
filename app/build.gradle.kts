import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Membaca kredensial signing dari keystore.properties (lokal) atau environment variable (CI)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.rahmatulula.motocare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rahmatulula.motocare"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "KEYSTORE_FILE")
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val rel = signingConfigs.getByName("release")
            // Jika keystore tidak tersedia, pakai debug key agar tetap bisa dibuild/diuji
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX + Material Design
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // MVVM: ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Room (penyimpanan lokal persisten)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase Authentication
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Retrofit (API cuaca Open-Meteo)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // WorkManager (notifikasi terjadwal)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines & image loading
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
