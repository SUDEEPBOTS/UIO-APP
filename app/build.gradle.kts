import java.net.URL
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.example"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("downloadRepo") {
  doLast {
    val url = URL("https://github.com/SUDEEPBOTS/UIO-TRAIN/archive/refs/heads/main.zip")
    val zipFile = file("${rootDir}/repo.zip")
    println("Downloading zip...")
    url.openStream().use { input: InputStream ->
      FileOutputStream(zipFile).use { output: FileOutputStream ->
        input.copyTo(output)
      }
    }
    println("Extracting zip...")
    val destDir = file("${rootDir}/extracted_tmp")
    destDir.deleteRecursively()
    destDir.mkdirs()
    
    copy {
      from(zipTree(zipFile))
      into(destDir)
    }
    
    println("Copying files to app directory...")
    val mainFolder = destDir.listFiles()?.firstOrNull()
    if (mainFolder != null) {
      println("Found folder: ${mainFolder.name}")
      // Copy the entire contents of app/src
      val appSrc = file("${mainFolder.absolutePath}/app/src")
      if (appSrc.exists()) {
        copy {
          from(appSrc)
          into(file("${rootDir}/app/src"))
        }
      }
      // Also copy app/build.gradle.kts and root-level properties/configs if available
      val appBuild = file("${mainFolder.absolutePath}/app/build.gradle.kts")
      if (appBuild.exists()) {
        appBuild.copyTo(file("${rootDir}/app/build.gradle.kts.downloaded"), overwrite = true)
      }
      val mainBuild = file("${mainFolder.absolutePath}/build.gradle.kts")
      if (mainBuild.exists()) {
        mainBuild.copyTo(file("${rootDir}/build.gradle.kts.downloaded"), overwrite = true)
      }
      val settingsBuild = file("${mainFolder.absolutePath}/settings.gradle.kts")
      if (settingsBuild.exists()) {
        settingsBuild.copyTo(file("${rootDir}/settings.gradle.kts.downloaded"), overwrite = true)
      }
      val metadataJson = file("${mainFolder.absolutePath}/metadata.json")
      if (metadataJson.exists()) {
        metadataJson.copyTo(file("${rootDir}/metadata.json"), overwrite = true)
      }
    }
    zipFile.delete()
    destDir.deleteRecursively()
    println("Done!")
  }
}

