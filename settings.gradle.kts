rootProject.name = "compose-order-desk-demo"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Downloads a missing JDK (17 to build, 25 for the desktop app) instead of failing.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

// lagoon-compose comes from a checkout of the Lagoon theme repository next to this one, until it is published.
// Point elsewhere with -Plagoon.build=<path to its compose/ folder>.
val lagoonBuild = providers.gradleProperty("lagoon.build").orNull
    ?: listOf("../lagoon-theme/compose", "../swing-flatlaf-lagoon-theme/compose").firstOrNull { file(it).isDirectory }
if (lagoonBuild != null) includeBuild(lagoonBuild)

include(":shared", ":androidApp", ":desktopApp")
