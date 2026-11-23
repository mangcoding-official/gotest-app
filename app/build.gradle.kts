import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.gotest.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gotest.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Server address per build type (T-018): release talks to gotest.id.
        buildConfigField("String", "API_BASE_URL", "\"https://gotest.id/\"")
        // Release always talks to the API; debug uses the fakes unless `gotest.useApi=true` (T-026 required).
        buildConfigField("boolean", "USE_API", "true")
    }

    buildTypes {
        debug {
            // A local server by default (the emulator reaches the host as 10.0.2.2); point at staging with
            // `gotest.apiBaseUrl=https://…/` in ~/.gradle/gradle.properties or with -Pgotest.apiBaseUrl=….
            val apiBaseUrl = providers.gradleProperty("gotest.apiBaseUrl").getOrElse("http://10.0.2.2:8000/")
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            // `gotest.useApi=true` (or -Pgotest.useApi=true) puts the API repositories in place of the fakes in
            // src/debug, so a debug build can be tried against staging (T-026 required, A2e review).
            val useApi = providers.gradleProperty("gotest.useApi").getOrElse("false").toBoolean()
            buildConfigField("boolean", "USE_API", useApi.toString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        // java.time on API 24-25.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Indonesian has no grammatical plural; "%d dari %d" strings are already correct.
        disable += "PluralsCandidate"
    }
}

// T-018 review 1: the fake repositories accept any email, so a release must never ship them. While a Hilt
// module in src/main still names a Fake* class, packaging a release APK or bundle fails. Debug builds,
// unit tests, and lintDebug are not affected; the check passes on its own once the fakes move to src/debug
// or are replaced by the API.
val checkNoFakeData by tasks.registering {
    val modules = fileTree("src/main/java") { include("**/di/*.kt") }
    inputs.files(modules)
    doLast {
        val fake = Regex("""\bFake[A-Z]\w*""")
        val bound =
            modules.files
                .filter { fake.containsMatchIn(it.readText()) }
                .map { it.name }
                .sorted()
        if (bound.isNotEmpty()) {
            throw GradleException(
                "Release build refused: fake data is still bound in ${bound.joinToString()}. " +
                    "Bind the API repositories, or move the fakes to src/debug, before releasing (T-018).",
            )
        }
    }
}
tasks
    .matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease", "packageReleaseBundle") }
    .configureEach { dependsOn(checkNoFakeData) }

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    // Question and passage images from the server (`/storage/...`, T-026 A2b review 5).
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.zxing.core)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    // Question HTML and KaTeX math in a WebView served from app assets (T-026 criterion 6).
    implementation(libs.androidx.webkit)
    ksp(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    ktlintRuleset(libs.compose.rules.ktlint)
    detektPlugins(libs.compose.rules.detekt)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
