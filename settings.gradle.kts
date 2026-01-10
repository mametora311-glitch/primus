// settings.gradle.kts
rootProject.name = "primus_project"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // ← モジュール側repositoriesを禁止
    repositories {
        google()
        mavenCentral()
        // 追加が要るときは「ここ」に書く（jitpack等）
        // maven("https://jitpack.io")
    }
}
rootProject.name = "primus_project"
include(":core_ai", ":primus2", ":primus-proxy")
