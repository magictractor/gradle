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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaVersionAlias {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionAlias.class);

    // Not using the JDK version because the toolchain and version catalog could be using a later JDK than Gradle.
    private static final int CATCH_ALL_JAVA_LANGUAGE_VERSION = 99;

    public static JavaVersionAlias of(String catalogAlias) {
        int lastDotIndex = catalogAlias.lastIndexOf('.');

        if (lastDotIndex >= 0) {
            String lastAliasSegment = catalogAlias.substring(lastDotIndex + 1);
            if (lastAliasSegment.startsWith("java")) {
                try {
                    int javaVersion = Integer.parseInt(lastAliasSegment, 4, lastAliasSegment.length(), 10);
                    return new JavaVersionAlias(catalogAlias, catalogAlias.substring(0, lastDotIndex), javaVersion);
                }
                catch (NumberFormatException e) {
                    LOGGER.warn("Malformed alias {}", catalogAlias);
                    // Swallow exception and fall through to default behaviour.
                }
            }
        }

        return new JavaVersionAlias(catalogAlias);
    }

    private final String catalogAlias;
    private final String normalisedAlias;
    private final int uptoJavaVersion;

    private JavaVersionAlias(String catalogAlias) {
        this(catalogAlias, catalogAlias, CATCH_ALL_JAVA_LANGUAGE_VERSION);
    }

    private JavaVersionAlias(String catalogAlias, String normalisedAlias, int uptoJavaVersion) {
        this.catalogAlias = catalogAlias;
        this.normalisedAlias = normalisedAlias;
        this.uptoJavaVersion = uptoJavaVersion;
    }

    public String getCatalogAlias() {
        return catalogAlias;
    }

    public String getNormalisedAlias() {
        return normalisedAlias;
    }

    public int getUptoJavaVersion() {
        return uptoJavaVersion;
    }

    /**
     * Flag indicating whether this alias has no upper limit on the Java
     * versions it may be used with.
     */
    public boolean isCatchAll() {
        return uptoJavaVersion == CATCH_ALL_JAVA_LANGUAGE_VERSION;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }

        JavaVersionAlias other = (JavaVersionAlias) obj;
        return other.normalisedAlias.equals(normalisedAlias) && other.uptoJavaVersion == uptoJavaVersion;
    }

    @Override
    public int hashCode() {
        return normalisedAlias.hashCode() ^ Integer.hashCode(uptoJavaVersion);
    }

    @Override
    public String toString() {
        //        return MoreObjects.toStringHelper(this)
        //                .add("normalisedAlias", normalisedAlias)
        //                .add("uptoJavaVersion", uptoJavaVersion)
        //                .toString();
        //        "JavaVersionAlias{normalisedAlias=mockito, uptoJavaVersion=10}
        return new StringBuilder().append(getClass().getSimpleName())
                .append("{normalisedAlias=")
                .append(normalisedAlias)
                .append(", uptoJavaVersion=")
                .append(uptoJavaVersion)
                .append('}')
                .toString();
    }

}
