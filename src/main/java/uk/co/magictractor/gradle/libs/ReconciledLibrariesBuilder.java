/**
 * Copyright 2026 the original author or authors.
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
package uk.co.magictractor.gradle.libs;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import org.gradle.api.internal.catalog.DependencyModel;
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory;
import org.gradle.api.provider.Provider;
import org.gradle.internal.management.VersionCatalogBuilderInternal;

import uk.co.magictractor.gradle.MagicTractorSettingsPlugin;

/**
 * <p>
 * Builder to create a reconciled view of all defined version catalogs with
 * versions adjusted to match the Java version.
 * </p>
 * <dl>
 * <dt><span class="strong">Merge Version Catalogs</span></dt>
 * </dl>
 * <p>
 * Merges all version catalogs into a single view. Those version catalogs are
 * not modified, but their libraries are now included in another view.
 * </p>
 * <ul>
 * <li>{@code MagicTractorSettingsPlugin} provides a version catalog for
 * projects commonly used by magictractor projects for logging, testing and
 * utils.</li>
 * <li>A project may define version catalogs. It is recommended that projects do
 * not use the usual name {@code libs} for their version catalogs, leaving that
 * for the reconciled view created by this builder. {@code projectLibs} is
 * recommended.</li>
 * </ul>
 * <dl>
 * <dt><span class="strong">Libraries for Java version</span></dt>
 * </dl>
 * <p>
 * Many libraries have major releases that increase the required Java version,
 * for example Mockito 4 can be used with Java 8, but
 * <a href="https://github.com/mockito/mockito/releases/tag/v5.0.0">Mockito
 * 5</a> requires Java 11.
 * </p>
 * <p>
 * The version catalog provided by {@code MagicTractorSettingsPlugin} does not
 * know which Java version will be used. So the version alias
 * {@code mockito-java8} is used for a 4.x.x release and {@code mockito} for a
 * 5.x.x release.
 * <p>
 * The view created by this builder resolves {@code version} and {@code library}
 * names using {@code "-javaN"} suffixes to use different versions depending on
 * the Java version in the project.
 * </p>
 * </ul>
 * <dl>
 * <dt><span class="strong">Design</span></dt>
 * </dl>
 * <p>
 * The are many classes related to version catalogs. Ideally, this code should
 * deal with {@code org.gradle.api.internal.catalog.DependencyModel} in order to
 * inspect the defined {@code version.ref} values.
 * </p>
 * <p>
 * In settings, {@code DependencyResolutionManagement.getVersionCatalogs()}
 * returns a {@code MutableVersionCatalogContainer}.
 * {@code MutableVersionCatalogContainer} contains
 * {@code VersionCatalogBuilder}s. The {@code version} and {@code library}
 * entries in {@code .toml} files correspond to calls on a
 * {@code VersionCatalogBuilder}. However, there are no getters on
 * {@code VersionCatalogBuilder} to allow the configuration to be inspected.
 * {@code VersionCatalogBuilder} has to be cast and {@code build()} called to
 * create a {@code DefaultVersionCatalog}.
 * {@code DefaultVersionCatalog.getLibraryAliases()} then
 * {@code getDependencyData(String libraryAlias)} can then be used to get the
 * {@code DependencyModel}s.
 * </p>
 * <p>
 * Alternatively, {@code DefaultVersionCatalog} is available in project
 * extensions, but only by using reflection.
 * {@code VersionCatalogsExtension.getCatalogNames()} provides the names of the
 * generated {@code LibrariesForXxx} extensions that extend
 * {@code AbstractExternalDependencyFactory} which has a protected field
 * containing a {@code DefaultVersionCatalog}. Sources for the generated code
 * can be found in
 * {@code ~\.gradle\caches\{gradle.version}\dependencies-accessors\{uuid}\sources\org\gradle\accessors\dm}.
 * <p>
 */
// could create an extension, but then cannot be wrapped with an accessor...
//
// Javadoc heading: https://stackoverflow.com/a/18141686
public class ReconciledLibrariesBuilder {

    public Object build(Project project) {
        Settings settings = MagicTractorSettingsPlugin.getSettings(project.getGradle());
        List<DefaultVersionCatalog> versionCatalogs = versionCatalogsFromSettings(settings);

        //        for (String libraryAlias : config.getLibraryAliases()) {
        //            DependencyModel data = config.getDependencyData(libraryAlias);
        //            System.out.println(data + "  " + data.getVersionRef());
        //        }

        Map<String, Provider<MinimalExternalModuleDependency>> map = buildMapFromVersionCatalogs(versionCatalogs, project);

        //        Function<String, ClassFileElementVisitor> clonedMethodVisitorFunction = (libraryAlias) -> {
        //            String methodName = getterNameForLibraryAlias(libraryAlias);
        //            return new ChangeConstantVisitor("template", methodName + "Value");
        //        };
        //        CloneMethodVisitor visitor = new CloneMethodVisitor("getTemplate", map.keySet(), clonedMethodVisitorFunction);
        //
        //        Object generated = new RuntimeGeneratedClassBuilder(ReconciledLibs_Template.class)
        //                .withVisitor(visitor)
        //                .buildInstance();
        //
        //        return generated;

        return map;
    }

    // TODO! find the gradle code that does similar and reuse if possible
    private String getterNameForLibraryAlias(String libraryAlias) {
        int libraryAliasLen = libraryAlias.length();
        StringBuilder sb = new StringBuilder(libraryAliasLen + 3);
        sb.append("get");
        boolean capitaliseNext = true;
        for (int i = 0; i < libraryAliasLen; i++) {
            char c = libraryAlias.charAt(i);
            if (c > 127) {
                throw new IllegalArgumentException();
            }
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                if (capitaliseNext) {
                    c = Character.toUpperCase(c);
                    capitaliseNext = false;
                }
                sb.append(c);
            }
            // TODO! if using this approach then '-' is treated the same as '.'
            // and they need nested classes where there is more than one separator
            // Park and look at generating and compiling Java source files.
            else if (c == '-') {
                capitaliseNext = true;
            }
            else {
                throw new IllegalArgumentException("Unexpected character '" + c + "' in library alias \"" + libraryAlias + "\"");
            }
        }

        return sb.toString();
    }

    private List<DefaultVersionCatalog> versionCatalogsFromSettings(Settings settings) {
        return settings.getDependencyResolutionManagement()
                .getVersionCatalogs()
                .stream()
                .map(VersionCatalogBuilderInternal.class::cast)
                .map(VersionCatalogBuilderInternal::build)
                .toList();
    }

    // Could use this if settings plugin not used.
    private List<DefaultVersionCatalog> versionCatalogsFromExtensionWithReflection(Project project) {
        try {
            return versionCatalogsFromExtensionWithReflection0(project);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<DefaultVersionCatalog> versionCatalogsFromExtensionWithReflection0(Project project) throws ReflectiveOperationException {
        Field field;
        field = AbstractExternalDependencyFactory.class.getDeclaredField("config");
        field.setAccessible(true);

        List<DefaultVersionCatalog> versionCatalogs = new ArrayList<>();
        Set<String> catalogNames = project.getExtensions().getByType(VersionCatalogsExtension.class).getCatalogNames();
        for (String catalogName : catalogNames) {
            Object librariesFor = project.getExtensions().getByName(catalogName);
            DefaultVersionCatalog versionCatalog = (DefaultVersionCatalog) field.get(librariesFor);
            versionCatalogs.add(versionCatalog);
        }

        return versionCatalogs;
    }

    public Map<String, Provider<? extends Dependency>> xxxbuild(VersionCatalogsExtension versionCatalogsExtension) {
        Map<String, Provider<? extends Dependency>> result = new HashMap<>();
        for (VersionCatalog versionCatalog : versionCatalogsExtension) {
            // versionCatalog.getVersionAliases();
            versionCatalog.getLibraryAliases();
            for (String libraryAlias : versionCatalog.getLibraryAliases()) {
                Provider<MinimalExternalModuleDependency> library = versionCatalog.findLibrary(libraryAlias).get();
                System.out.println(libraryAlias + " -> " + library.get());
                result.put(libraryAlias, library);
            }
        }
        return result;
    }

    private Map<String, Provider<MinimalExternalModuleDependency>> buildMapFromVersionCatalogs(List<DefaultVersionCatalog> versionCatalogs, Project project) {
        Map<String, Provider<MinimalExternalModuleDependency>> map = new HashMap<>();
        for (DefaultVersionCatalog versionCatalog : versionCatalogs) {
            addVersionCatalog(map, versionCatalog, project);
        }

        return map;
    }

    private void addVersionCatalog(Map<String, Provider<MinimalExternalModuleDependency>> map, DefaultVersionCatalog versionCatalog, Project project) {
        for (String libraryAlias : versionCatalog.getLibraryAliases()) {
            DependencyModel library = versionCatalog.getDependencyData(libraryAlias);
            System.out.println(library);

            ExternalModuleDependencyFactory libraryFor = (ExternalModuleDependencyFactory) project.getExtensions().getByName(versionCatalog.getName());
            map.put(libraryAlias, libraryFor.create(libraryAlias));
        }

    }

}
