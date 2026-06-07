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

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 *
 */
public interface MagicTractorExtension {

    /** The owning project. */
    // design decision: this is used to reduce the number of parameters passed around in MagicTractorPlugin
    Project getProject();

    /**
     * <p>
     * The Java version of the source files and target classes.
     * <p>
     * <p>
     * This is required. LTS releases should generally be used (8, 11, 17, 21,
     * 25 etc).
     * </p>
     * <p>
     * This replaces {@code toolchain} declarations such as <pre>
     * java {
     *     toolchain {
     *         languageVersion = JavaLanguageVersion.of(8)
     *     }
     * }
     * </pre>
     */
    Property<Integer> getJavaVersion();

    /** The description to be included in the {@code pom.xml} file. */
    Property<String> getPomDescription();

    /** The inception year to be included in the {@code pom.xml} file. */
    Property<String> getPomInceptionYear();

}
