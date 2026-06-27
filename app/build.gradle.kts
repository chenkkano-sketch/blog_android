plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "cc.kkano.blog"
    compileSdk = 36

    defaultConfig {
        applicationId = providers.gradleProperty("APP_ID").get()
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()

        buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("API_BASE_URL").get()}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.google.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.zxing.embedded)
}
