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
package uk.co.magictractor.gradle.extension;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.provider.PropertyFactory;
import org.gradle.api.provider.Property;

public class DefaultMagicTractorExtension implements MagicTractorExtension {

    private final ProjectInternal project;

    private final Property<Integer> javaVersion;
    private final StandardDependencies standardDependencies;
    private final Property<String> pomDescription;
    private final Property<String> pomInceptionYear;

    @Inject
    public DefaultMagicTractorExtension(Project project, PropertyFactory propertyFactory) {
        this.project = (ProjectInternal) project;

        javaVersion = propertyFactory.property(Integer.class);
        standardDependencies = new DefaultStandardDependencies(propertyFactory);
        // TODO! convention() should read the description from the first para of README.md
        pomDescription = propertyFactory.property(String.class);
        pomInceptionYear = propertyFactory.property(String.class);
    }

    @Override
    public ProjectInternal getProject() {
        return project;
    }

    @Override
    public Property<Integer> getJavaVersion() {
        return javaVersion;
    }

    @Override
    public StandardDependencies getStandardDependencies() {
        return standardDependencies;
    }

    @Override
    public StandardDependencies standardDependencies(Action<? super StandardDependencies> configureAction) {
        configureAction.execute(standardDependencies);
        return standardDependencies;
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
