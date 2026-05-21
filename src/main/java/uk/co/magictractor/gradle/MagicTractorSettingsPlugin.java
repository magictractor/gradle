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

import java.net.URL;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class MagicTractorSettingsPlugin implements Plugin<Settings> {

    /**
     * <p>
     * Applies a Kotlin Gradle file, {@code magictractor.settings.gradle.kts}.
     * That script will:
     * </p>
     * <ul>
     * <li>Apply the {@code
     */
    @Override
    public void apply(Settings settings) {
        URL url = getClass().getResource("/magictractor.settings.gradle.kts");
        settings.apply(act -> act.from(url));
    }

}
