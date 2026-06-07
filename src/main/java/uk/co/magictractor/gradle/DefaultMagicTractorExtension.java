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

import javax.inject.Inject;

import org.gradle.api.internal.provider.PropertyFactory;
import org.gradle.api.provider.Property;

public abstract class DefaultMagicTractorExtension implements MagicTractorExtension {

    private final Property<Integer> javaVersion;
    private final Property<String> pomDescription;
    private final Property<String> pomInceptionYear;

    @Inject
    public DefaultMagicTractorExtension(PropertyFactory propertyFactory) {
        javaVersion = propertyFactory.property(Integer.class);
        // TODO! convention() should read the description from the first para of README.md
        pomDescription = propertyFactory.property(String.class);
        pomInceptionYear = propertyFactory.property(String.class);
    }

    @Override
    public Property<Integer> getJavaVersion() {
        return javaVersion;
    }

    @Override
    public Property<String> getPomDescription() {
        return pomDescription;
    }

    @Override
    public Property<String> getPomInceptionYear() {
        return pomInceptionYear;
    }

}
