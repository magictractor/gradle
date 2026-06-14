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

import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import uk.co.magictractor.gradle.accessors.MapAccessor_Template;
import uk.co.magictractor.gradle.libs.ReconciledLibrariesBuilder;

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
        // System.out.println("Settings[p]: " + MagicTractorSettingsPlugin.getSettings(project.getGradle()));

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
        configurePublishingExtension(mte);
    }

    private void configureDefaultPlugins(MagicTractorExtension mte) {
        Project project = mte.getProject();

        // TODO! maybe "java" or "java-platform" for some projects??
        project.getPlugins().apply("java-library");
        // https://docs.gradle.org/current/userguide/publishing_maven.html
        project.getPlugins().apply("maven-publish");
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

        jpe.withSourcesJar();
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
        // Local maven used for other magictractor projects.
        repositories.mavenLocal();
    }

    private void configureReconciledLibraries(MagicTractorExtension mte) {
        MinimalExternalModuleDependency dep;
        //dep.ver

        // org.gradle.api.internal.catalog.DefaultVersionCatalogBuilder_Decorated@3fe743b
        // org.gradle.internal.extensibility.DefaultExtraPropertiesExtension@3e29172d  DefaultExtraPropertiesExtension
        mte.getProject().getExtensions().configure(Object.class, ext -> {
            System.out.println(ext + "  " + ext.getClass().getSimpleName());
        });
        mte.getProject().getAllTasks(true).entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
        });

        //  implementation(libs3) expected to fail, but exception is interesting
        //  ah - it's simple - Map is for one dependency, should have keys "group", "name" and "version" (has none of those")
        // visitor.candidate("Maps").example("[group: 'org.gradle', name: 'gradle-core', version: '1.0']");
        //
        //        * What went wrong:
        //            Could not create an instance of type org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency.
        //            > Multiple constructors for parameters [null, null, null]:
        //                1. candidate: DefaultExternalModuleDependency(String, String, String)
        //                2. best match: DefaultExternalModuleDependency(ModuleIdentifier, MutableVersionConstraint, String)
        //
        //        org.gradle.api.reflect.ObjectInstantiationException: Could not create an instance of type org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency.
        //        at org.gradle.internal.instantiation.generator.DependencyInjectingInstantiator.doCreate(DependencyInjectingInstantiator.java:67)
        //        at org.gradle.internal.instantiation.generator.DependencyInjectingInstantiator.newInstance(DependencyInjectingInstantiator.java:53)
        //        at org.gradle.api.internal.notations.DependencyMapNotationConverter.parseMap(DependencyMapNotationConverter.java:76)
        //        at org.gradle.internal.typeconversion.MapNotationConverter.parseType(MapNotationConverter.java:94)
        //        at org.gradle.internal.typeconversion.MapNotationConverter.parseType(MapNotationConverter.java:41)
        //        at org.gradle.internal.typeconversion.TypedNotationConverter.convert(TypedNotationConverter.java:43)
        //        at org.gradle.internal.typeconversion.CompositeNotationConverter.convert(CompositeNotationConverter.java:34)
        //        at org.gradle.internal.typeconversion.NotationConverterToNotationParserAdapter.parseNotation(NotationConverterToNotationParserAdapter.java:31)
        //        at org.gradle.internal.typeconversion.ErrorHandlingNotationParser.parseNotation(ErrorHandlingNotationParser.java:48)
        //        at org.gradle.api.internal.artifacts.DefaultDependencyFactory.createDependency(DefaultDependencyFactory.java:77)
        //        at org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.create(DefaultDependencyHandler.java:166)
        //        at org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.doAddRegularDependency(DefaultDependencyHandler.java:195)
        //        at org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.doAdd(DefaultDependencyHandler.java:190)
        //        at org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.add(DefaultDependencyHandler.java:118)
        //        at org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.add(DefaultDependencyHandler.java:112)
        //        at org.gradle.kotlin.dsl.support.delegates.DependencyHandlerDelegate.add(DependencyHandlerDelegate.kt:54)
        //        at org.gradle.kotlin.dsl.ImplementationConfigurationAccessorsKt.implementation(Unknown Source)
        //        at Util_gradle._init_$lambda$0(util.gradle.kts:35)
        //        at org.gradle.kotlin.dsl.ProjectExtensionsKt.dependencies(ProjectExtensions.kt:167)

        // Cannot remove extensions
        //mte.getProject().getExtensions();

        Map<String, String> libs3 = Map.of("template", "org.unbescape:unbescape:1.1.6.RELEASE", "ook", "ook:ook:1.2.3");
        mte.getProject().getExtensions().add("libs3", libs3);
        System.out.println("libs3: " + mte.getProject().getExtensions().getByName("libs3"));

        //MapAccessor_Template accessor = new MapAccessor_Template("accessor", libs3);
        //mte.getProject().getExtensions().add("accessor", accessor);

        Map<String, Provider<? extends Dependency>> reconciledLibs = new ReconciledLibrariesBuilder().build(mte.getProject());
        MapAccessor_Template reconciledAccessor = new MapAccessor_Template(reconciledLibs);
        mte.getProject().getExtensions().add("reconciled", reconciledAccessor);

        // aah... can this be used to pass javaVersion to settings??
        //mte.getProject().getGradle().beforeSettings(null);

        // Could not set unknown property 'libs4' for root project 'magictractor-util' of type org.gradle.api.Project.
        // Object map = Map.of("hibernate", "ook:ook:1.2.3");
        // mte.getProject().setProperty("libs4", map);

        // see LibrariesSourceGenerator
        // writeSingleVersionAccessor creates a method that delegates to getVersion()
        // could write the generated code for easy inspection...

        // Object versionCatalogs = mte.getProject().getExtensions().getByName("versionCatalogs");
        // DefaultVersionCatalogsExtension
        // System.out.println("versionCatalogs: " + versionCatalogs + "  " + versionCatalogs.getClass().getSimpleName());
        VersionCatalogsExtension versionCatalogsExtension = mte.getProject().getExtensions().getByType(VersionCatalogsExtension.class);
        System.out.println("getCatalogNames(): " + versionCatalogsExtension.getCatalogNames());

        // ReconciledLibraries container = mte.getProject().getExtensions().create("container", DefaultReconciledLibraries.class);
        // container.add(new ReconciledLibrary("hibernate"));
        // still didn't find it...

        // System.out.println("container.size: " + container.size());
        // System.out.println("container: " + container + "  " + container.getClass());

        Object libs = mte.getProject().getExtensions().getByName("libs");
        System.out.println("libs: " + libs + "  " + libs.getClass());

        // https://www.linen.dev/s/gradle-community/t/18470829/would-it-be-possible-to-generate-accessors-extensions-in-a-d
        // "create an project extension extending a DomainCollection"

        // VersionCatalogBuilder ook = (VersionCatalogBuilder) mte.getProject().getExtensions().getByName("ook");
        //ook.library("hibernate", "ook:ook:1.2.3");
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
     *      testImplementation(libs.junit.jupiter)
     *      testRuntimeOnly(libs.junit.jupiter.platform)
     *      testImplementation(libs.assertj)
     *  }
     *  </pre>
     */
    private void configureDefaultDependencies(MagicTractorExtension mte) {
        Project project = mte.getProject();
        DependencyHandler dependencyHandler = project.getDependencies();
        ExternalModuleDependencyFactory versionCatalog = (ExternalModuleDependencyFactory) project.getExtensions().findByName("libs");

        // Logging libs.
        addDependency(dependencyHandler, versionCatalog, "implementation", "slf4j-api");
        addDependency(dependencyHandler, versionCatalog, "runtimeOnly", "logback.classic");

        // Unit testing libs.
        addDependency(dependencyHandler, versionCatalog, "testImplementation", "junit-jupiter");
        addDependency(dependencyHandler, versionCatalog, "testRuntimeOnly", "junit-jupiter-platform");
        addDependency(dependencyHandler, versionCatalog, "testImplementation", "assertj");
    }

    private void addDependency(DependencyHandler dependencyHandler,
            ExternalModuleDependencyFactory versionCatalog,
            String configurationName,
            String alias) {

        MinimalExternalModuleDependency dependency = versionCatalog.create(alias).get();
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
    private void configurePublishingExtension(MagicTractorExtension mte) {
        PublishingExtension publishingExtension = mte.getProject().getExtensions().getByType(PublishingExtension.class);

        //publishingExtension.publications(publications -> configurePublications(mte, publications));

        MavenPublication maven = publishingExtension.getPublications().create("mavenJava", MavenPublication.class);

        // Wthout from() only the pom file would be created.
        // How to get the SoftwareCompontent matching "from components.java"?
        // Looks like project.getComponents().getByName("java");
        maven.from(mte.getProject().getComponents().getByName("java"));

        MavenPom pom = maven.getPom();

        mte.getProject().getRootDir();

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

}
