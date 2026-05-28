### `gradle.properties` files

##### Summary

* `magictractor` projects will not include a `gradle.properties` file.

* Gradle properties that are to be used across all projects should be configured in `~/.gradle/gradle.properties` 
(or another location specified by the `GRADLE_USER_HOME` environment variable).

* When a project is known to not work with a property value, this must be documented. It is not sufficient
to put a working value for the property in the project's `gradle.properties` file because that could be
overridden by a property in `~/.gradle/gradle.properties`. An example of this is creating a custom
plugin that does not work with caching.

* Care must be taken if using `GradleRunner` for testing. By default, it will not pick up `~/.gradle/gradle.properties`
because it uses a different Gradle user home. As a result the behaviour of the test can be different from what is observed from
the command line. The Gradle user home can be set using `GradleRunner.withTestKitDir()`.
Explicit properties may also be set using `GradleRunner.withArguments()` by including arguments such as 
`"-Dorg.gradle.configuration-cache=false"`.


##### Considerations

* A `gradle.properties` file external to `magictractor` projects will be required for managing
the JVMs used by Gradle `toolchain`s because my preferred option is to point to JVMs I already have
installed in non-standard locations. It is possible to configure Gradle to download JVMs as needed, or to
following

* Using `$GRADLE_HOME/gradle.properties` did not work. I was not using a proper Gradle installation,
just hijacking the environment variable to point to a directory containing a `gradle.properties` that would
have lower precedence than the project `gradle.properties`.
