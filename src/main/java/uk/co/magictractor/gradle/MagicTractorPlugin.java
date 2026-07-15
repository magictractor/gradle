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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import uk.co.magictractor.gradle.libs.ReconciledLibs;
import uk.co.magictractor.gradle.libs.ReconciledLibsBuilder;

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
// TODO! Fix availability of gradle source.
// https://discuss.gradle.org/t/eclipse-buildship-gradle-plugin-development-gradle-api-sources-missing/33461
// https://discuss.gradle.org/t/custom-plugins-dont-include-source/5651
public class MagicTractorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        MagicTractorExtension mte = project.getExtensions()
                .create("magictractor", DefaultMagicTractorExtension.class);

        configureDefaultPlugins(mte);
        configureRepositories(mte);
        configureGroup(mte);
        configureJavaPluginExtension(mte);
        configureJavaCompileTasks(mte);
        configureTestTasks(mte);
        configureReconciledLibraries(mte);
        configureDefaultDependencies(mte);
        configurePublishing(mte);

        project.afterEvaluate(p -> {
            //configureReconciledLibraries(mte);
        });
    }

    private void configureDefaultPlugins(MagicTractorExtension mte) {
        Project project = mte.getProject();

        // TODO! maybe "java" or "java-platform" for some projects??
        // and "java-gradle-plugin" when bootstrapping this project
        // https://docs.gradle.org/current/userguide/java_plugin.html#java_plugin
        // https://docs.gradle.org/current/userguide/java_library_plugin.html
        project.getPlugins().apply("java-library");
        // https://docs.gradle.org/current/userguide/publishing_maven.html
        project.getPlugins().apply("maven-publish");

        // dependies use apply(), not extended classes
        //
        // JavaLibraryPlugin applies JavaPlugin
        // JavaBasePlugin "compiles and tests Java source, and assembles it into a JAR file"
        // JavaGradlePluginPlugin applies JavaLibraryPlugin
        // JavaLibraryDistributionPlugin applies JavaLibraryPlugin and DistributionPlugin "package a Java project as a distribution including the JAR and runtime dependencies"
        // JavaPlatformPlugin distinct from "java" and "java-library", like Maven BOM
        // JavaPlugin applies JavaBasePlugin
        // JavaTestFixturesPlugin
    }

    private void configureJavaPluginExtension(MagicTractorExtension mte) {
        Project project = mte.getProject();

        JavaPluginExtension jpe = project.getExtensions()
                .findByType(JavaPluginExtension.class);

        Property<JavaLanguageVersion> languageVersionProperty = jpe.getToolchain()
                .getLanguageVersion();
        Provider<JavaLanguageVersion> languageVersion = mte.getJavaVersion()
                .orElse(project.provider(() -> {
                    throw new IllegalStateException("magictractor.javaVersion is required");
                }))
                .map(JavaLanguageVersion::of);

        languageVersionProperty.convention(languageVersion);

        // Would be nice if this gave a more specific error.
        // Currently "The value for property 'languageVersion' cannot be changed any further."
        // Maybe validate MagicTractorExtension?
        languageVersionProperty.disallowChanges();

        // Maven Central artifacts must have jars for source and Javadoc.
        // https://central.sonatype.org/publish/requirements/#supply-javadoc-and-sources
        jpe.withSourcesJar();
        // Javadoc causes build errors with missing params etc,
        // so we'll only want Javadoc for full releases, and maybe not on all projects.
        //jpe.withJavadocJar();
    }

    private void configureGroup(MagicTractorExtension mte) {
        mte.getProject().setGroup("uk.co.magictractor");
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
    private void configureRepositories(MagicTractorExtension mte) {
        // Unit test could run a task to list repos?
        // https://stackoverflow.com/questions/32143437/how-to-list-the-configured-repositories
        RepositoryHandler repositories = mte.getProject().getRepositories();
        repositories.mavenCentral();
        // Local maven used for snapshots of other magictractor projects.
        repositories.mavenLocal();
        // TODO! add repsy here?
    }

    private void configureReconciledLibraries(MagicTractorExtension mte) {
        ProjectInternal project = mte.getProject();

        ReconciledLibsBuilder reconciledLibsBuilder = new ReconciledLibsBuilder(project);
        ReconciledLibs reconciledLibs = reconciledLibsBuilder.build(mte.getJavaVersion());

        // TODO! allow the name of the reconciled libs extension to be configured in the magictractor block
        mte.getProject().getExtensions().add("reconciledLibs", reconciledLibs);

        if (mte.getProject().getExtensions().findByName("libs") == null) {
            mte.getProject().getExtensions().add("libs", reconciledLibs);
        }
        else {
            mte.getProject().getLogger().lifecycle("Reconciled libs not available in \"libs\"; only in \"reconciledLibs\".");
        }
    }

    /**
     * <p>
     * Create standard dependencies for logging and unit testing.
     * </p>
     * <p>
     * Previously this boilerplate was included in all projects.
     * </p>
     * <pre>
     *  dependencies {
     *      // Logger API.
     *      implementation(libs.slf4j.api)
     *      // Logger implementation for unit tests.
     *      runtimeOnly(libs.logback.classic)
     *
     *      testImplementation(libs.junit)
     *      testRuntimeOnly(libs.junit.platform)
     *      testImplementation(libs.assertj)
     *  }
     *  </pre>
     */
    private void configureDefaultDependencies(MagicTractorExtension mte) {
        Project project = mte.getProject();
        DependencyHandler dependencyHandler = project.getDependencies();
        ReconciledLibs reconciledLibs = project.getExtensions().findByType(ReconciledLibs.class);

        // Logging libs.
        addDependency(dependencyHandler, reconciledLibs, "implementation", "slf4j.api");
        addDependency(dependencyHandler, reconciledLibs, "runtimeOnly", "logback.classic");

        // Unit testing libs.
        addDependency(dependencyHandler, reconciledLibs, "testImplementation", "junit");
        addDependency(dependencyHandler, reconciledLibs, "testRuntimeOnly", "junit.platform");
        addDependency(dependencyHandler, reconciledLibs, "testImplementation", "assertj");
    }

    private void addDependency(DependencyHandler dependencyHandler,
            ReconciledLibs reconciledLibs,
            String configurationName,
            String normalisedAlias) {

        Provider<MinimalExternalModuleDependency> dependency = reconciledLibs.getDependency(normalisedAlias);
        dependencyHandler.add(configurationName, dependency);
    }

    private void configureJavaCompileTasks(MagicTractorExtension mte) {
        mte.getProject().getTasks().withType(JavaCompile.class, this::configureJavaCompileTask);
    }

    /**
     * Typically called twice, for {@code :javaCompile} and
     * {@code :javaTestCompile}.
     */
    private void configureJavaCompileTask(JavaCompile javaCompileTask) {
        javaCompileTask.getOptions().setDeprecation(true);
        javaCompileTask.getOptions().setEncoding("UTF-8");
    }

    private void configureTestTasks(MagicTractorExtension mte) {
        mte.getProject().getTasks().withType(Test.class, this::configureTestTask);
    }

    private void configureTestTask(Test testTask) {
        testTask.useJUnitPlatform();
    }

    // https://docs.gradle.org/current/userguide/publishing_maven.html
    private void configurePublishing(MagicTractorExtension mte) {
        PublishingExtension publishingExtension = mte.getProject().getExtensions().getByType(PublishingExtension.class);

        // TODO! repositories added should be dependent on whether or not this is a snapshot
        configurePublishingRepositories(publishingExtension.getRepositories(), mte.getProject());

        PublicationContainer publications = publishingExtension.getPublications();
        publications.configureEach(publication -> {
            MavenPom pom = ((MavenPublication) publication).getPom();
            configurePom(pom, mte);
        });

        // Wthout from() only the pom file would be created.
        // TODO! commented out temporarily to check whether still needed
        //publication.from(mte.getProject().getComponents().getByName("java"));
    }

    private void configurePom(MavenPom pom, MagicTractorExtension mte) {
        // Name used in the artifact. Libs should have "magictractor-" prefix.
        String projectName = mte.getProject().getName();
        // Name without "magictractor-" prefix used in Github.
        String shortProjectName = projectName.replace("magictractor-", "");
        String url = "https://github.com/magictractor/" + shortProjectName;

        //MagicTractorExtension mte = project.getExtensions().findByType(MagicTractorExtension.class);

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

    private void configurePublishingRepositories(RepositoryHandler repositoryHandler, Project project) {
        repositoryHandler.maven(repo -> {
            repo.setName("repsy");
            repo.setUrl(project.uri("https://repo.repsy.io/magictractor/maven"));
            PasswordCredentials credentials = repo.getCredentials();
            credentials.setUsername(project.getProviders().gradleProperty("repsyUsername").get());
            credentials.setPassword(project.getProviders().gradleProperty("repsyPassword").get());
        });
    }

}
