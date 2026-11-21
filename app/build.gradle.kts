import com.android.build.api.variant.HostTestBuilder
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.stability.analyzer)
}

val analyticsEndpointOverride = providers.gradleProperty("analyticsEndpoint")

fun analyticsEndpointField(defaultOrigin: String = ""): String {
    val endpoint = analyticsEndpointOverride.getOrElse(defaultOrigin).trim().trimEnd('/')
    if (endpoint.isNotEmpty()) {
        val uri = URI(endpoint)
        require(
            uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null &&
                uri.rawPath.isEmpty() && uri.rawQuery == null && uri.rawFragment == null,
        ) { "analyticsEndpoint must be an HTTPS origin." }
    }
    return "\"$endpoint\""
}

android {
    namespace = "fi.clocklings"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "fi.clocklings.blender"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "fi.clocklings.ClocklingsTestRunner"
        buildConfigField("String", "ANALYTICS_ENDPOINT", analyticsEndpointField())
    }

    // This independent visual experiment has no release signing configuration.

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            buildConfigField(
                "String",
                "ANALYTICS_ENDPOINT",
                analyticsEndpointField("https://clocklings-analytics.emmahota.workers.dev"),
            )
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Only local measurement variants use the standard debug key.
        create("benchmarkRelease") {
            initWith(getByName("release"))
            buildConfigField("String", "ANALYTICS_ENDPOINT", analyticsEndpointField())
            signingConfig = signingConfigs.getByName("debug")
        }
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            buildConfigField("String", "ANALYTICS_ENDPOINT", analyticsEndpointField())
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Original pixels are packaged only in the test APK for Android decoder parity checks.
    sourceSets
        .getByName("androidTest")
        .assets
        .directories
        .add(rootProject.file("art/blender/atlases").absolutePath)

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "3g"
            it.extensions.configure<JacocoTaskExtension> {
                // Robolectric loads application classes without a source location.
                isIncludeNoLocationClasses = true
                includes = listOf("fi.clocklings.*")
            }
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
}

androidComponents {
    // AGP 9.5 preview tasks require unit-test resources when stability checks realize all tasks.
    beforeVariants {
        it.hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
    }
}

abstract class ResourcePackagingLock : BuildService<BuildServiceParameters.None>

if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    // Avoid intermittent access errors when variants copy the same raw resources.
    val resourcePackagingLock =
        gradle.sharedServices
            .registerIfAbsent(
                "resourcePackagingLock",
                ResourcePackagingLock::class,
            ) {
                maxParallelUsages.set(1)
            }
    tasks.configureEach {
        if (name.startsWith("package") && name.endsWith("Resources")) {
            usesService(resourcePackagingLock)
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
    coloredOutput.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        xml.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    suppressionFiles = listOf(rootProject.file("config/dependency-check-suppressions.xml").absolutePath)
    failBuildOnCVSS = 7f
    autoUpdate =
        providers
            .environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE")
            .map { it.lowercase() in listOf("1", "true", "yes", "on") }
            .getOrElse(true)
    analyzers {
        ossIndex {
            enabled = false
        }
    }
    nvd {
        datafeedUrl = "https://nvd.nist.gov/feeds/json/cve/2.0/nvdcve-2.0-{0}.json.gz"
    }
}

// Version 13.0.0 requires the data directory on tasks to override its built-in default.
tasks.withType<org.owasp.dependencycheck.gradle.tasks.ConfiguredTask>().configureEach {
    data.directory.set(
        providers
            .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
            .orElse(
                rootProject.layout.projectDirectory
                    .dir(".gradle/dependency-check-data")
                    .asFile.absolutePath,
            ),
    )
}

// Compose-kääntäjä ja vakausanalysaattori lukevat saman vakauskonfiguraation (domain ja kokoelmat).
val composeStabilityConfig = isolated.rootProject.projectDirectory.file("config/compose-stability.conf")

composeCompiler {
    stabilityConfigurationFiles.add(composeStabilityConfig)
}

composeStabilityAnalyzer {
    stabilityConfigurationFiles.add(composeStabilityConfig)
}

dependencies {
    baselineProfile(project(":benchmark"))
    lintChecks(libs.android.security.lints)
    detektPlugins(libs.compose.rules.detekt)
    ktlintRuleset(libs.compose.rules.ktlint)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.google.play.billing)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    constraints {
        testImplementation(libs.guava) {
            because("Fix CVE-2026-102554 in Robolectric's transitive dependency")
        }
        add("ktlint", libs.logback.classic.get()) {
            because("Use the patched Logback release in ktlint")
        }
    }
    testImplementation(composeBom)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.espresso.core)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
