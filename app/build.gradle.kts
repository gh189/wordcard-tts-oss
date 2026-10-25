import com.android.build.api.variant.impl.VariantOutputImpl
import java.util.Properties

plugins {
    id("com.android.application")
}

// Google Play 上传密钥，keystore.properties 不进 git；没有这个文件时 release 不签名
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.gh189.wordcard.tts"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.gh189.wordcard.tts"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        // 离线发音的原生库较大，只保留真机和模拟器常用架构
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("upload") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    // 模型不压缩，加载时可直接读取
    androidResources {
        noCompress += listOf("onnx", "bin")
        // 生成 localeConfig，Android 13 起可在系统设置里单独选择应用语言
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? VariantOutputImpl)?.outputFileName?.set(
                output.versionName.map { "wordcard-tts-$it.apk" }
            )
        }
    }
}

dependencies {
    // sherpa-onnx 离线语音合成，AAR 来自 https://github.com/k2-fsa/sherpa-onnx/releases
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    testImplementation("junit:junit:4.13.2")
}
