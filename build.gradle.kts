import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// AGP 9:n sisäänrakennettu Kotlin tuo oletuksena KGP 2.2.10:n. Uudempi KGP nostetaan classpathiin,
// ja Compose-kääntäjäliitännäisen versio on sama kuin KGP:n.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        constraints {
            classpath(libs.guava) {
                because("Fix CVE-2026-102554 in build-tool dependencies")
            }
            classpath(libs.bouncycastle.bcpkix) {
                because("Use the patched Bouncy Castle release in AGP's signing tools")
            }
            classpath(libs.jose4j) {
                because("Fix GHSA-3677-xxcr-wjqv in AGP's device testing tools")
            }
            classpath(libs.jdom2) {
                because("Fix GHSA-2363-cqg2-863c in AGP's XML processing tools")
            }
            classpath(libs.json) {
                because("Fix GHSA-ghc9-756g-vrcv in OWASP Dependency-Check")
            }
            classpath(libs.jsoup) {
                because("Fix GHSA-65r4-943x-97jj in OWASP Dependency-Check")
            }
        }
        classpath(platform(libs.jackson.bom))
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.sonarqube)
}

val sonarProjectProperties =
    java.util.Properties().apply {
        rootProject.file("sonar-project.properties").inputStream().use(::load)
    }

sonar {
    properties {
        sonarProjectProperties.forEach { key, value ->
            property(key.toString(), value.toString())
        }
    }
}

project(":app") {
    sonar {
        properties {
            // The scanner discovers javac's outputs, but AGP's built-in Kotlin compiles into separate directories.
            property("sonar.java.binaries", listOf(kotlinClasses("Debug"), javaClasses("Debug")))
            property(
                "sonar.java.test.binaries",
                listOf(kotlinClasses("DebugUnitTest"), kotlinClasses("DebugAndroidTest"), javaClasses("DebugAndroidTest")),
            )
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory.file("reports/coverage/test/debug/report.xml").get().asFile.absolutePath,
            )
        }
    }
}

project(":benchmark") {
    sonar {
        properties {
            property("sonar.java.binaries", kotlinClasses("BenchmarkRelease"))
        }
    }
}

fun Project.kotlinClasses(variant: String): File =
    tasks.named<KotlinCompile>("compile${variant}Kotlin").get().destinationDirectory.get().asFile

fun Project.javaClasses(variant: String): File =
    tasks.named<JavaCompile>("compile${variant}JavaWithJavac").get().destinationDirectory.get().asFile

tasks.named("sonar") {
    dependsOn(
        ":app:assembleDebug",
        ":app:createDebugUnitTestCoverageReport",
        ":app:compileDebugAndroidTestJavaWithJavac",
        ":benchmark:compileBenchmarkReleaseKotlin",
    )
}
