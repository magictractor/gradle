pluginManagement {
    plugins {  
        includeBuild("../..")
    }
}

plugins {   
    id("uk.co.magictractor.magictractor-settings-plugin")
}


rootProject.name = "magictractor-example"
rootProject.buildFileName = "example.gradle.kts"
