import java.util.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.fitotv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fitotv.app"
        minSdk = 22
        targetSdk = 34
        versionCode = (System.getenv("FITOTV_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("FITOTV_VERSION_NAME") ?: "1.0.0"
    }

    // Firma de la familia: llave privada guardada en los secretos de GitHub (nunca en el repositorio).
    // Todas las versiones se firman con ella para que se instalen encima sin borrar nada.
    val keystoreB64 = System.getenv("FITOTV_KEYSTORE_B64")
    val hasFamilyKey = !keystoreB64.isNullOrBlank()
    signingConfigs {
        if (hasFamilyKey) {
            create("familia") {
                val out = layout.buildDirectory.file("fitotv-release.keystore").get().asFile
                out.parentFile.mkdirs()
                out.writeBytes(Base64.getDecoder().decode(keystoreB64!!.trim()))
                storeFile = out
                storePassword = System.getenv("FITOTV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FITOTV_KEY_ALIAS")
                keyPassword = System.getenv("FITOTV_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sin la llave el instalable sale sin firmar y el flujo de GitHub se detiene antes de publicarlo.
            if (hasFamilyKey) signingConfig = signingConfigs.getByName("familia")
        }
        debug {
            // Versión de prueba (com.fitotv.app.dev): llave de depuración normal de Android.
            applicationIdSuffix = ".dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Media3 marca varias funciones como "inestables"; no deben frenar la compilación.
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.annotation:annotation-experimental:1.4.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val media3 = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation("junit:junit:4.13.2")
}
