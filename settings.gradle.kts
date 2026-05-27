// Other magictractor projects use the plugins in this project
// to provide simpler and standardised build configurations.
//
// This project does not use its own plugins for the build
// so the build structure is not consistent with other 
// Magic Tractor projects.

// Delegate to the setting file used by magictractor-settings-plugin,
// so this is consistent with using the plugin.
// This applies settings.project.gradle.kts and setting.project-local.gradle.kts
// (the latter is optional and not likely to be useful in this project because
// there are no dependencies to use with includeBuild()). It also configures
// the version catalog.

pluginManagement {
    // gradlePluginPortal to get the Kotlin plugin
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    //id("magictractor-settings-plugin")
}

apply { from(file("${rootDir}/src/main/resources/magictractor-settings-plugin.settings.gradle.kts")) }
