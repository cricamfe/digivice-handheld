import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.cricamfe.digivice"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "dev.cricamfe.digivice"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Release key from Android/keystore.properties (ignored by git; see README.md). Without it the release build is
    // signed with the debug key, so it still installs over a debug build and the other way.
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.isFile) {
        val keystore = Properties().apply { keystoreFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = rootProject.file(keystore.getProperty("storeFile"))
            storePassword = keystore.getProperty("storePassword")
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            // R8 stays off: nothing here is shrunk or obfuscated, and the JS bridge methods must keep their names.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.webkit)
    implementation(project(":extractor"))
    implementation(libs.kotlinx.serialization.json)
}

/**
 * Puts the two things the app reads at run time into the assets of a build, copying them from where they live in the
 * repository so that nothing is duplicated in git:
 *  - `Frontend/dist-handheld` (the built page) as `www/`,
 *  - `Backend/Memory/Definitions` (the RAM addresses) as `definitions/`.
 *
 * `buildHandheld` checks types and rebuilds the page before this task can copy it.
 */
abstract class SyncDigiviceAssets : DefaultTask() {
    @get:Internal
    abstract val bundleDir: DirectoryProperty

    /** Watches the page's files; an absent folder is just an empty tree, so the task still runs and says what is wrong. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleFiles: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val definitionsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun sync() {
        val bundle = bundleDir.get().asFile
        if (!bundle.resolve("handheld.html").isFile) {
            throw GradleException(
                "The handheld build did not produce $bundle/handheld.html.\n" +
                    "Check the output of :app:buildHandheld.",
            )
        }
        fileSystem.sync {
            into(outputDir)
            from(bundle) { into("www") }
            from(definitionsDir) {
                into("definitions")
                // The partners' RAM addresses carry the original project's English name; nothing on Android reads it
                // (DigimonAddress.name), and the release ships no Digimon name: the app reads them from the disc.
                filesMatching("Parties/DigimonsAddresses.json") {
                    filter { line -> if (line.trimStart().startsWith("\"Name\":")) null else line }
                }
            }
        }
    }
}

val repositoryRoot: File = rootProject.projectDir.parentFile
val npmCommand = if (System.getProperty("os.name").startsWith("Windows")) {
    listOf("cmd", "/c", "npm")
} else {
    listOf("npm")
}

val buildHandheld = tasks.register<Exec>("buildHandheld") {
    group = "build"
    description = "Checks types and rebuilds the handheld interface before packaging."
    workingDir(File(repositoryRoot, "Frontend"))
    commandLine(npmCommand + listOf("run", "build:handheld"))
    // Always rebuild: imports and generated assets extend beyond the handheld entry point.
}

val testHandheld = tasks.register<Exec>("testHandheld") {
    group = "verification"
    description = "Runs the handheld interface tests."
    workingDir(File(repositoryRoot, "Frontend"))
    commandLine(npmCommand + listOf("run", "test:handheld"))
}

tasks.named("check") { dependsOn(testHandheld) }

extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val sync = tasks.register<SyncDigiviceAssets>("sync${name}DigiviceAssets") {
            dependsOn(buildHandheld)
            val bundle = File(repositoryRoot, "Frontend/dist-handheld")
            bundleDir.set(bundle)
            bundleFiles.from(fileTree(bundle))
            definitionsDir.set(File(repositoryRoot, "Backend/Memory/Definitions"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(sync, SyncDigiviceAssets::outputDir)
    }
}
