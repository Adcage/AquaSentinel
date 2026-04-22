pluginManagement {
    repositories {
        // 1. 优先使用国内镜像插件源
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 2. 建议改为 PREFER_SETTINGS，防止和你之前的 init.gradle 冲突
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 3. 核心依赖镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 高德官方 Maven 仓库
        maven { url = uri("https://maven.amap.com/repository/public") }
        // 高德地图 SDK Maven 仓库
        maven { url = uri("https://packages.aliyun.com/maven/repository/2265988-release-Q8lKzR") }
        google()
        mavenCentral()
    }
}

rootProject.name = "android"
include(":app")
