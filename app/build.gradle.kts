import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
}
abstract class CommandRunner @Inject constructor(private val execOps: ExecOperations) {
    fun runCommand(command: List<String>): String {
        val byteOut = ByteArrayOutputStream()
        execOps.exec {
            commandLine = command
            standardOutput = byteOut
        }
        return String(byteOut.toByteArray()).trim()
    }
}

fun getGitSha(): String =
    project.objects.newInstance<CommandRunner>().runCommand("git rev-parse --short HEAD".split(" "))



android {
    namespace = "com.android.serialport2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.serialport2"
        minSdk = 24
        targetSdk = 36
        versionCode = 502
        versionName = "5.0.2"

        vectorDrawables {
            useSupportLibrary = true
        }

        flavorDimensions += "default"
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        buildConfigField("String", "GIT_SHA", "\"${getGitSha()}\"")

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "armeabi"
        }
    }

    signingConfigs {
        create("normal") {
            storeFile = file("../app.jks")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
        create("system") {
            storeFile = file("../platform.jks")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
    }

    productFlavors {
        create("normal") {
            applicationId = "com.android.serialport2"
            manifestPlaceholders["sharedUserId"] = "com.android.serialport2"
            manifestPlaceholders["applicationLabel"] = "SerialPort"
            manifestPlaceholders["applicationIcon"] = "@mipmap/ic_launcher"
            signingConfig = signingConfigs.getByName("normal")
        }
        create("system") {
            applicationId = "com.android.serialport2.system"
            manifestPlaceholders["sharedUserId"] = "android.uid.system"
            manifestPlaceholders["applicationLabel"] = "SystemSerialPort"
            manifestPlaceholders["applicationIcon"] = "@mipmap/ic_launcher_system"
            signingConfig = signingConfigs.getByName("system")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            productFlavors.getByName("system").signingConfig = signingConfigs.getByName("system")
            signingConfig = signingConfigs.getByName("normal")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("system")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
            //proto.srcDir("src/main/proto")
        }
    }

    applicationVariants.configureEach {
        val buildType = buildType.name
        val prefix = "android_serialport"
        val createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        if (buildType == "release") {
            outputs.all {
                val fromFile = outputFile
                val intoFile = "D:/test/${prefix}/v${android.defaultConfig.versionName}/"

                copy {
                    from(fromFile)
                    into(intoFile)
                    include("*.apk")
                    rename(
                        "app-",
                        "${prefix}_v${android.defaultConfig.versionName}_${createTime}_${getGitSha()}_"
                    )
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.accompanist.adaptive)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.java.websocket)
    implementation(libs.data.saver.core)
}