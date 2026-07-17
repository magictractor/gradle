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
package uk.co.magictractor.gradle.extension;

import org.gradle.api.provider.Property;

public interface StandardDependencies {

    /**
     * The default value for {@code standardDependencies} values. It defaults to
     * {@code true} and other properties may selectively be turned off.
     * Alternatively, this may be change to {@false} and other properties
     * selectively turned on.
     */
    Property<Boolean> getDefault();

    /** If true, SLF4J API and logback dependencies are added. */
    Property<Boolean> getAddLoggingDependencies();

    /** If true, JUnit and AssertJ dependencies are added. */
    Property<Boolean> getAddUnitTestDependencies();

}
