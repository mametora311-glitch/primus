// Project-level build.gradle.kts  ← フル置換（各プラグインの version を明示）
plugins {
    id("com.android.application") version "8.12.1" apply false
    id("com.android.library")    version "8.12.1" apply false

    id("org.jetbrains.kotlin.android")            version "1.9.24" apply false
    id("org.jetbrains.kotlin.jvm")                version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false

    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false

    id("com.google.gms.google-services") version "4.4.2" apply false
}
// 版の整合：Gradle = 8.1.x（wrapperは 8.1.4 推奨）、JBR = 17 に固定でOK
