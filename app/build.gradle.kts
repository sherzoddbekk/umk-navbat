import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release imzo (signing) ma'lumotlari `keystore.properties` faylidan o'qiladi.
// Bu fayl git'ga TUSHMAYDI (.gitignore) — parollar maxfiy qoladi.
// Fayl bo'lmasa (masalan CI yoki boshqa dev), release imzosiz qoladi va
// `bundleRelease` xato beradi — bu kutilgan holat.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "uz.jurabekov.guard"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "uz.jurabekov.umknavbat"
        minSdk = 24
        targetSdk = 36
        // ⬆️ MUHIM: Play'ga qayta yuklash uchun versionCode oshirilishi SHART.
        // versionName foydalanuvchiga ko'rinadi (Play sahifa, About).
        // v1.3.0 — navbatni bekor qilish + "Yangilik!" e'loni.
        versionCode = 8
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://apigate.uzbeksteel.uz/\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://apigate.uzbeksteel.uz/\"")
        }
        release {
            // Imzo faqat keystore.properties mavjud bo'lganda qo'llanadi.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Play Console "Этот объект содержит нативный код..." warning'ni
            // bartaraf etadi. Pusher / OkHttp / Kotlin coroutines tranzitiv
            // ravishda native .so fayllar olib keladi - debug symbols crash
            // analytics'da ANR/crash stack trace'larini o'qishni osonlashtiradi.
            // Build size'ga ta'siri minimal (~1-3 MB AAB ichida, foydalanuvchiga
            // yetkazilmaydi).
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core / Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // websocket - foreground UI real-time yangilash uchun qoldiriladi
    implementation(libs.pusher.java.client)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // QR Code — permit dialog'da ruxsatnoma uuid'ini scan qilish uchun.
    // Pure-Java (native .so yo'q), ASF License, Play Console qo'shimcha
    // talab qo'ymaydi. Faqat `core` — encoder/decoder qismi yo'q
    // (decoder kamera bilan ishlash uchun — bizga kerak emas).
    implementation(libs.zxing.core)

    //    for update
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Test
    testImplementation(libs.junit)
    // ViewModel testlari uchun — `Dispatchers.setMain` + `runTest`.
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
