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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleDependencyCapabilitiesHandler;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.artifacts.capability.CapabilitySelector;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.capabilities.Capability;
import org.gradle.internal.instantiation.InstanceGenerator;
import org.jspecify.annotations.Nullable;

import groovy.lang.Closure;

/**
 * <p>
 * A variant of {@link ReconciledLibs} where as well has having descendant
 * dependency providers, this node also provides a dependency.
 * </p>
 * <p>
 * For example, ...
 */
public class ReconciledLibsWithDependency extends ReconciledLibs implements ExternalModuleDependency {

    private transient MinimalExternalModuleDependency dependency;

    @Inject
    public ReconciledLibsWithDependency(Map<String, Object> map, InstanceGenerator instanceGenerator) {
        super(map, instanceGenerator);
    }

    @Override
    public boolean isDependency() {
        return true;
    }

    private MinimalExternalModuleDependency ensureDependency() {
        if (dependency == null) {
            dependency = getDependencyProvider().get();
        }
        return dependency;
    }

    @Override
    public String getGroup() {
        return ensureDependency().getGroup();
    }

    @Override
    public String getName() {
        return ensureDependency().getName();
    }

    @Override
    public String getVersion() {
        return ensureDependency().getVersion();
    }

    @Override
    public ModuleIdentifier getModule() {
        return ensureDependency().getModule();
    }

    @Override
    public VersionConstraint getVersionConstraint() {
        return ensureDependency().getVersionConstraint();
    }

    @Override
    public boolean isForce() {
        return ensureDependency().isForce();
    }

    @Override
    public boolean isChanging() {
        return ensureDependency().isChanging();
    }

    @Override
    public boolean isTransitive() {
        return ensureDependency().isTransitive();
    }

    @Override
    public AttributeContainer getAttributes() {
        return ensureDependency().getAttributes();
    }

    @Override
    public Set<CapabilitySelector> getCapabilitySelectors() {
        return ensureDependency().getCapabilitySelectors();
    }

    @Override
    public @Nullable String getTargetConfiguration() {
        return ensureDependency().getTargetConfiguration();
    }

    @Override
    public Set<DependencyArtifact> getArtifacts() {
        ensureDependency();
        return ensureDependency().getArtifacts();
    }

    @Override
    public Set<ExcludeRule> getExcludeRules() {
        return ensureDependency().getExcludeRules();
    }

    @Override
    public boolean isEndorsingStrictVersions() {
        return ensureDependency().isEndorsingStrictVersions();
    }

    @Override
    public @Nullable String getReason() {
        return ensureDependency().getReason();
    }

    @Override
    public void because(@Nullable String reason) {
        throw unsupported();
    }

    @Override
    public ModuleDependency exclude(Map<String, String> excludeProperties) {
        throw unsupported();
    }

    @Override
    public ModuleDependency addArtifact(DependencyArtifact artifact) {
        throw unsupported();
    }

    @Override
    public DependencyArtifact artifact(Closure configureClosure) {
        throw unsupported();
    }

    @Override
    public DependencyArtifact artifact(Action<? super DependencyArtifact> configureAction) {
        throw unsupported();
    }

    @Override
    public ModuleDependency setTransitive(boolean transitive) {
        throw unsupported();
    }

    @Override
    public void setTargetConfiguration(@Nullable String name) {
        throw unsupported();
    }

    @Override
    public ExternalModuleDependency copy() {
        throw unsupported();
    }

    @Override
    public ModuleDependency attributes(Action<? super AttributeContainer> configureAction) {
        throw unsupported();
    }

    @Override
    public ModuleDependency capabilities(Action<? super ModuleDependencyCapabilitiesHandler> configureAction) {
        throw unsupported();
    }

    @Override
    public List<Capability> getRequestedCapabilities() {
        throw unsupported();
    }

    @Override
    public void endorseStrictVersions() {
        throw unsupported();
    }

    @Override
    public void doNotEndorseStrictVersions() {
        throw unsupported();
    }

    @Override
    public void version(Action<? super MutableVersionConstraint> configureAction) {
        throw unsupported();
    }

    @Override
    public boolean matchesStrictly(ModuleVersionIdentifier identifier) {
        throw unsupported();
    }

    @Override
    public ExternalModuleDependency setChanging(boolean changing) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Code must be modified to delegate to the ExternalModuleDependency");
    }

}
