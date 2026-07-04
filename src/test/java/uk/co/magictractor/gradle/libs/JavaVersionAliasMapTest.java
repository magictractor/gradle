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

public class JavaVersionAliasMapTest {

    //    @Test
    //    public void testAliasesForJavaVersion_noAlts() {
    //        JavaVersionAliasMap<String> map = new JavaVersionAliasMap<>();
    //        map.put("guava", "33.6.0-jre");
    //
    //        assertThat(map.aliasesForJavaVersion(17)).containsExactly("guava");
    //        assertThat(map.aliasesForJavaVersion(8)).containsExactly("guava");
    //    }
    //
    //    @Test
    //    public void testAliasesForJavaVersion_oneAlt() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        map.put("mockito.java8");
    //        map.put("mockito");
    //
    //        assertThat(map.aliasesForJavaVersion(17)).containsExactly("mockito");
    //        assertThat(map.aliasesForJavaVersion(8)).containsExactly("mockito.java8");
    //        assertThat(map.aliasesForJavaVersion(9)).containsExactly("mockito");
    //        assertThat(map.aliasesForJavaVersion(7)).containsExactly("mockito.java8");
    //        assertThat(map.aliasesForJavaVersion(1)).containsExactly("mockito.java8");
    //    }
    //
    //    @Test
    //    public void testAliasesForJavaVersion_twoAlts() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        // Shuffled order here.
    //        map.put("mockito.java16");
    //        map.put("mockito");
    //        map.put("mockito.java10");
    //
    //        assertThat(map.aliasesForJavaVersion(25)).containsExactly("mockito");
    //        assertThat(map.aliasesForJavaVersion(17)).containsExactly("mockito");
    //        assertThat(map.aliasesForJavaVersion(16)).containsExactly("mockito.java16");
    //        assertThat(map.aliasesForJavaVersion(11)).containsExactly("mockito.java16");
    //        assertThat(map.aliasesForJavaVersion(10)).containsExactly("mockito.java10");
    //        assertThat(map.aliasesForJavaVersion(8)).containsExactly("mockito.java10");
    //        assertThat(map.aliasesForJavaVersion(1)).containsExactly("mockito.java10");
    //    }
    //
    //    @Test
    //    public void testAliasesForJavaVersion_noMatch() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        map.put("mockito.java10");
    //
    //        assertThatThrownBy(() -> map.aliasesForJavaVersion(11))
    //                .isExactlyInstanceOf(IllegalStateException.class)
    //                .hasMessage("Aliases for mockito are only specified up to Java version 10");
    //    }
    //
    //    @Test
    //    public void testAliasesForJavaVersion_noCatchAll() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        map.put("mockito.java10");
    //
    //        // Here there's a match, but no catch all
    //        assertThat(map.aliasesForJavaVersion(8)).containsExactly("mockito.java10");
    //
    //        // TODO! check logged messages - create an Extension in util project?
    //    }
    //
    //    @Test
    //    public void testAdd_duplicate() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        map.put("guava");
    //
    //        assertThatThrownBy(() -> map.put("guava"))
    //                .isExactlyInstanceOf(IllegalArgumentException.class)
    //                .hasMessage("Already have value matching JavaVersionAlias{normalisedAlias=guava, uptoJavaVersion=99}");
    //    }
    //
    //    @Test
    //    public void testJavaVersionBoundaries_empty() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //
    //        assertThat(map.getJavaVersionBoundaries()).isEmpty();
    //    }
    //
    //    @Test
    //    public void testJavaVersionBoundaries() {
    //        JavaVersionAliasMap map = new JavaVersionAliasMap();
    //        map.put("mockito.java16");
    //        map.put("mockito");
    //        map.put("mockito.java10");
    //
    //        assertThat(map.getJavaVersionBoundaries()).containsExactly(99, 16, 10);
    //    }

}
