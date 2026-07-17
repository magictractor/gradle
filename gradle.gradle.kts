import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library

plugins {
    // https://docs.gradle.org/current/userguide/java_gradle_plugin.html/
    // gradle-tooling-api and gradle-test-kit unavailable from regular repositories?
    // https://repo.gradle.org/ui/native/libs-releases-remote-cache/org/gradle/gradle-tooling-api/9.5.0
    // is present but does not contain expected classes such as org.gradle.api.Plugin.
    id("java-gradle-plugin")
    
    // java-gradle-plugin magictractor-plugin both apply java-library
    //id("uk.co.magictractor.magictractor-plugin") version "0.0.3"
    id("uk.co.magictractor.magictractor-plugin") version "0.0.1-SNAPSHOT"
    
    id("eclipse")
}


group = "uk.co.magictractor"
version = "0.0.1-SNAPSHOT"


magictractor {
    javaVersion = 17

    standardDependencies {
       // Gradle provides a logging API and a copy of many libraries.
       addLoggingDependencies = false;
    }

    pomInceptionYear = "2026"
}


gradlePlugin {
    plugins {
        register("magictractor-plugin") {
            id = "uk.co.magictractor.magictractor-plugin"
            implementationClass = "uk.co.magictractor.gradle.MagicTractorPlugin"
        }
        register("magictractor-settings-plugin") {
            id = "uk.co.magictractor.magictractor-settings-plugin"
            implementationClass = "uk.co.magictractor.gradle.MagicTractorSettingsPlugin"
        }
    }
}


java {
    // All test resources because .java files will be copied for compilation using GradleRunner
    // and they will not be packaged in the jar.
    sourceSets["test"].resources {
        srcDir("src/example/java")
        srcDir("src/example/test")
        srcDir("src/example/resources")
    }
}


dependencies {
    implementation("com.netflix.nebula:nebula-release-plugin:21.0.0")

    // Guava is used in tests to check that toString() implementations are consistent with Guava's ToStringHelper.
    testImplementation(libs.guava);
}


// https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseProject.html
// https://github.com/redhat-developer/vscode-java/issues/3311
// https://discuss.gradle.org/t/ad-hoc-jar-file-downloads-via-gradle/5989/5

// Use ALL to get source for Gradle classes.
//
// https://docs.gradle.org/current/userguide/gradle_wrapper.html#customizing_wrapper
tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.ALL
}

// Copy of the source files with a slightly different structure (no module directories).
//
// https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html
tasks.register<Jar>("packageGradleSource") {
    group = "IDE"
    description = "Packages Gradle source in a jar file for inclusion in IDE"

    val gradleHome = file("${project.gradle.gradleHomeDir}")
    val gradleSrcDir = File(gradleHome, "src")
    
    // Configure Jar task
    destinationDirectory = gradleHome
    archiveFileName = "__src.jar"
    from(gradleSrcDir.listFiles())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // TODO! log message if this looks like a BIN distribution
    onlyIf("Gradle source already exists") { !archiveFile.get().getAsFile().exists() }
    
   
    // Should be present if Gradle distribution type is ALL
    onlyIf("No Gradle source") { gradleSrcDir.exists() }
    if (!gradleSrcDir.exists()) {
        logger.warn("Gradle source directory not found in " + gradleSrcDir)
    }
    
    doFirst {
        // It'll take about a minute, so write a message before starting.
        logger.lifecycle("Packaging Gradle source...")
    }
    doLast {
        logger.lifecycle("Gradle source copied to " + archiveFile.get())
    }
}

// https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.plugins.ide.eclipse.model/-eclipse-classpath/index.html
eclipse {
    // Buildship runs autoBuildTasks, synchronizationTasks then autoBuildTasks again.
    // I have opted to use synchronizationTasks for source packaging because it is only triggered once. 
    synchronizationTasks("packageGradleSource")

    // TODO! too soon to be resolving this value - use doFirst or a global variable
    //val sourcePath = tasks.named<Jar>("packageGradleSource").get().archiveFile.get().asFile
    val sourcePath = File(File("${gradle.gradleHomeDir}"), "__src.jar")
    
    classpath.file {
        whenMerged(Action<Classpath> {
            entries.filterIsInstance<Library>().forEach { lib ->
                if (lib.path.contains("/generated-gradle-jars/")) {
                    if (lib.sourcePath != null) {
                        logger.warn("Expected source path for Gradle lib to be null before setting it, is " + lib.sourcePath)
                    }
                    lib.sourcePath = fileReference(sourcePath);
                    logger.debug("Source path for Gradle lib " + lib.path + " set to " + sourcePath)
                }
            }
        })
    }

}
