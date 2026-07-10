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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.dsl.VersionCatalogBuilder;
import org.gradle.api.internal.catalog.parser.TomlCatalogFileParser;

public abstract class MagicTractorSettingsPlugin implements Plugin<Settings> {

    private static final String MAGIC_TRACTOR_VERSION_CATALOG = "/gradle/magictractor.versions.toml";
    private static final Map<String, String> ZIP_FILE_SYSTEM_OPTIONS = Map.of("create", "true");
    private static final String UNSET = "__unset__";

    @Override
    public void apply(Settings settings) {
        settings.getRootProject().setName(UNSET);
        settings.getRootProject().setBuildFileName(UNSET);

        // TEMP
        checkDeprecatedSettingsFiles(settings);

        // This plugin provides a version catalog for libs that are used in most projects
        // for logging, testing etc.
        configureMagictractorLibs(settings);

        // Looks for .toml files in the project. Not mandatory.
        configureProjectVersionCatalog(settings);

        settings.getGradle().settingsEvaluated(s -> {
            checkRootProjectName(s);
            checkRootProjectBuildFileName(s);
        });
    }

    private void checkDeprecatedSettingsFiles(Settings settings) {
        StringBuilder failBuilder = new StringBuilder();

        // These File checks are temporary. Remove once the few projects using this plugin are tidied.
        File rootDir = settings.getRootDir();
        if (new File(rootDir, "project.settings.gradle.kts").exists()) {
            failBuilder.append("project.settings.gradle.kts is no longer used\n");
        }
        if (new File(rootDir, "project-local.settings.gradle.kts").exists()) {
            failBuilder.append("project-local.settings.gradle.kts is no longer used\n");
        }

        if (!failBuilder.isEmpty()) {
            throw new GradleException(failBuilder.toString());
        }
    }

    // An explicit rootProject.name is best practice.
    // See https://docs.gradle.org/current/userguide/best_practices_general.html#name_your_root_project.
    private void checkRootProjectName(Settings settings) {
        if (UNSET.equals(settings.getRootProject().getName())) {
            String suggested = settings.getRootDir().getName();
            throw new GradleException("Settings must include an explicit root project name, such as rootProject.name = \"" + suggested + "\"\n");
        }
    }

    /**
     * <p>
     * The build file name should be explicitly configured using
     * {@code rootProject.buildFileName=<name>}.
     * </p>
     * <p>
     * When working with multiple projects, finding a specific settings file can
     * be tricky if they are all called {@code build.gradle.kts}.
     * </p>
     * <p>
     * My preference is to use the last part of the root project name plus
     * {@code ".gradle.kts"}. I decided against having the plugin set that
     * automatically so that anybody unfamiliar with my non-standard preference
     * can get a clue about what is happening.
     * </p>
     */
    // TODO! Gradle property to turn this off
    private void checkRootProjectBuildFileName(Settings settings) {
        if (UNSET.equals(settings.getRootProject().getBuildFileName())) {
            // Could use a StringBuilder, but I suspect that the errors will be centralised so they can be documented.
            // They also need associated properties to turn options off or reduce severity.
            String rootProjectName = settings.getRootProject().getName();
            int lastHyphenIndex = rootProjectName.lastIndexOf("-");
            String suggested = ((lastHyphenIndex == -1) ? rootProjectName : rootProjectName.substring(lastHyphenIndex + 1)) + ".gradle.kts";
            throw new GradleException("Settings must include an explicit root project build file name, such as rootProject.buildFileName = \"" + suggested + "\"\n");
        }
    }

    private void configureMagictractorLibs(Settings settings) {
        settings.getDependencyResolutionManagement().getVersionCatalogs().create("magictractorLibs", builder -> parseVersionCatalog(builder));
    }

    private void configureProjectVersionCatalog(Settings settings) {
        // TODO allow alternative to libs.version.toml to be easily configured
        // to leave "libs" for reconciled version catalogs
    }

    private void parseVersionCatalog(VersionCatalogBuilder builder) {
        try {
            parseVersionCatalog0(builder);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private void parseVersionCatalog0(VersionCatalogBuilder builder) throws IOException, URISyntaxException {
        URI libUri = getClass().getResource(MAGIC_TRACTOR_VERSION_CATALOG).toURI();

        // Must explictly create the ZipFileSystem before it can be used by a Path.
        // https://stackoverflow.com/questions/25032716/getting-filesystemnotfoundexception-from-zipfilesystemprovider-when-creating-a-p
        // https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
        try (FileSystem zipfs = FileSystems.newFileSystem(libUri, ZIP_FILE_SYSTEM_OPTIONS)) {
            Path libPath = Path.of(libUri);
            TomlCatalogFileParser.parse(libPath, builder, null);
        }
    }

}
