/**
 * Copyright 2026 Ken Dobson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.magictractor.gradle;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;

/**
 * <p>
 * Gradle plugin used to avoid boilerplate configuration being copied between
 * all magictractor.co.uk projects.
 * </p>
 * <p>
 * Referring to other third-party gradle plugins can be helpful during
 * development:
 * <ul>
 * <li>{@code gradle-git-version} provides a {@code gitVersion()} function that
 * can be used in gradle scripts to provide a version number based on the state
 * of the git repository. See
 * https://github.com/palantir/gradle-git-version/blob/5.0.0/src/main/java/com/palantir/gradle/gitversion/GitVersionPlugin.java</li>
 * </ul>
 *
 * @see https://discuss.gradle.org/t/apply-from-vs-apply-plugin-aka-build-logic-reuse/31922/2
 */
// jars dierectory not ported because jars are available by default in build/libs (now visible in my Eclipse)
//
//@NonNullApi
//public class ExamplePlugin implements Plugin<Project> {
//
//    private final String REPO = "file:///tmp/somewhere";
//
//    @Override
//    public void apply(final Project project) {
//        final Logger logger = project.getLogger();
//        final ExtensionContainer ext = project.getExtensions();
//        final PluginContainer plugins = project.getPlugins();
//        logger.info("applying java plugin");
//        plugins.apply(JavaPlugin.class);
//        logger.info("download dependencies from maven central");
//        project.getRepositories().mavenCentral();
//        logger.info("all java compile tasks will report all warnings");
//        project.getTasks().withType(JavaCompile.class).configureEach(t -> t.getOptions().getCompilerArgs().add("-Xlint:all"));
//        logger.info("tests will be able to use assertj lib");
//        project.getDependencies().add("testImplementation", "org.assertj:assertj-core:3.12.2");
//        logger.info("maven publish plugin");
//        plugins.apply(MavenPublishPlugin.class);
//        logger.info("configuring publishing repository to be a maven type hosted at {}", REPO);
//        ext.getByType(PublishingExtension.class).getRepositories().maven(m -> m.setUrl(REPO));
//        logger.info("adding an end-user dsl to apply preferences");
//        ext.create("myPlugin", ExamplePluginDsl.class, project);
//    }
//}
//
// TODO! Fix availability of gradle source.
// https://discuss.gradle.org/t/eclipse-buildship-gradle-plugin-development-gradle-api-sources-missing/33461
// https://discuss.gradle.org/t/custom-plugins-dont-include-source/5651
//
// https://discuss.gradle.org/t/custom-plugins-how-to-avoid-using-afterevaluate-when-setting-another-plugins-extension-configuration/42314/3
public class MagicTractorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.setGroup("magictractor.co.uk");

        // temp
        // project.getLogger().lifecycle("tasks.size(): " + project.getTasks().size());
        // project.getTasks().forEach(task -> project.getLogger().lifecycle("task " + task));

        project.getExtensions().create("magictractor", DefaultMagicTractorExtension.class, project);

        applyConfigurations(project);

        // Avoid afterEvaluate()?
        // https://discuss.gradle.org/t/is-project-afterevaluate-the-proper-way-for-gradle-plugin-to-dynamically-create-default-tasks/31349
        project.afterEvaluate(this::afterEvaluateConfigurations);
    }

    private void applyConfigurations(Project project) {
        configureRepositories(project);
    }

    private void afterEvaluateConfigurations(Project project) {
        project.getLogger().lifecycle("Test task count : " + project.getTasks().withType(Test.class).size());

        project.getTasks().withType(JavaCompile.class).forEach(this::configureJavaCompileTask);
        project.getTasks().withType(Test.class).forEach(this::configureTestTask);
        project.getTasks().withType(Jar.class).forEach(this::configureJarTask);
        project.getTasks().withType(Delete.class).forEach(this::configureDeleteTask);

        //        JavaPluginExtension javaExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        //        if (javaExtension != null) {
        //            configureJavaExtension(javaExtension);
        //        }
        configureOptionalExtension(project, JavaPluginExtension.class, this::configureJavaPluginExtension);
        configureOptionalExtension(project, PublishingExtension.class, this::configurePublishingExtension);
    }

    private <EXTENSION> void configureOptionalExtension(Project project, Class<EXTENSION> extensionType, Consumer<EXTENSION> extensionConfiguration) {
        EXTENSION extension = project.getExtensions().findByType(extensionType);
        if (extension != null) {
            extensionConfiguration.accept(extension);
        }
    }

    private <EXTENSION> void configureOptionalExtension(Project project, Class<EXTENSION> extensionType, BiConsumer<Project, EXTENSION> extensionConfiguration) {
        EXTENSION extension = project.getExtensions().findByType(extensionType);
        if (extension != null) {
            extensionConfiguration.accept(project, extension);
        }
    }

    /**
     * Was <pre>
     * repositories {
     *     mavenCentral()
     *     mavenLocal()
     * }
     * </pre> Projects might need to include a {@code repositories} section for
     * non-standard repositories.
     */
    //  https://docs.gradle.org/current/userguide/declaring_repositories.html
    private void configureRepositories(Project project) {
        // Unit test could run a task to list repos?
        // https://stackoverflow.com/questions/32143437/how-to-list-the-configured-repositories
        RepositoryHandler repositories = project.getRepositories();
        repositories.mavenCentral();
        // Local maven used for other magictractor projects.
        repositories.mavenLocal();
    }

    /**
     * Typically called twice, for {@code :javaCompile} and
     * {@code :javaTestCompile}.
     */
    private void configureJavaCompileTask(JavaCompile javaCompileTask) {
        javaCompileTask.getOptions().setDeprecation(true);
        javaCompileTask.getOptions().setEncoding("UTF-8");
    }

    private void configureTestTask(Test testTask) {
        testTask.useJUnitPlatform();

        testTask.getLogger().lifecycle("configured Test");
    }

    /**
     * <p>
     * Set up a directory that will contain a copy of generated jar files. The
     * directory and jra files will be removed by the {@code clean} task.
     * </p>
     * <p>
     * Was
     * </p>
     * <pre>
     * tasks.withType<Jar>().configureEach {
     *     destinationDirectory.set(file("$rootDir/jars"))
     * }
     * </pre>
     */
    private void configureJarTask(Jar jarTask) {
        jarTask.getLogger().lifecycle("Default jar directory: " + jarTask.getDestinationDirectory().get());
        //jarTask.getDestinationDirectory().

        //      project.getLogger().lifecycle("configured Test");
    }

    /**
     * <p>
     * Remove the directory that contains a copy of generated jar files.
     * </p>
     * <p>
     * Was
     * </p>
     * <pre>
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
     * </pre>
     */
    private void configureDeleteTask(Delete deleteTask) {
        // deleteTask.des

        //project.getLogger().lifecycle("delete config");
        // File jarDir = new File(project.getRootDir(), "jars");

        File jarDir = new File(deleteTask.getProject().getRootDir(), "jars");

        deleteTask.doFirst(task -> {

            //task.getProject().getRootDir()
            // Hmm. Does Gradle rovide a wrapper that gives deleteRecursively?
            // jarDir.d
            task.getLogger().lifecycle("delete " + jarDir);

            //Files.deleteIfExists(null);
        });
    }

    //    java {
    //        // task: extension 'java'  class org.gradle.api.plugins.internal.DefaultJavaPluginExtension_Decorated
    //        //logger.lifecycle("task: " + this + "  " + this.javaClass)
    //
    //        toolchain {
    //            languageVersion = JavaLanguageVersion.of(8)
    //        }
    //
    //        withSourcesJar()
    //        //withJavadocJar()
    //    }
    //
    // could use $ ./gradlew javaToolchains for testing?
    private void configureJavaPluginExtension(JavaPluginExtension javaExtension) {
        // Setting the toolchain language version remains in the build file
        // because Buildship does not detect the correct version if it is done here.
        //        javaExtension.toolchain(toolchain -> {
        //            toolchain.getLanguageVersion().convention(JavaLanguageVersion.of(8));
        //            project.getLogger().lifecycle("toolchain: " + toolchain);
        //        });

        javaExtension.withSourcesJar();
    }

    // https://docs.gradle.org/current/userguide/publishing_maven.html
    private void configurePublishingExtension(Project project, PublishingExtension publishingExtension) {
        publishingExtension.publications(publications -> configurePublications(project, publications));
    }

    private void configurePublications(Project project, PublicationContainer publications) {
        MavenPublication maven = publications.create("mavenJava", MavenPublication.class);

        // Wthout from() only the pom file would be created.
        // How to get the SoftwareCompontent matching "from components.java"?
        // Looks like project.getComponents().getByName("java");
        maven.from(project.getComponents().getByName("java"));

        maven.pom(pom -> configurePom(project, pom));

        //publications.add(pom);
    }

    private void configurePom(Project project, MavenPom pom) {
        // Name used in the artifact. Libs should have "magictractor-" prefix.
        String projectName = project.getName();
        // Name without "magictractor-" prefix used in Github.
        String shortProjectName = projectName.replace("magictractor-", "");
        String url = "https://github.com/magictractor/" + shortProjectName;

        MagicTractorExtension mte = project.getExtensions().findByType(MagicTractorExtension.class);

        pom.getName().set(shortProjectName);
        pom.getDescription().set(mte.getPomDescription());
        pom.getUrl().set(url);
        pom.getInceptionYear().set(mte.getPomInceptionYear());

        pom.licenses(licenses -> licenses.license(license -> {
            license.getName().set("Apache License, Version 2.0");
            license.getUrl().set("http://www.apache.org/licenses/LICENSE-2.0");
        }));

        pom.developers(devs -> devs.developer(dev -> {
            dev.getId().set("kend");
            dev.getName().set("Ken D");
        }));

        pom.scm(scm -> {
            scm.getUrl().set(url);
        });
    }

}
