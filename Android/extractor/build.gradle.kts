import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

application { mainClass.set("dev.cricamfe.digivice.extractor.MainKt") }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("digivice.repositoryRoot", rootProject.projectDir.parentFile.absolutePath)
    inputs.property("DW3_ISO", providers.environmentVariable("DW3_ISO").orElse(""))
    inputs.property("DW3_BIN", providers.environmentVariable("DW3_BIN").orElse(""))
    inputs.property("DW3_REFERENCE", providers.environmentVariable("DW3_REFERENCE").orElse(""))
    testLogging { events("passed", "skipped", "failed") }
}
