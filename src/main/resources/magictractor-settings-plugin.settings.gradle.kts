
// settings.project.gradle.kts is expected to exist
// and it must set rootProject.name
rootProject.name = "__undefined"
apply { from(file("${rootDir}/project.settings.gradle.kts")) }
if (rootProject.name == "__undefined") {
    // An explicit rootProject.name is best practice.
    // See https://docs.gradle.org/current/userguide/best_practices_general.html#name_your_root_project.
    throw GradleException("rootProject.name should be set in project.settings.gradle.kts")
}


// includeBuild "../{project}" entries may be added to
// settings.local.gradle to link to source in sibling projects
// instead of a jar file.
val localSettingsOld = file("${rootDir}/settings.project-local.gradle.kts")
if (localSettingsOld.exists()) {
    logger.warn("settings.project-local.gradle.kts should be renamed to project-local.settings.gradle.kts")
    apply { from(localSettingsOld) }
}
val localSettings = file("${rootDir}/project-local.settings.gradle.kts")
if (localSettings.exists()) {
    apply { from(localSettings) }
}


// Use non-standard build file name because usual build.gradle.kts is cumbersome when working with multiple projects.
rootProject.buildFileName = "${rootProject.name}.gradle.kts".replace("magictractor-", "")
