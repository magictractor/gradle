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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 * <p>
 * Gradle plugin used to avoid boilerplate configuration being copied between
 * all magictractor.co.uk projects.
 * </p>
 * <p>
 * Referring to other third-party gradle plugins can be helpful during
 * development:
 * <ul>
 * <li>{@code gradle-git-version} provides a {@code gitVersion()} function that
 * can be used in gradle scripts to provide a version number based on the state
 * of the git repository. See
 * https://github.com/palantir/gradle-git-version/blob/5.0.0/src/main/java/com/palantir/gradle/gitversion/GitVersionPlugin.java</li>
 * </ul>
 *
 * @see https://discuss.gradle.org/t/apply-from-vs-apply-plugin-aka-build-logic-reuse/31922/2
 */
//@NonNullApi
//public class ExamplePlugin implements Plugin<Project> {
//
//    private final String REPO = "file:///tmp/somewhere";
//
//    @Override
//    public void apply(final Project project) {
//        final Logger logger = project.getLogger();
//        final ExtensionContainer ext = project.getExtensions();
//        final PluginContainer plugins = project.getPlugins();
//        logger.info("applying java plugin");
//        plugins.apply(JavaPlugin.class);
//        logger.info("download dependencies from maven central");
//        project.getRepositories().mavenCentral();
//        logger.info("all java compile tasks will report all warnings");
//        project.getTasks().withType(JavaCompile.class).configureEach(t -> t.getOptions().getCompilerArgs().add("-Xlint:all"));
//        logger.info("tests will be able to use assertj lib");
//        project.getDependencies().add("testImplementation", "org.assertj:assertj-core:3.12.2");
//        logger.info("maven publish plugin");
//        plugins.apply(MavenPublishPlugin.class);
//        logger.info("configuring publishing repository to be a maven type hosted at {}", REPO);
//        ext.getByType(PublishingExtension.class).getRepositories().maven(m -> m.setUrl(REPO));
//        logger.info("adding an end-user dsl to apply preferences");
//        ext.create("myPlugin", ExamplePluginDsl.class, project);
//    }
//}
//
// TODO! Fix availability of gradle source.
// https://discuss.gradle.org/t/eclipse-buildship-gradle-plugin-development-gradle-api-sources-missing/33461
// https://discuss.gradle.org/t/custom-plugins-dont-include-source/5651
public class MagicTractorProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Note that info() does not get displayed by default.
        // See https://docs.gradle.org/current/userguide/logging.html
        Logger logger = project.getLogger();
        logger.lifecycle(getClass().getSimpleName() + ".apply() called");
    }

}
