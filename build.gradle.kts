plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":core"))
    kover(project(":app"))
}

kover {
    reports {
        filters {
            excludes {
                // @Composable functions carry Compose-compiler-generated recomposition branches
                // (startRestartGroup/skipToGroupEnd/updateScope) that are not reachable by tests.
                // They are still exercised by the Robolectric render tests and the emulator screenshot.
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        total {
            verify {
                rule {
                    // 100% LINE coverage. Branch is not gate-enforced: idiomatic Kotlin inline/synthetic
                    // constructs (MutableStateFlow.update's CAS retry, Iterable.all, ...) emit branch
                    // stubs that single-threaded tests cannot reach.
                    minBound(100)
                }
            }
        }
    }
}

tasks.register("buildSmoke") {
    group = "verification"
    description = "Build debug APK, run unit tests + the 100% coverage gate, and lint."
    dependsOn("koverVerify", ":app:lintDebug", ":app:assembleDebug")
}
