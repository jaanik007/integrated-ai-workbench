import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.jaanik.integratedaiworkbench"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))

    intellijPlatform {
        intellijIdea("2026.1.3")
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

sourceSets {
    main {
        kotlin.srcDirs(
            "base/frontend/src/main/kotlin",
            "base/shared/src/main/kotlin",
            "base/backend/src/main/kotlin",
        )
        resources.srcDirs("base/frontend/src/main/resources")
    }
    test {
        kotlin.srcDirs(
            "base/frontend/src/test/kotlin",
            "base/shared/src/test/kotlin",
            "base/backend/src/test/kotlin",
        )
    }
}

tasks.withType<Test>().configureEach {
    listOf(
        "iwb.realMcp",
        "iwb.realJudge",
        "iwb.runtime.executable",
        "iwb.runtime.model",
        "iwb.runtime.releaseTag",
        "iwb.runtime.artifact",
        "iwb.runtime.executableSha256",
    ).forEach { property ->
        providers.systemProperty(property).orNull?.let { systemProperty(property, it) }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }
}
