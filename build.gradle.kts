plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

allprojects {
    group = "org.bettamind"
    version = "0.1.0-phase1"
}

tasks.register("phaseOneCheck") {
    group = "verification"
    description = "Runs the Phase 1 mobile checks that are available without Xcode."
    dependsOn(":shared:compileKotlinMetadata", ":shared:testDebugUnitTest", ":androidApp:assembleDebug")
}

tasks.register("phaseThreeCheck") {
    group = "verification"
    description = "Runs the Phase 3 Windows mobile checks available without Xcode."
    dependsOn("phaseOneCheck", ":androidApp:lintDebug")
}

tasks.register("phaseFiveCheck") {
    group = "verification"
    description = "Runs the Phase 5 Windows mobile checks available without Xcode."
    dependsOn("phaseThreeCheck", ":shared:testDebugUnitTest")
}

tasks.register("phaseSixCheck") {
    group = "verification"
    description = "Runs the Phase 6 Windows mobile checks available without Xcode."
    dependsOn("phaseFiveCheck", ":shared:testDebugUnitTest")
}

tasks.register("phaseSixFourCheck") {
    group = "verification"
    description = "Runs the Phase 6.4 Windows mobile checks available without Xcode."
    dependsOn("phaseSixCheck", ":shared:compileDebugKotlinAndroid", ":shared:testDebugUnitTest", ":androidApp:lintDebug")
}

tasks.register("phaseSixFiveCheck") {
    group = "verification"
    description = "Runs the Phase 6.5 Windows mobile checks available without Xcode."
    dependsOn("phaseSixFourCheck", ":shared:testDebugUnitTest")
}

tasks.register("phaseSixSixCheck") {
    group = "verification"
    description = "Runs the Phase 6.6 Windows mobile checks available without Xcode."
    dependsOn("phaseSixFiveCheck", ":shared:testDebugUnitTest", ":androidApp:lintDebug")
}
