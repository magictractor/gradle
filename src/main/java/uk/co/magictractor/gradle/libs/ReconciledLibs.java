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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.internal.DynamicObjectAware;
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
public class ReconciledLibs implements DynamicObjectAware {

    private final ExtensibleDynamicObject dynamicObject;

    // Map values are Provider<MinimalExternalModuleDependency> or nested ReconciledLibs.
    @Inject
    public ReconciledLibs(Map<String, Object> map, InstanceGenerator instanceGenerator) {
        if (map.isEmpty()) {
            throw new IllegalArgumentException("map must not be empty");
        }

        this.dynamicObject = new ExtensibleDynamicObject(this, getClass(), instanceGenerator);

        ExtensionContainer extensions = dynamicObject.getExtensions();
        for (var entry : map.entrySet()) {
            extensions.add(entry.getKey(), entry.getValue());
        }
    }

    public boolean isDependency() {
        return false;
    }

    @Override
    public DynamicObject getAsDynamicObject() {
        return dynamicObject;
    }

    @SuppressWarnings("unchecked")
    public Provider<MinimalExternalModuleDependency> getDependency(String normalisedAlias) {
        int dotIndex = normalisedAlias.indexOf('.');
        if (dotIndex >= 0) {
            ReconciledLibs subLibs = (ReconciledLibs) dynamicObject.getExtensions().getByName(normalisedAlias.substring(0, dotIndex));
            return subLibs.getDependency(normalisedAlias.substring(dotIndex + 1));
        }

        if (dynamicObject.getExtensions().getByName(normalisedAlias) instanceof ReconciledLibs) {
            // Has a value and children, like junit and junit.platform
            ReconciledLibs lib = (ReconciledLibs) dynamicObject.getExtensions().getByName(normalisedAlias);
            return lib.getDependencyProvider();
        }

        return (Provider<MinimalExternalModuleDependency>) dynamicObject.getExtensions().getByName(normalisedAlias);
    }

    @SuppressWarnings("unchecked")
    protected Provider<MinimalExternalModuleDependency> getDependencyProvider() {
        return (Provider<MinimalExternalModuleDependency>) dynamicObject.getExtensions().getByName("_this");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append(getClass().getSimpleName())
                .append("{keys=");

        String keys = StreamSupport.stream(dynamicObject.getExtensions().getExtensionsSchema().getElements().spliterator(), false)
                .map(ExtensionSchema::getName)
                .filter(name -> !"ext".equals(name))
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
        sb.append(keys);

        if (isDependency()) {
            sb.append(", isDependency=true");
        }

        sb.append('}');
        return sb.toString();
    }

}
