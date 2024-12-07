import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.atlys.barcode_scan"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.camerax)
    implementation(libs.mlkit.barcode.scanning)
}

publishing {

    val secretProperties = getSecretProperties()

    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "com.atlys"
            artifactId = "barcode-scan"
            version = "0.0.2"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/atlyslabs/barcode-scan-android-sdk")
            credentials {
                username = secretProperties.getProperty("GITHUB_USER")
                password = secretProperties.getProperty("GITHUB_TOKEN")
            }
        }
    }
}

fun getSecretProperties(filePath: String = "secret.properties"): Properties {
    val properties = Properties()
    val secretFile = File(filePath)
    if (secretFile.exists()) {
        FileInputStream(secretFile).use { properties.load(it) }
    }
    return properties
}