import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library

plugins {
    // https://docs.gradle.org/current/userguide/java_gradle_plugin.html/
    // gradle-tooling-api and gradle-test-kit unavailable from regular repositories?
    // https://repo.gradle.org/ui/native/libs-releases-remote-cache/org/gradle/gradle-tooling-api/9.5.0
    // is present but does not contain expected classes such as org.gradle.api.Plugin.
    id("java-gradle-plugin")
    
    // https://docs.gradle.org/current/userguide/publishing_maven.html
    id("maven-publish")

    id("eclipse")
}

group = "uk.co.magictractor"
version = "0.0.1-SNAPSHOT"

// https://docs.gradle.org/current/userguide/publishing_maven.html
publishing {
    publications {
        withType<MavenPublication>().configureEach {
            pom {
                name = "${project.name}"
                description = "Create PDFs and other documents using a builder that abstracts use of Apache FOP."
                url = "https://github.com/magictractor/${project.name}"
                inceptionYear = "2026"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "kend"
                        name = "Ken Dobson"
                       // email = "me@gmail.com"
                    }
                }
                scm {
                    // connection = "scm:git:git:github.com/magictractor/${project.name}.git"
                    // developerConnection = "scm:git:ssh://github.com/magictractor/${project.name}.git"
                    url = "https://github.com/magictractor/${project.name}"
                }
            }
        }
    }

    repositories {
        //maven {
        //    name = "myRepo"
        //    url = layout.buildDirectory.dir("repo")
        //}
    }
}

// https://docs.gradle.org/current/userguide/declaring_repositories.html
repositories {
    mavenCentral()
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    //jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// https://docs.gradle.org/current/userguide/building_java_projects.html
// https://docs.gradle.org/current/userguide/java_plugin.html
// TODO! revisit withType() here - JavaCompile is not correct?
// tasks.withType<JavaCompile>().configureEach {
java {
//tasks.withType<JavaPlugin>().configureEach {
    // task: extension 'java'  class org.gradle.api.plugins.internal.DefaultJavaPluginExtension_Decorated
    //logger.lifecycle("task: " + this + "  " + this.javaClass)

    toolchain {
        // Gradle 9.0.0 code needs Java 17.
        // https://docs.gradle.org/9.0.0/release-notes.html#jvm-17
        //
        // Working with the new Class-File API (JEP 484) needs Java 24+.
        //
        // Projects using this may use toolchains with earlier versions.
        languageVersion = JavaLanguageVersion.of(25)
    }
    
    withSourcesJar()
    //withJavadocJar()
}

// :compileJava
tasks.withType<JavaCompile>().configureEach {
    // Include details about deprecated code in build/reports/problems/problems-report.html
    // options.compilerArgs.add("-Xlint:unchecked")
    options.setDeprecation(true)
}


// :jar
tasks.withType<Jar>().configureEach {
    destinationDirectory.set(file("$rootDir/jars"))
}

// :clean
tasks.withType<Delete>().configureEach {
    // Before doFirst for caching.
    // https://docs.gradle.org/9.5.0/userguide/configuration_cache_requirements.html#config_cache:requirements:disallowed_types
    val jarDir = File("$rootDir/jars")
     
    doFirst {
        val deleted = jarDir.deleteRecursively()
        if (deleted) {
            logger.lifecycle("jars deleted")
        } else {
            logger.warn("Failed to delete " + jarDir)
        }
    }
}

dependencies {
    testImplementation(libs.junit.jupiter);
    testRuntimeOnly(libs.junit.jupiter.platform);
    testImplementation(libs.assertj);
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
