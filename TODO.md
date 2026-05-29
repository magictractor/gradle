### TODO

A TODO list for this project. In no particular order. These may never happen.

* Switch to IntelliJ. Something I have been thinking about for a long time. Kotlin support there
is much better than in Eclipse. Eclipse support appears dead https://github.com/Kotlin/kotlin-eclipse.

* Switch my Gradle Kotlin scripts to use vanilla Kotlin without Gradle's syntactic sugar. Likely to be clearer
for me (a Kotlin novice). Maybe faster too. If importing Gradle rather than using gradleApi() can maybe remove
the hackery to get the Gradle source included in Eclipse.
https://mbonnin.net/2025-07-10_the_case_against_kotlin_dsl/

* Change licence file header to use "Copyright the original authors..." text used by some libs (all projects).

* Change pom code to check licence in pom against the LICENCE file in the top level of the project (created by Github).

* Put the version catalog into a yaml file. (Maybe not, might want logic depending on how to handle different dependency versions
related to Java version, e.g. Mockito capped at 4.x.x if using Java 8. See below.)

* Change version catalog (or use thereof) to provide different versions depending on the Java version. For example,
Mockito capped at 4.x.x for Java 8. Jackson too, 3.0.0 needs Java 17. 

* How to configure org.gradle.java.installations.paths? Maybe using a gradle.properties in GRADLE_USER_HOME or GRADLE_HOME?
https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties

* Figure out why sometimes incompatible Gradle daemons are running.

* Try using Develocity or similar scanning tools.

* Do training courses. https://dpeuniversity.gradle.com/app

* Create a helper class for setting up GradleRunner (GradleRunner.withTestKitDir(), see properties.md).

* There is a danger that ~/.gradle/gradle.properties could be lost by purging the .gradle directory. How to mitigate? (see properties.md)
