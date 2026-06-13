import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

val sqlCipherIosVersion = "4.16.0"
val sqlCipherIosChecksum = "510fd00fa51fb017909a159bb1cc233b012e8ce18dc9c2f09014fe47f557c1a6"

abstract class PrepareSqlCipherIosTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val checksum: Property<String>

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val outputRoot = outputDirectory.get().asFile
        val archiveFile = outputRoot.resolve("SQLCipher.xcframework.zip")
        archiveFile.parentFile.mkdirs()
        if (!archiveFile.exists() || archiveFile.sha256() != checksum.get()) {
            URI(downloadUrl.get()).toURL().openStream().use { input ->
                archiveFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        check(archiveFile.sha256() == checksum.get()) {
            "Downloaded SQLCipher iOS XCFramework checksum did not match the pinned value."
        }

        val marker = outputRoot.resolve("SQLCipher.xcframework/Info.plist")
        if (!marker.exists()) {
            unzip(archiveFile, outputRoot)
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun unzip(archive: File, outputRoot: File) {
        ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val output = outputRoot.resolve(entry.name).canonicalFile
                check(output.path.startsWith(outputRoot.canonicalPath)) {
                    "Blocked invalid SQLCipher archive path ${entry.name}."
                }
                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    output.parentFile.mkdirs()
                    output.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }
}

val prepareSqlCipherIos by tasks.registering(PrepareSqlCipherIosTask::class) {
    group = "verification"
    description = "Downloads and verifies the pinned SQLCipher iOS XCFramework for Kotlin/Native linking."

    version.set(sqlCipherIosVersion)
    checksum.set(sqlCipherIosChecksum)
    downloadUrl.set("https://github.com/sqlcipher/SQLCipher.swift/releases/download/$sqlCipherIosVersion/SQLCipher.xcframework.zip")
    outputDirectory.set(layout.buildDirectory.dir("sqlcipher"))
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        val sqlCipherRoot = layout.buildDirectory.get().asFile
            .resolve("sqlcipher/SQLCipher.xcframework")
        val sqlCipherSlice = when (konanTarget.name) {
            "ios_arm64" -> "ios-arm64"
            "ios_x64", "ios_simulator_arm64" -> "ios-arm64_x86_64-simulator"
            else -> error("Unsupported SQLCipher iOS target ${konanTarget.name}.")
        }
        val sqlCipherFrameworkSearchPath = sqlCipherRoot.resolve(sqlCipherSlice).absolutePath
        val sqlCipherHeadersPath = sqlCipherRoot
            .resolve("ios-arm64_x86_64-simulator/SQLCipher.framework/Headers")
            .absolutePath

        compilations.getByName("main") {
            cinterops.create("BettamindSqlCipher") {
                defFile(project.file("src/nativeInterop/cinterop/BettamindSqlCipher.def"))
                compilerOpts("-I$sqlCipherHeadersPath", "-DSQLITE_HAS_CODEC")
            }
        }

        binaries.configureEach {
            linkerOpts(
                "-F$sqlCipherFrameworkSearchPath",
                "-framework",
                "SQLCipher",
                "-rpath",
                sqlCipherFrameworkSearchPath,
            )
        }

        binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "dev.bettamind.placeholder.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.sqlite)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqlcipher.android)
        }
    }
}

android {
    namespace = "org.bettamind.shared"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.bettamind.shared.generated.resources"
}

tasks.matching { task ->
    task.name != "prepareSqlCipherIos" &&
        (
            task.name.contains("BettamindSqlCipher") ||
                task.name.contains("Ios", ignoreCase = true) ||
                task.name.contains("Apple", ignoreCase = true)
        )
}.configureEach {
    dependsOn(prepareSqlCipherIos)
}
