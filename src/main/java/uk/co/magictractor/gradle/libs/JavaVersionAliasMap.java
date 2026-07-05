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

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;

public class JavaVersionAliasMap<V> {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JavaVersionAliasMap.class);

    private static final Comparator<Integer> JAVA_VERSION_COMPARATOR = (v1, v2) -> v2 - v1;
    private static final Comparator<JavaVersionAlias> JAVA_VERSION_ALIAS_COMPARATOR = Comparator.comparing(JavaVersionAlias::getUptoJavaVersion, JAVA_VERSION_COMPARATOR);

    // LinkedHashMap to keep same order as in the version catalogs.
    private final Map<String, TreeMap<JavaVersionAlias, V>> aliases = new LinkedHashMap<>();

    public Set<String> keySet() {
        return aliases.keySet();
    }

    public void put(String catalogAlias, V value) {
        put(JavaVersionAlias.of(catalogAlias), value);
    }

    private void put(JavaVersionAlias alias, V value) {
        TreeMap<JavaVersionAlias, V> v = aliases.computeIfAbsent(alias.getNormalisedAlias(), _ -> new TreeMap<>(JAVA_VERSION_ALIAS_COMPARATOR));
        if (v.containsKey(alias)) {
            throw new IllegalArgumentException("Already have value matching \"" + alias.getCatalogAlias() + "\"");
        }
        v.put(alias, value);
    }

    public V valueForJavaVersion(String normalisedAlias, int javaVersion) {
        TreeMap<JavaVersionAlias, V> candidates = aliases.get(normalisedAlias);
        Map.Entry<JavaVersionAlias, V> entry = aliasForJavaVersion(javaVersion, candidates);
        return entry.getValue();
    }

    /** @param candidates */
    private Map.Entry<JavaVersionAlias, V> aliasForJavaVersion(int javaVersion, TreeMap<JavaVersionAlias, V> candidates) {
        Iterator<Map.Entry<JavaVersionAlias, V>> iter = candidates.entrySet().iterator();

        Map.Entry<JavaVersionAlias, V> result = iter.next();
        JavaVersionAlias alias = result.getKey();
        if (!alias.isCatchAll()) {
            String message = "Aliases for \"" + alias.getNormalisedAlias() + "\" are only specified up to Java version " + alias.getUptoJavaVersion();
            if (alias.getUptoJavaVersion() < javaVersion) {
                throw new IllegalStateException(message);
            }
            else {
                LOGGER.warn(message);
            }
        }

        while (iter.hasNext()) {
            Map.Entry<JavaVersionAlias, V> candidate = iter.next();
            if (candidate.getKey().getUptoJavaVersion() < javaVersion) {
                break;
            }
            result = candidate;
        }

        return result;
    }

}
