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

import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory;
import org.gradle.api.provider.Provider;

/**
 * <p>
 * This class serves as a template for generated accessor classes that allow
 * Kotlin scripts to use syntax like {@code libs.hibernate} rather than
 * {@libs.get("hibernate")}.
 * </p>
 * <p>
 */
// Note - does not work if there are generics on the class
public class ReconciledLibs_Template implements ExternalModuleDependencyFactory {

    private final Map<String, Provider<MinimalExternalModuleDependency>> map;

    public ReconciledLibs_Template(Map<String, Provider<MinimalExternalModuleDependency>> map) {
        this.map = map;
    }

    public Provider<MinimalExternalModuleDependency> getTemplate() {
        return create("template");
    }

    // Once everything is robust, this method could be removed
    @Override
    public Provider<MinimalExternalModuleDependency> create(String alias) {
        Provider<MinimalExternalModuleDependency> value = map.get(alias);
        if (value == null) {
            // If this is to be retained long-term, then the keys could be sorted
            throw new IllegalStateException("No library with alias \"" + alias + "\", the available aliases are " + map.keySet());
        }
        return value;
    }

    @Override
    public String toString() {
        // TODO! match style with Guava
        return getClass().getSimpleName() + map.keySet();
    }

}
