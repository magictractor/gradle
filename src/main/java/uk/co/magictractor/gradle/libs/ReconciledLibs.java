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

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtensionsSchema.ExtensionSchema;
import org.gradle.api.provider.Provider;
import org.gradle.internal.extensibility.ExtensibleDynamicObject;
import org.gradle.internal.instantiation.InstanceGenerator;
import org.gradle.internal.metaobject.DynamicObject;

/**
 * <p>
 * Instances must be created via an {@code ObjectFactory} (or other Gradle
 * instantiation code) otherwise the getters used in DSL for "libs.guava" etc
 * will not be created.
 * </p>
 */
// TODO! rather than always inheriting from DefaultExternalModuleDependency it could
// be added to the DynamicObject.
public class ReconciledLibs extends DefaultExternalModuleDependency implements DynamicObjectAware {

    private final ExtensibleDynamicObject dynamicObject;

    private transient String group = null;
    private transient String name = null;
    private transient String version = null;

    // Map values are Provider<String> or nested ReconciledLibs.
    @Inject
    public ReconciledLibs(Map<String, Object> map, InstanceGenerator instanceGenerator) {
        super("placeholderGroup", "placeholderName", "placeholderVersion");

        if (map.isEmpty()) {
            throw new IllegalArgumentException("map must not be empty");
        }

        this.dynamicObject = new ExtensibleDynamicObject(this, getClass(), instanceGenerator);

        ExtensionContainer extensions = dynamicObject.getExtensions();
        for (var entry : map.entrySet()) {
            extensions.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public DynamicObject getAsDynamicObject() {
        return dynamicObject;
    }

    @SuppressWarnings("unchecked")
    public Provider<String> getDependency(String normalisedAlias) {
        int dotIndex = normalisedAlias.indexOf('.');
        if (dotIndex >= 0) {
            ReconciledLibs subLibs = (ReconciledLibs) dynamicObject.getExtensions().getByName(normalisedAlias.substring(0, dotIndex));
            return subLibs.getDependency(normalisedAlias.substring(dotIndex + 1));
        }

        if (dynamicObject.getExtensions().getByName(normalisedAlias) instanceof ReconciledLibs) {
            // Has a value and children, like junit.jupiter and junit.jupiter.platform
            ReconciledLibs lib = (ReconciledLibs) dynamicObject.getExtensions().getByName(normalisedAlias);
            return (Provider<String>) lib.dynamicObject.getExtensions().getByName("_this");
        }

        return (Provider<String>) dynamicObject.getExtensions().getByName(normalisedAlias);
    }

    @Override
    public String getGroup() {
        ensureDependency();
        return group;
    }

    @Override
    public String getName() {
        ensureDependency();
        return name;
    }

    @Override
    public String getVersion() {
        ensureDependency();
        return version;
    }

    @Override
    public VersionConstraint getVersionConstraint() {
        // Ick. More evidence that Providers should be switched to return a Dependency or DependencyModel.
        return new DefaultMutableVersionConstraint(version);
    }

    private void ensureDependency() {
        if (group != null) {
            return;
        }

        // TODO! rather than creating a String and then parsing it
        // the provider could/should be changed to Provider<Dependency> or Provider<DependencyModel>
        Provider<String> provider = (Provider<String>) dynamicObject.getExtensions().getByName("_this");
        String dependency = provider.get();

        int colonIndex1 = dependency.indexOf(":");
        int colonIndex2 = dependency.indexOf(":", colonIndex1 + 1);
        if (dependency.indexOf(':', colonIndex2 + 1) >= 0) {
            throw new IllegalStateException("Expected exactly two colons in \"" + dependency + "\"");
        }

        group = dependency.substring(0, colonIndex1);
        name = dependency.substring(colonIndex1 + 1, colonIndex2);
        version = dependency.substring(colonIndex2 + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append(getClass().getSimpleName())
                .append("{keys=");

        boolean first = true;
        for (ExtensionSchema schema : dynamicObject.getExtensions().getExtensionsSchema().getElements()) {
            if (schema.getName().equals("ext")) {
                continue;
            }
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(schema.getName());
        }

        sb.append('}');
        return sb.toString();
    }

}
