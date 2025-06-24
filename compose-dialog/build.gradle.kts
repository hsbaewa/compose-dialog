import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)

    // Compose
    alias(libs.plugins.compose.compiler)

    // Maven publish
    alias(libs.plugins.maven.publish)
    id("signing") // GPG 서명을 위한 플러그인 추가
}

// Maven 그룹 및 버전 설정
group = "io.github.hsbaewa"
version = "0.0.1"

tasks.withType(Javadoc::class) {
    options {
        encoding = "UTF-8"
    }
}

signing {
    sign(publishing.publications)
    useGpgCmd() // 이거 있으면 signAllPublications() 필요 없음.
}

mavenPublishing {
//    signAllPublications() // Gpg 서명을 위한 설정
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL) // 포탈로 등록 할거기 때문에 타입 추가

    coordinates("io.github.hsbaewa", "compose-dialog", "0.0.1") // 네임 스페이스, 라이브러리 이름, 버전 순서로 작성

    // POM 설정
    pom {
        /**
        name = '[라이브러리 이름]'
        description = '[라이브러리 설명]'
        url = '[오픈소스 Repository Url]'
         */
        name = "compose-dialog"
        description = "Compose Dialog Library"
        url = "https://github.com/hsbaewa/compose-dialog"
        inceptionYear = "2025"

        // 라이선스 정보
        licenses {
            license {
                name = "Apache License"
                url = "https://github.com/hsbaewa/compose-dialog/blob/main/LICENSE"
            }
        }

        // 개발자 정보
        developers {
            developer {
                id = "hsbaewa"
                name = "Development guy"
                email = "hsbaewa@gmail.com"
            }
            // 다른 개발자 정보 추가 가능...
        }

        /**
        connection = 'scm:git:github.com/[Github 사용자명]/[오픈소스 Repository 이름].git'
        developerConnection = 'scm:git:ssh://github.com/[Github 사용자명]/[오픈소스 Repository 이름].git'
        url = '<https://github.com/>[Github 사용자명]/[오픈소스 Repository 이름]/tree/[배포 브랜치명]'
         */
        scm {
            connection = "scm:git:github.com/hsbaewa/compose-dialog.git"
            developerConnection = "scm:git:ssh://github.com:hsbaewa/compose-dialog.git"
            url = "https://github.com/hsbaewa/compose-dialog/tree/main"
        }
    }
}

android {
    namespace = "kr.co.hs.compose.dialog"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0"
    }
}

dependencies {

    /**
     * [compose start]
     */
    val composeBom = platform("androidx.compose:compose-bom:2024.08.00")
    implementation(composeBom)

    // Material Design 3
    implementation(libs.androidx.material3)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.runtime.livedata)

    // system ui
    implementation(libs.accompanist.systemuicontroller)

    // NavigationSuiteScaffold
    implementation(libs.androidx.compose.material3.navigationSuite)

    // navController
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.accompanist.webview)
    /**
     * [compose end]
     */
}