import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
        // @EncodeDefault marks the DTO fields the wire format always writes.
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    // GameLoop.start takes a CoroutineScope, so the app sees the type.
    api(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("reflect"))
    // Virtual time for the loop's scheduling tests.
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // The tests read the C# backend's definitions and the RAM snapshots from the repo; they are never copied here.
    systemProperty("digivice.repoRoot", rootProject.projectDir.parentFile.absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    // RAM dumps of the game stay out of the public repository: say so when the tests that need them are skipped.
    val ramDumps = listOf("Tools/MemoryScanner/Snapshots", "Tools/RetroArchProbe/thor-title.bin")
        .map { rootProject.projectDir.parentFile.resolve(it) }
    doFirst {
        val missing = ramDumps.filterNot { it.exists() }
        if (missing.isNotEmpty()) {
            logger.warn("RAM dumps not found, so the tests that read them are skipped: ${missing.joinToString { it.name }}")
        }
    }
}
