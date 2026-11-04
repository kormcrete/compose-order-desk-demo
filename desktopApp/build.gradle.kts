import java.nio.file.FileSystems
import java.nio.file.Files
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // Development runs that reload the UI on save (on the JetBrains Runtime 21); the packaged app is unaffected.
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    // Bytecode level. The app runs on the newer runtime below; its gains are runtime features.
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test"))
}

// Development runs open the window at once; the splash is for the packaged app.
tasks.withType<JavaExec>().configureEach {
    systemProperty("orderdesk.splash", "off")
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
    "-XX:-UsePerfData",
)

/**
 * ProGuard needs the JDK's classes as a library. JDK 25 builds come without jmods (a runtime can be linked
 * without them, JEP 493), so they come from the JDK 17 build toolchain; the app uses no newer API.
 */
val buildJdk = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
    .map { it.metadata.installationPath.asFile }
val jdkLibraryRules = layout.buildDirectory.file("proguard/jdk-library.pro")
val writeJdkLibraryRules by tasks.registering {
    description = "Writes the ProGuard rules that point at the JDK class library."
    // Locals, not the script's values: a task that refers to the script cannot go in the configuration cache.
    val jdkPath = buildJdk.map { it.absolutePath }
    val rulesFile = jdkLibraryRules
    inputs.property("jdk", jdkPath)
    outputs.file(rulesFile)
    doLast {
        val jmods = File(jdkPath.get()).resolve("jmods")
        val modules = listOf(
            "java.base", "java.desktop", "java.datatransfer", "java.prefs", "java.xml", "java.logging",
            "java.management", "java.naming", "jdk.management", "jdk.unsupported",
        )
        val rules = modules.map { jmods.resolve("$it.jmod") }.filter { it.exists() }.joinToString("\n") {
            "-libraryjars '${it.absolutePath.replace('\\', '/')}'(!**.jar;!module-info.class)"
        }
        rulesFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(rules + "\n")
        }
    }
}

/**
 * Skia's macOS jar holds both architectures. The packaging then takes this computer's library out of the jar and puts
 * it next to the jars, so the other one stays in the app and is never loaded: it goes here, before that.
 */
tasks.matching { it.name == "proguardReleaseJars" }.configureEach {
    val jars = layout.buildDirectory.dir("compose/tmp/main-release/proguard")
    val otherMacLibrary = if (System.getProperty("os.arch") == "aarch64") "libskiko-macos-x64" else "libskiko-macos-arm64"
    doLast {
        jars.get().asFile.listFiles { file -> file.extension == "jar" }.orEmpty().forEach { jar ->
            FileSystems.newFileSystem(jar.toPath()).use { zip ->
                Files.list(zip.getPath("/")).use { entries ->
                    entries.filter { it.fileName?.toString()?.startsWith(otherMacLibrary) == true }.forEach { Files.delete(it) }
                }
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.kormcrete.orderdesk.desktop.MainKt"
        javaHome = runtimeJdk.get()
        jvmArgs += desktopJvmArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // Next to the Swing app, which is "Kormcrete Order Desk" with the same icon.
            packageName = "Kormcrete Order Desk Compose"
            // The release workflow passes the tag's version. On macOS the first number cannot be 0.
            packageVersion = providers.gradleProperty("orderdesk.version").getOrElse("1.0.0")
            description = "Order Desk, a demo of the Lagoon theme for Compose Multiplatform"
            vendor = "Kormcrete"
            // A trimmed runtime: only the JDK modules the app uses.
            modules("java.desktop", "java.management", "jdk.management", "jdk.unsupported")
            macOS {
                // Not the Swing app's: macOS would take the two for one app.
                bundleID = "io.github.kormcrete.orderdesk.compose"
                iconFile.set(rootProject.file("packaging/icons/kormcrete-order-desk.icns"))
            }
            windows {
                iconFile.set(rootProject.file("packaging/icons/kormcrete-order-desk.ico"))
            }
            linux {
                iconFile.set(rootProject.file("packaging/icons/kormcrete-order-desk.png"))
            }
        }

        buildTypes.release.proguard {
            // 7.8 is the first ProGuard that reads Java 25 class files (the bundled runtime).
            version.set("7.8.2")
            configurationFiles.from(project.file("proguard-rules.pro"), writeJdkLibraryRules)
            obfuscate.set(false)
            optimize.set(true)
            // One jar instead of fifty: less to open at start, and smaller.
            joinOutputJars.set(true)
        }
    }
}

/**
 * Skia's native libraries for the computers the Java zip runs on. The macOS jar holds both Mac architectures, and the
 * other jars of these dependencies are the same classes as the app's.
 */
val desktopNatives: Configuration by configurations.creating {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
    }
}
dependencies {
    desktopNatives(compose.desktop.macos_arm64)
    desktopNatives(compose.desktop.windows_x64)
    desktopNatives(compose.desktop.linux_x64)
    desktopNatives(compose.desktop.linux_arm64)
}

/** The release build's jar without the build computer's Skia library: the Java zip has Skia for every computer. */
val javaZipJar by tasks.registering(Jar::class) {
    dependsOn("proguardReleaseJars")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/javaZip"))
    archiveFileName.set("order-desk.jar")
    from(layout.buildDirectory.dir("compose/tmp/main-release/proguard").map { dir -> fileTree(dir).files.map { zipTree(it) } })
    exclude("libskiko-*")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/** The Java download: the release build's jar, Skia for every computer, launchers. Runs on your own Java 17+. */
val packageJavaZip by tasks.registering(Zip::class) {
    description = "Builds kormcrete-order-desk-compose-java.zip."
    group = "distribution"
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    archiveFileName.set("kormcrete-order-desk-compose-java.zip")
    val top = "Kormcrete Order Desk Compose"
    into("$top/lib") {
        from(javaZipJar)
        from(desktopNatives) { include("skiko-awt-runtime-*") }
    }
    into(top) {
        from(rootProject.file("packaging/java/order-desk")) { filePermissions { unix("rwxr-xr-x") } }
        from(rootProject.file("packaging/java/order-desk.cmd"))
        from(rootProject.file("LICENSE"), rootProject.file("NOTICE"), rootProject.file("packaging/legal/INTER-OFL.txt"))
    }
}
