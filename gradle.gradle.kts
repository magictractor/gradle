import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath
import org.gradle.plugins.ide.eclipse.GenerateEclipseProject
//import org.gradle.plugins.ide.eclipse.model.*

plugins {
    // https://docs.gradle.org/current/userguide/java_gradle_plugin.html
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
        create<MavenPublication>("maven") {
            from(components["java"])
        
            pom {
                name = "${project.name.replaceFirstChar(Char::titlecase)}"
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
        languageVersion = JavaLanguageVersion.of(8)
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


// This creates a new minimal project in the Maven repository that has a dependency on this project.
gradlePlugin {
    plugins {
        register("magictractor-project-plugin") {
            id = "uk.co.magictractor.magictractor-project-plugin"
            implementationClass = "uk.co.magictractor.gradle.MagicTractorProjectPlugin"
        }
    }
}

// https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseProject.html
// https://github.com/redhat-developer/vscode-java/issues/3311
// https://discuss.gradle.org/t/ad-hoc-jar-file-downloads-via-gradle/5989/5

// Use ALL to get source for Gradle classes.
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
eclipse.classpath.file {
    whenMerged(Action<Classpath> {
        val sourcePath = fileReference(File(file("${project.gradle.gradleHomeDir}"), "__src.jar"))
        entries.filterIsInstance<Library>().forEach {
            lib ->
            if (lib.path.contains("/generated-gradle-jars/")) {
                if (lib.sourcePath != null) {
                    logger.warn("Expected source path for Gradle lib to be null before setting it")
                }
                lib.sourcePath = sourcePath;
                logger.debug("Source path for Gradle lib " + lib.path + " set to " + sourcePath.file)
            }
        }
    })
}

eclipse {
    // dependencyTasks might be more appropriate, but I can't find documentation
    // TODO! add a debug task for both auto build and dependency
    autoBuildTasks( "packageGradleSource" )
}
