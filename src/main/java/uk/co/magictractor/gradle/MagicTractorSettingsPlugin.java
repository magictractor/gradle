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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.dsl.VersionCatalogBuilder;
import org.gradle.api.internal.catalog.parser.TomlCatalogFileParser;
import org.gradle.api.invocation.Gradle;

import uk.co.magictractor.gradle.libs.ReconciledLibrariesBuilder;

public abstract class MagicTractorSettingsPlugin implements Plugin<Settings> {

    private static final String GRADLE_EXTRA_PROPERTIES_SETTINGS_KEY = MagicTractorSettingsPlugin.class.getPackageName() + "settings";

    private static final String MAGIC_TRACTOR_VERSION_CATALOG = "/gradle/magictractor.versions.toml";
    private static final Map<String, String> ZIP_FILE_SYSTEM_OPTIONS = Map.of("create", "true");

    // Makes the settings available to project Plugins and Extensions.
    // Specifically, it is used to allow version catalog settings to be inspected without resorting to reflection.
    public static Settings getSettings(Gradle gradle) {
        // hmm, Logger not on Gradle. Just use SLF4J directly?
        // TODO! return Optional and have fallback to reflection?
        return getGradleExtraProperty(gradle, GRADLE_EXTRA_PROPERTIES_SETTINGS_KEY);
    }

    private static <T> T getGradleExtraProperty(Gradle gradle, String propertyKey) {
        @SuppressWarnings("unchecked")
        T propertyValue = (T) gradle.getExtensions().getExtraProperties().get(propertyKey);
        if (propertyKey == null) {
            // TODO! verify the syntax of the workaround ("this" is dubious")
            // TODO! pass the "this" part as a param to this method
            throw new IllegalStateException("Property not found. Either use " + MagicTractorSettingsPlugin.class.getSimpleName() +
                    " or workaround by explicitly setting the property value in your settings file gradle.ext[\"" +
                    propertyKey + "\"=this");
        }
        return propertyValue;
    }

    // and static setter? setter could check whether the prop has already set explicitly?

    @Override
    public void apply(Settings settings) {
        settings.getGradle().getExtensions().getExtraProperties().set(GRADLE_EXTRA_PROPERTIES_SETTINGS_KEY, settings);

        URL url = getClass().getResource("/magictractor-settings-plugin.settings.gradle.kts");
        settings.apply(act -> act.from(url));

        // This plugin provides a version catalog for libs that are used in most projects
        // for logging, testing etc.
        configureMagictractorLibs(settings);
        configureReconciledLibs(settings);

        // Looks for .toml files in the project. Not mandatory.
        configureProjectVersionCatalog(settings);

        // A Map of reconciled version catalogs is created
        // in MagicTractorPlugin where the Java language version
        // is known and can be used to adjust dependencies used.
        // See ReconciledLibrariesBuilder.
    }

    private void configureMagictractorLibs(Settings settings) {
        settings.getDependencyResolutionManagement().getVersionCatalogs().create("magictractorLibs", builder -> parseVersionCatalog(builder));
    }

    /**
     * Creates {@code reconciledLibsNN} version catalogs for each library or
     * version alias with a {@code .javaNN} suffix. TODO! say more...
     */
    private void configureReconciledLibs(Settings settings) {
        settings.getExtensions().add("reconciledLibs", new ReconciledLibrariesBuilder(settings));
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
