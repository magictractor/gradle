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

import com.google.common.base.MoreObjects;

import org.assertj.core.util.Preconditions;
import org.junit.jupiter.api.Test;

public class JavaVersionAliasTest {

    @Test
    public void testOf_hyphen() {
        assertThatThrownBy(() -> JavaVersionAlias.of("hibernate-core"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("catalogAlias must not contain hyphens, is \"hibernate-core\"");
    }

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
        assertThat(actual.getCatalogAlias()).isEqualTo(alias);
        assertThat(actual.getUptoJavaVersion()).isEqualTo(99);
    }

    private void checkVersioned(String alias, String expectedNormalisedAlias, int expectedVersion) {
        JavaVersionAlias actual = JavaVersionAlias.of(alias);
        assertThat(actual.getNormalisedAlias()).isEqualTo(expectedNormalisedAlias);
        assertThat(actual.getCatalogAlias()).isEqualTo(alias);
        assertThat(actual.getUptoJavaVersion()).isEqualTo(expectedVersion);
    }

    @Test
    public void testEquals_null() {
        JavaVersionAlias alias = JavaVersionAlias.of("mockito");

        assertThat(alias.equals(null)).isFalse();
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals_otherType() {
        JavaVersionAlias alias = JavaVersionAlias.of("mockito");

        assertThat(alias.equals("other")).isFalse();
    }

    @Test
    public void testEqualsAndHashCode_same() {
        JavaVersionAlias alias1 = JavaVersionAlias.of("mockito");
        JavaVersionAlias alias2 = JavaVersionAlias.of("mockito");
        // Guard against of() using a cache.
        Preconditions.checkArgument(alias1 != alias2, "alias1 and alias2 should not be the same");

        assertThat(alias1).isEqualTo(alias2);
        assertThat(alias2).isEqualTo(alias1);
        assertThat(alias1.hashCode()).isEqualTo(alias2.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_differentAlias() {
        JavaVersionAlias alias1 = JavaVersionAlias.of("mockito");
        JavaVersionAlias alias2 = JavaVersionAlias.of("guava");

        checkNotEqual(alias1, alias2);
    }

    @Test
    public void testEqualsAndHashCode_differentAliasWithUpto() {
        JavaVersionAlias alias1 = JavaVersionAlias.of("mockito.java10");
        JavaVersionAlias alias2 = JavaVersionAlias.of("guava.java10");

        checkNotEqual(alias1, alias2);
    }

    @Test
    public void testEqualsAndHashCode_differentUpto() {
        JavaVersionAlias alias1 = JavaVersionAlias.of("mockito.java10");
        JavaVersionAlias alias2 = JavaVersionAlias.of("mockito.java16");

        checkNotEqual(alias1, alias2);
    }

    @Test
    public void testEqualsAndHashCode_oneCatchAll() {
        JavaVersionAlias alias1 = JavaVersionAlias.of("mockito");
        JavaVersionAlias alias2 = JavaVersionAlias.of("mockito.java16");

        checkNotEqual(alias1, alias2);
    }

    private void checkNotEqual(JavaVersionAlias alias1, JavaVersionAlias alias2) {
        assertThat(alias1.equals(alias2)).isFalse();
        assertThat(alias2.equals(alias1)).isFalse();
        assertThat(alias1.hashCode()).isNotEqualTo(alias2.hashCode());
    }

    @Test
    public void testToString_catchAll() {
        JavaVersionAlias alias = JavaVersionAlias.of("mockito");
        String guava = MoreObjects.toStringHelper(alias)
                .add("alias", "mockito")
                .toString();
        String expected = "JavaVersionAlias{alias=mockito}";
        Preconditions.checkArgument(expected.equals(guava), "Expected toString() does not match Guava ToStringHelper \"%s\"", expected);

        assertThat(alias.toString()).isEqualTo(expected);
    }

    @Test
    public void testToString_withUpto() {
        JavaVersionAlias alias = JavaVersionAlias.of("mockito.java10");
        String guava = MoreObjects.toStringHelper(alias)
                .add("normalisedAlias", "mockito")
                .add("catalogAlias", "mockito.java10")
                .add("uptoJavaVersion", 10)
                .toString();
        String expected = "JavaVersionAlias{normalisedAlias=mockito, catalogAlias=mockito.java10, uptoJavaVersion=10}";
        Preconditions.checkArgument(expected.equals(guava), "Expected toString() does not match Guava ToStringHelper \"%s\"", expected);

        assertThat(alias.toString()).isEqualTo(expected);
    }

}
