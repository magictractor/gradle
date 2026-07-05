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

import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtensionsSchema.ExtensionSchema;
import org.gradle.api.provider.Provider;
import org.gradle.internal.extensibility.ExtensibleDynamicObject;
import org.gradle.internal.instantiation.InstanceGenerator;
import org.gradle.internal.metaobject.DynamicObject;

// MethodMixIn might suffice - see DefaultDependencyHandler
/**
 * <p>
 * Instances must be created via an {@code ObjectFactory} (or other Gradle
 * instantiation code) otherwise the getters used in DSL will not be created.
 * </p>
 */
public abstract class ReconciledLibs implements DynamicObjectAware {

    private final ExtensibleDynamicObject dynamicObject;

    // Map values are Provider<String> or nested ReconciledLibs.
    @Inject
    public ReconciledLibs(Map<String, Object> map, InstanceGenerator instanceGenerator) {
        if (map.isEmpty()) {
            throw new IllegalArgumentException("map must not be empty");
        }

        dynamicObject = new ExtensibleDynamicObject(this, getClass(), instanceGenerator);

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

        // temp
        if (dynamicObject.getExtensions().getByName(normalisedAlias) instanceof ReconciledLibs) {
            throw new IllegalStateException("Expected " + normalisedAlias + " to be a Provider");
        }

        return (Provider<String>) dynamicObject.getExtensions().getByName(normalisedAlias);
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
