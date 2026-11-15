import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
}

// The version lives in gradle.properties, where the release workflow and F-Droid's
// checkupdates both read it. See the comment there before bumping it.
val appVersionName: String = providers.gradleProperty("VERSION_NAME").get()
val appVersionCode: Int = providers.gradleProperty("VERSION_CODE").get().toInt()

// Optional release signing — supplied via env in CI; absent locally so debug still builds.
fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
val ksFile = env("KEYSTORE_PATH")?.let { rootProject.file(it) }?.takeIf { it.isFile }
val ksPassword = env("KEYSTORE_PASSWORD")
val ksAlias = env("KEY_ALIAS")
val ksKeyPassword = env("KEY_PASSWORD")
val hasSigning = ksFile != null && ksPassword != null && ksAlias != null && ksKeyPassword != null

android {
    namespace = "dk.cocode.soccer"
    compileSdk = 36

    defaultConfig {
        applicationId = "dk.cocode.soccer"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }
    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            // Its own app id: a debug build installs beside the release-signed app instead of
            // needing an uninstall (the two are signed with different keys).
            applicationIdSuffix = ".debug"
        }
        release {
            // Release builds are shrunk with R8, as F-Droid expects.
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
    }
    // AGP otherwise adds a "Dependency metadata" block to the APK signing block,
    // encrypted with a key only Google Play holds. F-Droid rejects release APKs
    // that carry it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        jniLibs {
            // The only .so files in the APK are prebuilts from AndroidX (datastore, graphics-path).
            // AGP strips them with whatever NDK it finds, so a rebuild without that exact NDK
            // produces different bytes — F-Droid's builder has none unless its recipe pins one.
            // Keeping the symbols leaves the libraries exactly as their AARs ship them, which
            // rebuilds identically anywhere, and costs a few kB.
            keepDebugSymbols += "**/*.so"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}

// Coverage and CI only target the debug variant; disable release unit tests so Kover does not run
// them (they lack the debug-only Compose `ui-test-manifest` needed by the Robolectric UI tests).
tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
    if (name.contains("Release")) enabled = false
}
