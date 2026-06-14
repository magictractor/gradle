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
package uk.co.magictractor.gradle.accessors;

import java.util.Map;

/**
 * <p>
 * This class serves as a template for dynamically generated accessors that
 * allow Kotlin scripts to use syntax like {@code libs.hibernate} rather than
 * {@libs.get("hibernate")}.
 * </p>
 * <p>
 */
// Note - does not work if there are generics on the class
public class MapAccessor_Template {

    private final Map<String, ?> map;

    public MapAccessor_Template(Map<String, ?> map) {
        this.map = map;
    }

    public Object getTemplate() {
        return get("mockito");
    }

    // Once everything is robust, this method could be removed
    public Object get(String libraryAlias) {
        //return map.get("template");
        Object value = map.get("mockito");
        if (value == null) {
            // If this is to be retained long-term, then the keys could be sorted
            throw new IllegalStateException("No library with alias \"" + libraryAlias + "\", the available aliases are " + map.keySet());
        }
        return value;
    }

    @Override
    public String toString() {
        // TODO! match style with Guava
        return getClass().getSimpleName() + map.keySet();
    }

}
