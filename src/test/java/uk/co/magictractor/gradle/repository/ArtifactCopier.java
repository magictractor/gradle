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
package uk.co.magictractor.gradle.repository;

import java.io.IOException;

/**
 * Copy an artifact from one repository to another. Used to copy third-party
 * dependencies from Gradle Plugin Portal to the magictractor Repsy repository.
 */
public class ArtifactCopier {

    private final Repository sourceRepository;
    private final Repository targetRepository;

    public ArtifactCopier() {
        this(Repository.GRADLE_PLUGIN_PORTAL, Repository.REPSY);
    }

    public ArtifactCopier(Repository sourceRepository, Repository targetRepository) {
        this.sourceRepository = sourceRepository;
        this.targetRepository = targetRepository;
    }

    public ArtifactCopier copy(String artifact) {
        for (RepositoryFile file : sourceRepository.list(artifact)) {
            System.out.println(file);
            targetRepository.write(file);
        }
        return this;
    }

    public static void main(String[] args) throws IOException {
        // TODO! allow copy to include transitive dependencies (configurable),
        // would need to parse .pom or .module (if present).
        new ArtifactCopier()
                // .copy("com.netflix.nebula:nebula-release-plugin:21.0.0")
                //.copy("com.fasterxml.jackson:jackson-bom:2.14.2")
                //.copy("com.fasterxml.jackson:jackson-parent:2.14")
                //.copy("com.fasterxml:oss-parent:48")
                //.copy("com.github.zafarkhaja:java-semver:0.9.0")
                .copy("org.sonatype.oss:oss-parent:9");
    }

}
