plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "dev.klangretro.player"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.klangretro.player"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-native"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        debug { applicationIdSuffix = ".nativepreview"; versionNameSuffix = "-preview" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    val media3 = "1.10.1"
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.media3:media3-exoplayer:" + media3)
    implementation("androidx.media3:media3-session:" + media3)
    implementation("net.jthink:jaudiotagger:3.0.1")
}
