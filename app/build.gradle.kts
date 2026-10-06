import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
}

// 시크릿은 local.properties 에서 읽는다. 없으면 빈 값 (키 미설정 시에도 빌드는 되게)
// 템플릿은 local.properties.example, 실제 파일은 gitignore
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String = localProperties.getProperty(key).orEmpty()

// 업로드 키(Play 에 올릴 AAB 서명용). keystore.properties 와 키스토어 파일은 gitignore 다.
// 키가 없는 환경(다른 개발자·CI)에서도 빌드는 되게, 없으면 서명 설정을 만들지 않는다
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val uploadKeyStoreFile = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(false)
        txt.required.set(false)
    }
}

android {
    namespace = "com.dulpick.app"
    compileSdk = 36

    defaultConfig {
        // base + suffix → release com.dulpick.app, debug com.dulpick.debug
        applicationId = "com.dulpick"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["deepLinkScheme"] = "dulpick"

        buildConfigField("String", "API_BASE_URL", "\"${secret("API_BASE_URL")}\"")
    }

    signingConfigs {
        if (uploadKeyStoreFile != null) {
            create("upload") {
                storeFile = uploadKeyStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 배포 빌드와 나란히 설치되도록 id·스킴 분리
            applicationIdSuffix = ".debug"
            manifestPlaceholders["deepLinkScheme"] = "dulpickdebug"

            val kakaoKey = secret("KAKAO_NATIVE_APP_KEY_DEBUG")
            manifestPlaceholders["kakaoNativeAppKey"] = kakaoKey
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID_DEBUG")}\"")
        }
        release {
            applicationIdSuffix = ".app"
            // 업로드 키가 있으면 서명한다. 없으면 서명 안 된 AAB 가 나오고 Play 는 그걸 받지 않는다
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val kakaoKey = secret("KAKAO_NATIVE_APP_KEY_RELEASE")
            manifestPlaceholders["kakaoNativeAppKey"] = kakaoKey
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID_RELEASE")}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // 앱이 뜨는 순간 보이는 런치스크린 (API 31 이전에도 같은 모양으로)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 코루틴
    implementation(libs.kotlinx.coroutines.android)

    // 화면 이동
    implementation(libs.androidx.navigation.compose)

    // DI (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // 네트워크
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)

    // 보안 저장 (세션 토큰)
    implementation(libs.androidx.security.crypto)

    // 소셜 로그인
    implementation(libs.kakao.user)
    implementation(libs.androidx.credentials)

    // 카카오 지도
    implementation(libs.kakao.map)
    // 현재 위치 1회 조회 (FusedLocationProviderClient)
    implementation(libs.play.services.location)

    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
