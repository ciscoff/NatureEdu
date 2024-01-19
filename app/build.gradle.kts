plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val apkFileName: String by project
val versionFileName: String by project
val keyBuild: String by project
val keyMinor: String by project
val keyMajor: String by project

var buildNum: Int = 0
var minorNum: Int = 0
var majorNum: Int = 0

fun readVersions() {

    val data = mutableMapOf<String, Int>()

    File(versionFileName).useLines { seq ->
        seq.forEach { line ->
            val chunks = line.split("=")
            if (chunks.size == 2) {
                data[chunks[0]] = chunks[1].toInt()
            }
        }
    }

    var buildVersion = (data[keyBuild] ?: 0) + 1
    var minorVersion = data[keyMinor] ?: 0
    var majorVersion = data[keyMajor] ?: 0

    if (buildVersion > 999) {
        buildVersion = 0
        minorVersion += 1
        if (minorVersion > 99) {
            minorVersion = 0
            majorVersion += 1
        }
    }

    buildNum = buildVersion
    minorNum = minorVersion
    majorNum = majorVersion
}

fun saveVersions() {
    val fileContent = "$keyBuild=$buildNum\n$keyMinor=$minorNum\n$keyMajor=$majorNum"
    File(versionFileName).writeText(fileContent)
}

android {
    namespace = "dev.barabu.nature"
    compileSdk = 34

    val signConfigName = "sign"

    defaultConfig {
        applicationId = "dev.barabu.nature"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        readVersions()
        setProperty("archivesBaseName", "${apkFileName}_${majorNum}.${minorNum}.${buildNum}")
        saveVersions()
    }

    signingConfigs {

        create(signConfigName) {
            keyAlias = "key0"
            keyPassword = "123456"
            storeFile = file("key_store/barabu.jks")
            storePassword = "123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(signConfigName)
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(signConfigName)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    val coroutinesVersion = "1.7.3"
    val lifecycleKtxVersion = "2.7.0"

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":app:base"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleKtxVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleKtxVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleKtxVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}