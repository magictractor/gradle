// Other magictractor projects use the plugins in this project
// to provide simpler and standardised build configurations.
//
// This project does not use its own plugins for the build
// so the build structure is not consistent with other 
// Magic Tractor projects.

rootProject.name = "magictractor-gradle"
rootProject.buildFileName = "gradle.gradle.kts"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("src/main/resources/gradle/magictractor.versions.toml"))
        }
    }
}
