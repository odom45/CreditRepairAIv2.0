import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.creditrepairai.v2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.creditrepairai.v2"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("CREDIT_VERSION_CODE").orElse("1").get().toInt()
        versionName = providers.gradleProperty("CREDIT_VERSION_NAME").orElse("2.0.0").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val gateway = providers.gradleProperty("CREDIT_AI_GATEWAY_URL").orElse("").get()
        buildConfigField("String", "AI_GATEWAY_URL", javaString(gateway))
        buildConfigField("String", "PLAY_PRODUCT_ID", "\"creditrepairai_monthly\"")
        buildConfigField("String", "SUPPORT_EMAIL", javaString(providers.gradleProperty("CREDIT_SUPPORT_EMAIL").orElse("").get()))
        val paidAccess = providers.gradleProperty("CREDIT_PAID_ACCESS_ENABLED").orElse("false").get()
        require(paidAccess in listOf("true", "false")) { "CREDIT_PAID_ACCESS_ENABLED must be true or false" }
        buildConfigField("boolean", "PAID_ACCESS_ENABLED", paidAccess)
    }

    val releaseKeystoreFile = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
    val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
    val hasReleaseSigning = listOf(
        releaseKeystoreFile,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseUpload") {
                storeFile = file(requireNotNull(releaseKeystoreFile))
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("releaseUpload")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions.jvmTarget = "17"

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

val validateReleaseReadiness by tasks.registering {
    doLast {
        val gateway = providers.gradleProperty("CREDIT_AI_GATEWAY_URL").orElse("").get()
        val url = runCatching { URI(gateway.trim().removeSuffix("/")) }.getOrNull()
        check(url?.scheme == "https" && !url.host.isNullOrBlank() && url.rawUserInfo == null && url.query == null && url.fragment == null && url.path in listOf("", "/credit-agent")) { "Release requires a valid HTTPS site origin or /credit-agent endpoint." }
        check(providers.gradleProperty("CREDIT_SUPPORT_EMAIL").orElse("").get().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "Release requires the owner's monitored CREDIT_SUPPORT_EMAIL." }
        check(listOf("ANDROID_KEYSTORE_FILE", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD").all { !providers.environmentVariable(it).orNull.isNullOrBlank() }) { "Release requires owner-supplied upload signing configuration." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseReadiness) }

dependencies {
    // Compose 1.12+ requires compileSdk 37 and AGP 9.1+. Keep this Android 36
    // build on the final Compose 1.11 BOM until the toolchain is upgraded.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("androidx.browser:browser:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("com.android.billingclient:billing:8.3.0")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
