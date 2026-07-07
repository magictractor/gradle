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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.resolve.MutableVersionCatalogContainer;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.ImmutableVersionConstraint;
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import org.gradle.api.internal.catalog.DependencyModel;
import org.gradle.api.internal.catalog.VersionModel;
import org.gradle.api.provider.Property;
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
public class ReconciledLibsBuilder {

    private final Project project;

    private final VersionedAliasMap<VersionModel> versionsMap = new VersionedAliasMap<>();
    private final VersionedAliasMap<DependencyModel> librariesMap = new VersionedAliasMap<>();

    public ReconciledLibsBuilder(Project project) {
        this.project = project;

        // TODO! fallback to reflection on LibrariesForXxx (or other) if MagicTractorSettingsPlugin is not used.
        Settings settings = MagicTractorSettingsPlugin.getSettings(project.getGradle());

        MutableVersionCatalogContainer vcbs = settings.getDependencyResolutionManagement().getVersionCatalogs();
        List<DefaultVersionCatalog> vcs = vcbs.stream()
                .map(VersionCatalogBuilderInternal.class::cast)
                .map(VersionCatalogBuilderInternal::build)
                .toList();

        for (DefaultVersionCatalog vc : vcs) {
            for (String versionAlias : vc.getVersionAliases()) {
                versionsMap.put(versionAlias, vc.getVersion(versionAlias));
            }
            for (String libraryAlias : vc.getLibraryAliases()) {
                librariesMap.put(libraryAlias, vc.getDependencyData(libraryAlias));
            }
        }
    }

    // Property must be used as a parameter because the MagicTractorExtension is typically not populated until after
    // ReconciledLibs has been built from Plugin.apply().
    public ReconciledLibs build(Property<Integer> javaVersion) {
        Map<String, Provider<MinimalExternalModuleDependency>> reconciledLibsMap = new HashMap<>();

        for (String normalisedAlias : librariesMap.keySet()) {
            // A new transform is used rather than mapping javaVersion.
            // If transforming javaVersion and the properties are used too soon, such
            // as when setting up default dependencies and javaVersion is still null
            // (not wired yet) then the transformed value is also null and the transform
            // is not called.
            Provider<MinimalExternalModuleDependency> dependencyProvider = project.provider(() -> lookupDependency(normalisedAlias, javaVersion));

            reconciledLibsMap.put(normalisedAlias, dependencyProvider);
        }

        return build(reconciledLibsMap);
    }

    private MinimalExternalModuleDependency lookupDependency(String normalisedAlias, Property<Integer> javaVersionProperty) {
        if (!javaVersionProperty.isPresent()) {
            throw new IllegalStateException("javaVersion Property has not been set, ReconciledLib is possibly being used too soon");
        }

        int javaVersion = javaVersionProperty.get();
        DependencyModel dependencyModel = librariesMap.valueForJavaVersion(normalisedAlias, javaVersion);

        ImmutableVersionConstraint version;
        if (dependencyModel.getVersionRef() != null) {
            // versionRef may include a "-javaNN" suffix,
            // typically used when the library and version refs are not used in later Java versions.
            VersionedAlias versionRefAlias = VersionedAlias.of(dependencyModel.getVersionRef());
            VersionModel versionModel = versionsMap.valueForJavaVersion(versionRefAlias.getNormalisedAlias(), javaVersion);
            version = versionModel.getVersion();

            // TODO! there should probably be additional checks or filters if versionRefAlias is not a catch-all
        }
        else {
            throw new UnsupportedOperationException("TODO");
        }

        // The mapping to a ModuleDependency is usually done in AbstractExternalDependencyFactory which
        // is extended by the generated LibrariesForXxx classes. However, the LibrariesForXxx classes
        // might not contain the appropriate library version.
        //
        // See AbstractExternalDependencyFactory.create().
        return new DefaultMinimalDependency(
            DefaultModuleIdentifier.newId(dependencyModel.getGroup(), dependencyModel.getName()),
            new DefaultMutableVersionConstraint(version));
    }

    private ReconciledLibs build(Map<String, Provider<MinimalExternalModuleDependency>> subLibsMap) {
        Map<String, Object> reconciledLibsMap = new HashMap<>();

        Set<String> providerKeys = new HashSet<>();
        Set<String> subGroupKeys = new HashSet<>();

        for (String libraryAlias : subLibsMap.keySet()) {
            int dotIndex = libraryAlias.indexOf('.');
            if (dotIndex == -1) {
                providerKeys.add(libraryAlias);
            }
            else {
                subGroupKeys.add(libraryAlias.substring(0, dotIndex));
            }
        }

        for (String providerKey : providerKeys) {
            reconciledLibsMap.put(providerKey, subLibsMap.get(providerKey));
        }

        for (String subGroupKey : subGroupKeys) {
            Map<String, Provider<MinimalExternalModuleDependency>> subMap = new HashMap<>();
            String subGroupPrefix = subGroupKey + ".";
            for (var subLibsEntry : subLibsMap.entrySet()) {
                if (subLibsEntry.getKey().startsWith(subGroupPrefix)) {
                    subMap.put(subLibsEntry.getKey().substring(subGroupPrefix.length()), subLibsEntry.getValue());
                }
            }

            if (providerKeys.contains(subGroupKey)) {
                subMap.put("_this", subLibsMap.get(subGroupKey));
            }

            ReconciledLibs subGroup = build(subMap);
            reconciledLibsMap.put(subGroupKey, subGroup);
        }

        boolean isDependency = reconciledLibsMap.containsKey("_this");
        Class<? extends ReconciledLibs> libsClass = isDependency ? ReconciledLibsWithDependency.class : ReconciledLibs.class;
        return project.getObjects().newInstance(libsClass, reconciledLibsMap);
    }

}
