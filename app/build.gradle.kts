import com.android.build.api.artifact.SingleArtifact
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.ksp)
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
            signingConfig = when (android.productFlavors.find { it.name == "system" }?.name) {
                "system" -> signingConfigs.getByName("system")
                else -> signingConfigs.getByName("normal")
            }
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

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.glance)
}

androidComponents {
    val id = "android_serialport"
    val createTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
    val versionName = android.defaultConfig.versionName ?: ""
    var intoFile = "D:/test/$id/v${versionName}"
    onVariants(selector().withBuildType("release")) { variant ->
        val variantCap =
            variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val copyApkTask = tasks.register<Copy>("copy${variantCap}Apk") {
            from(variant.artifacts.get(SingleArtifact.APK))
            into(intoFile)
            include("*.apk")
            rename("app-", "${id}_v${versionName}_${createTime}_${getGitSha()}_")
        }
        tasks.matching { it.name == "assemble${variantCap}" }
            .configureEach { finalizedBy(copyApkTask) }
        tasks.matching { it.name == "package${variantCap}" }
            .configureEach { finalizedBy(copyApkTask) }
    }
}