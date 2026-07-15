pluginManagement {
    repositories {
        mavenCentral()
        // Repsy for boostrapping with an older release of this plugin.
        maven {
            url = uri("https://repo.repsy.io/magictractor/maven")
        }
        
        // temp - bootstrap with snapshot
        mavenLocal()
    }
}


rootProject.name = "magictractor-gradle"
rootProject.buildFileName = "gradle.gradle.kts"


dependencyResolutionManagement {
    versionCatalogs {
        // Use the local magictractorLibs.versions.toml rather than 
        // boostrapping from a previous release which might have older versions.
        create("magictractorLibs") {
            from(files("src/main/resources/gradle/magictractorLibs.versions.toml"))
        }
    }
}
