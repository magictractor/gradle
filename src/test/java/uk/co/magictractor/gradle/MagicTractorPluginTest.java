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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class MagicTractorPluginTest {

    @Test
    public void t() throws URISyntaxException {
        URL url = getClass().getResource("/example");
        File testProjectDir = Paths.get(url.toURI()).toFile();

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("clean", "build")
                // forwardOutput temporary while developing
                .forwardOutput()
                .withDebug(true)
                .build();
    }

}
