plugins {
    id("java-library")
    
    // https://docs.gradle.org/current/userguide/publishing_maven.html
    id("maven-publish")
    
    id("uk.co.magictractor.magictractor-plugin")
}

version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

// "libs.xxx" refers to libraries configured in version catalog in settings.gradle.
dependencies {
    // Logger API.
    implementation(libs.slf4j.api)
    // Logger implementation for unit tests.
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.platform)
    testImplementation(libs.assertj)
    
    //implementation(libs.guava)
}
