plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "fi.clocklings.benchmark"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 2
            }
    }
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    useConnectedDevices = true
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
    coloredOutput.set(false)
    filter {
        exclude("**/build/**")
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    constraints {
        implementation(libs.kotlinx.coroutines.android) {
            because("Match the Coroutines version of the profiled app")
        }
        add("ktlint", libs.logback.classic.get()) {
            because("Use the patched Logback release in ktlint")
        }
        implementation(libs.moshi) {
            because("Remove the benchmark's transitive request for vulnerable Okio 2.10.0")
        }
        implementation(libs.wire.runtime) {
            because("Fix GHSA-9rm7-3qhh-h2mc in the benchmark trace processor")
        }
    }
}
