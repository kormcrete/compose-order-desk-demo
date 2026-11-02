import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // Bytecode level. The app runs on the newer runtime below; its gains are runtime features.
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

/** The JDK that runs and is bundled with the app: the latest LTS, found as -Porderdesk.jdk, a CI variable or a toolchain. */
val runtimeJdk: Provider<String> = providers.gradleProperty("orderdesk.jdk")
    .orElse(providers.environmentVariable("JAVA_HOME_25_X64"))
    .orElse(providers.environmentVariable("JAVA_HOME_25_arm64"))
    .orElse(providers.environmentVariable("JAVA_HOME_25_ARM64"))
    .orElse(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }.map { it.metadata.installationPath.asFile.absolutePath })

/**
 * JVM options for a small, quick desktop app (see README, "Desktop runtime"):
 * a serial collector and a capped heap that is given back when idle, compact object headers,
 * C1 only with a smaller code cache, smaller thread stacks, and the AOT cache when one is packaged.
 */
val desktopJvmArgs = listOf(
    "-XX:+UseSerialGC",
    "-Xms24m",
    "-Xmx384m",
    "-XX:MinHeapFreeRatio=10",
    "-XX:MaxHeapFreeRatio=30",
    "-XX:+UseCompactObjectHeaders",
    "-XX:+UseStringDeduplication",
    "-XX:TieredStopAtLevel=1",
    "-XX:ReservedCodeCacheSize=48m",
    "-Xss512k",
)

compose.desktop {
    application {
        mainClass = "io.github.kormcrete.orderdesk.desktop.MainKt"
        javaHome = runtimeJdk.get()
        jvmArgs += desktopJvmArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Order Desk"
            packageVersion = "1.0.0"
            description = "Order Desk, a demo of the Lagoon theme for Compose Multiplatform"
            vendor = "Kormcrete"
            // A trimmed runtime: only the JDK modules the app uses.
            modules("java.desktop", "java.management", "jdk.management", "jdk.unsupported")
            macOS {
                bundleID = "io.github.kormcrete.orderdesk"
            }
        }

        buildTypes.release.proguard {
            // 7.8 is the first ProGuard that reads Java 25 class files (the bundled runtime).
            version.set("7.8.2")
            configurationFiles.from(project.file("proguard-rules.pro"))
            obfuscate.set(false)
            optimize.set(true)
        }
    }
}
