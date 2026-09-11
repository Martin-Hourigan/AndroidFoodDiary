
plugins {
    // AGP 9+ has built-in Kotlin support, so no kotlin-android plugin here.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.mahourigan.fooddiary"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.mahourigan.fooddiary"
        minSdk = 26
        targetSdk = 37
        // CI passes these from the git tag, so the number that decides whether an
        // update installs is derived rather than remembered. Local builds keep
        // the defaults.
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("appVersionName") as String?) ?: "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // The key and its password live in ~/.gradle/gradle.properties,
            // outside every repository. Nothing signing-related exists inside
            // this project, so there is no file here to commit by mistake --
            // the protection is structural rather than a rule in .gitignore.
            //
            // A machine without those properties still builds. It just
            // produces an unsigned release, which is exactly what should
            // happen on someone else's clone or a fork's CI.
            val storePath = findProperty("signingStoreFile") as String?
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = findProperty("signingStorePassword") as String?
                keyAlias = "fooddiary"
                keyPassword = findProperty("signingKeyPassword") as String?
            }
        }
    }

    buildTypes {
        release {
            if ((findProperty("signingStoreFile") as String?) != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        // UpdateRow compares BuildConfig.VERSION_CODE against the latest release.
        buildConfig = true
        compose = true
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("compose_stability.conf"),
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // No network dependency, and there is not going to be one. The analysis is
    // arithmetic over the local log — nothing here calls out to anything, so
    // there is no HTTP client, no Firebase and no food database.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
