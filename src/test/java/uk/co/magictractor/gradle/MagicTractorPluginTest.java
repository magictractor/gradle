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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class MagicTractorPluginTest {

    @Test
    public void t() throws URISyntaxException {
        File testProjectDir = new ProjectBuilder().build();
        //System.out.println("testProjectDir: " + testProjectDir);

        // TODO! also check for GRADLE_USER_HOME
        // TODO! and move set up to a util/helper

        // Default in Windows 11 was %USERPROFILE%\AppData\Local\Temp\.gradle-test-kit
        // so gradle.properties was not picked up.
        String userHome = System.getProperty("user.home");
        File gradleUserHome = new File(userHome, ".gradle");
        if (!gradleUserHome.exists()) {
            throw new IllegalStateException();
        }

        BuildResult result = GradleRunner.create()
                .withTestKitDir(gradleUserHome)
                .withProjectDir(testProjectDir)
                // TODO! why does this fail in Eclipse when using the configuration cache?
                .withArguments("-Dorg.gradle.configuration-cache=false", "clean", "build")
                //.withArguments("clean", "build")
                // forwardOutput temporary while developing
                .forwardOutput()
                .withDebug(true)
                .build();
    }

}
