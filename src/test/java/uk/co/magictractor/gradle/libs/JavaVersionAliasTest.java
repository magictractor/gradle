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

import org.junit.jupiter.api.Test;

public class JavaVersionAliasTest {

    @Test
    public void testOf_noDot() {
        checkUnversioned("guava");
    }

    @Test
    public void testOf_noJavaVersion() {
        checkUnversioned("hibernate.core");
    }

    @Test
    public void testOf_singleDigit() {
        checkVersioned("mockito.java8", "mockito", 8);
    }

    @Test
    public void testOf_doubleDigit() {
        checkVersioned("something.java11", "something", 11);
    }

    @Test
    public void testOf_missingJavaVersion() {
        checkUnversioned("malformed.java");
    }

    @Test
    public void testOf_nonNumericJavaVersion() {
        checkUnversioned("malformed.javaNN");
    }

    private void checkUnversioned(String alias) {
        JavaVersionAlias actual = JavaVersionAlias.of(alias);
        assertThat(actual.getNormalisedAlias()).isEqualTo(alias);
        assertThat(actual.getUptoJavaVersion()).isEqualTo(99);
    }

    private void checkVersioned(String alias, String expectedNormalisedAlias, int expectedVersion) {
        JavaVersionAlias actual = JavaVersionAlias.of(alias);
        assertThat(actual.getNormalisedAlias()).isEqualTo(expectedNormalisedAlias);
        assertThat(actual.getUptoJavaVersion()).isEqualTo(expectedVersion);
    }

    @Test
    public void testToString() {
        JavaVersionAlias alias = JavaVersionAlias.of("mockito.java10");
        assertThat(alias.toString()).isEqualTo("JavaVersionAlias{normalisedAlias=mockito, uptoJavaVersion=10}");
    }

}
