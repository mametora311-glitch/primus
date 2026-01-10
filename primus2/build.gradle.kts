plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.company.primus2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.company.primus2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 既定（後段で上書き）
        buildConfigField("String", "PROXY_BASE_URL", "\"https://api.example.com\"")
        buildConfigField("String", "BASE_URL",       "\"https://api.example.com\"")
        buildConfigField("String", "SERVICE_TOKEN",  "\"REPLACE_AT_CI\"")
        buildConfigField("String", "VV_BASE_URL",    "\"https://voice.example.com\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // localhost 排除の接続フレーバ
    flavorDimensions += listOf("env", "endpoint")
    productFlavors {
        // 環境
        create("dev") {
            dimension = "env"
            buildConfigField("String", "SERVICE_TOKEN", "\"devtoken1\"")
            manifestPlaceholders["networkSecurityConfigRef"] = "@xml/network_security_config_dev"
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "SERVICE_TOKEN", "\"REPLACE_AT_CI\"")
            manifestPlaceholders["networkSecurityConfigRef"] = "@xml/network_security_config_prod"
        }

        // エンドポイント：エミュ / 実機ローカル
        create("avd") {
            dimension = "endpoint"
            buildConfigField("String", "PROXY_BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "BASE_URL",       "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "VV_BASE_URL",    "\"http://10.0.2.2:50022\"")
        }
        create("usb") {
            dimension = "endpoint"
            buildConfigField("String", "PROXY_BASE_URL", "\"http://127.0.0.1:8080\"")
            buildConfigField("String", "BASE_URL",       "\"http://127.0.0.1:8080\"")
            buildConfigField("String", "VV_BASE_URL",    "\"http://127.0.0.1:50022\"")
        }
        // ★ Cloud Run 用エンドポイント
        create("cloud") {
            dimension = "endpoint"
            buildConfigField(
                "String",
                "PROXY_BASE_URL",
                "\"https://primus-proxy-1043986973502.asia-northeast1.run.app\""
            )
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://primus-proxy-1043986973502.asia-northeast1.run.app\""
            )
            // Voice はまだ未接続なので空
            buildConfigField("String", "VV_BASE_URL", "\"\"")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "PROXY_BASE_URL", "\"\"")
            buildConfigField("String", "BASE_URL",       "\"\"")
            buildConfigField("String", "VV_BASE_URL",    "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/** Room スキーマ（KSP） */
abstract class PrepareRoomSchemas : DefaultTask() {
    @get:OutputDirectory
    abstract val out: DirectoryProperty

    @TaskAction
    fun touch() {
        out.get().asFile.mkdirs()
    }
}

val roomSchemasDir = layout.projectDirectory.dir("schemas")

tasks.register<PrepareRoomSchemas>("prepareRoomSchemas") {
    out.set(roomSchemasDir)
}

ksp {
    arg("room.schemaLocation", roomSchemasDir.asFile.absolutePath)
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn("prepareRoomSchemas")
}

dependencies {
    implementation(project(":core_ai"))
    implementation("androidx.datastore:datastore-core-android:1.1.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    val ktorBom    = platform("io.ktor:ktor-bom:2.3.12")
    val roomVer    = "2.6.1"

    implementation(composeBom)
    androidTestImplementation(composeBom)

    // AndroidX / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Material（XML テーマ解決用）← これがないと今のエラーになる
    implementation("com.google.android.material:material:1.12.0")

    // Ktor（BOM）
    implementation(ktorBom)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // KotlinX / Network
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room（KSP）
    implementation("androidx.room:room-runtime:$roomVer")
    implementation("androidx.room:room-ktx:$roomVer")
    ksp("androidx.room:room-compiler:$roomVer")

    // Logging / Test
    debugImplementation("com.jakewharton.timber:timber:5.0.1")
    releaseImplementation("org.slf4j:slf4j-nop:2.0.13")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
}

configurations.configureEach {
    exclude(group = "com.android.support")
}
