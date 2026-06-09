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

public abstract class MagicTractorSettingsPlugin implements Plugin<Settings> {

    private static final String MAGIC_TRACTOR_VERSION_CATALOG = "/gradle/magictractor.versions.toml";
    private static final Map<String, String> ZIP_FILE_SYSTEM_OPTIONS = Map.of("create", "true");

    @Override
    public void apply(Settings settings) {
        URL url = getClass().getResource("/magictractor-settings-plugin.settings.gradle.kts");
        settings.apply(act -> act.from(url));

        settings.getDependencyResolutionManagement().getVersionCatalogs().create("libs", builder -> parseVersionCatalog(builder));
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
