pluginManagement {
  plugins {
    //id("uk.co.magictractor.magictractor-settings-plugin") // version "0.0.1-SNAPSHOT"
    //id("uk.co.magictractor.magictractor-plugin") // version "0.0.1-SNAPSHOT"
    
    includeBuild("../../../../gradle")
  }
}

plugins {   
    id("uk.co.magictractor.magictractor-settings-plugin")
}
