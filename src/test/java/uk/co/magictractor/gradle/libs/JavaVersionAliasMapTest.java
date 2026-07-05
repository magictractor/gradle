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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class JavaVersionAliasMapTest {

    @Test
    public void test() {
        JavaVersionAliasMap<String> map = new JavaVersionAliasMap<>();
        map.put("mockito", "5.23.0");
        map.put("mockito.java16", "4.11.0");

        assertThat(map.keySet()).hasSize(1);

        assertThat(map.valueForJavaVersion("mockito", 8)).isEqualTo("4.11.0");
        assertThat(map.valueForJavaVersion("mockito", 16)).isEqualTo("4.11.0");
        assertThat(map.valueForJavaVersion("mockito", 17)).isEqualTo("5.23.0");
        assertThat(map.valueForJavaVersion("mockito", 25)).isEqualTo("5.23.0");
    }

    @Test
    public void t2() {
        JavaVersionAliasMap<String> map = new JavaVersionAliasMap<>();
        map.put("junit", "6.1.1");
        map.put("junit.platform", "6.1.1");
        map.put("junit.java16", "5.14.4");
        map.put("junit.platform.java16", "1.14.4");

        assertThat(map.keySet()).hasSize(2);

        assertThat(map.valueForJavaVersion("junit", 8)).isEqualTo("5.14.4");
        assertThat(map.valueForJavaVersion("junit", 16)).isEqualTo("5.14.4");
        assertThat(map.valueForJavaVersion("junit", 17)).isEqualTo("6.1.1");
        assertThat(map.valueForJavaVersion("junit", 25)).isEqualTo("6.1.1");

        assertThat(map.valueForJavaVersion("junit.platform", 8)).isEqualTo("1.14.4");
        assertThat(map.valueForJavaVersion("junit.platform", 16)).isEqualTo("1.14.4");
        assertThat(map.valueForJavaVersion("junit.platform", 17)).isEqualTo("6.1.1");
        assertThat(map.valueForJavaVersion("junit.platform", 25)).isEqualTo("6.1.1");
    }

    @Test
    public void test_noCatchAll() {
        JavaVersionAliasMap<String> map = new JavaVersionAliasMap<>();
        map.put("mockito.java16", "4.11.0");

        assertThat(map.valueForJavaVersion("mockito", 16)).isEqualTo("4.11.0");
        assertThatThrownBy(() -> map.valueForJavaVersion("mockito", 17))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Aliases for \"mockito\" are only specified up to Java version 16");
    }

    @Test
    public void test_putDuplicate() {
        JavaVersionAliasMap<String> map = new JavaVersionAliasMap<>();
        map.put("mockito.java16", "4.11.0");
        // TODO! check warning was logged

        assertThatThrownBy(() -> map.put("mockito.java16", "4.11.0"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Already have value matching \"mockito.java16\"");
    }

}
