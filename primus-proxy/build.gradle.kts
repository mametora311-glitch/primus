plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

group = "com.company"
version = "1.0.0"

application {
    // Main.kt のパッケージに合わせる
    mainClass.set("com.company.primusproxy.MainKt")
}

dependencies {
    val ktorVersion = "2.3.12"

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    // Ktor JSON
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor HTTP client（今回の OpenAI 呼び出しは HttpURLConnection だから最低限でいいが、保険で残す）
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // JSON シリアライザ
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ログ
    implementation("ch.qos.logback:logback-classic:1.5.8")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// shadowJar で Main-Class が埋まるようにしておく
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("primus-proxy")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.company.primusproxy.MainKt"
    }
}
