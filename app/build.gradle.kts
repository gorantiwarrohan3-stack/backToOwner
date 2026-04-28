import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

private val appwriteProperties =
    Properties().apply {
        val f = rootProject.file("appwrite.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

private fun appwriteProp(key: String, default: String = ""): String =
    (appwriteProperties.getProperty(key, default) ?: default).trim()

private val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

private fun localProp(key: String, default: String = ""): String =
    (localProperties.getProperty(key, default) ?: default).trim()

android {
    namespace = "com.wpi.backtoowner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wpi.backtoowner"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val projectId = appwriteProp("appwrite.projectId")
        buildConfigField(
            "String",
            "APPWRITE_ENDPOINT",
            "\"${appwriteProp("appwrite.endpoint", "https://fra.cloud.appwrite.io/v1")}\"",
        )
        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"$projectId\"")
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"${appwriteProp("appwrite.databaseId")}\"")
        buildConfigField(
            "String",
            "APPWRITE_STORAGE_BUCKET_ID",
            "\"${appwriteProp("appwrite.storageBucketId")}\"",
        )
        manifestPlaceholders["appwriteOAuthScheme"] =
            if (projectId.isEmpty()) "appwrite-callback-UNSET" else "appwrite-callback-$projectId"
        manifestPlaceholders["mapsApiKey"] = localProp("MAPS_API_KEY")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.appwrite.android)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.tensorflow.lite)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
