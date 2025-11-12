plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.qujianma.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.qujianma.app"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        // 当创建基线文件时不中止构建
        abortOnError = false
        // 忽略警告，只检查错误
        ignoreWarnings = true
        // 即使创建了基线文件也继续构建
        checkReleaseBuilds = false
        // 当创建基线文件时继续构建
        continueOnFailure = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    // 添加CardView依赖
    implementation("androidx.cardview:cardview:1.0.0")
    // 添加Gson依赖用于JSON序列化
    implementation("com.google.code.gson:gson:2.8.8")
    // 添加SwipeRefreshLayout依赖
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // 添加WorkManager依赖
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    // 添加OkHttp依赖用于网络请求
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    // 添加网络状态权限依赖
    implementation("androidx.core:core:1.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}