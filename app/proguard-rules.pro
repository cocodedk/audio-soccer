# R8 keep rules for the release build (isMinifyEnabled = true).
#
# No custom keep rules are needed: the app has no reflection, no JSON/Gson/Moshi model classes,
# no Retrofit interfaces, no JavascriptInterface, no JNI, and no Room/Hilt. :core is plain Kotlin
# and the Android adapters in :app call framework APIs directly.
