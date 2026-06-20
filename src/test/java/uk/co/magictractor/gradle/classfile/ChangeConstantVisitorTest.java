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
package uk.co.magictractor.gradle.classfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.classfile.AccessFlags;

import org.junit.jupiter.api.Test;

import uk.co.magictractor.gradle.accessors.RuntimeGeneratedClassBuilder;

public class ChangeConstantVisitorTest extends AbstractClassFileElementVisitorTest {

    @Test
    public void testAccept() {
        ChangeConstantVisitor visitor = new ChangeConstantVisitor("template", "new value");

        assertThat(visitor.acceptsElementType(AccessFlags.class)).isFalse();
    }

    @Test
    public void testGeneratedClass() throws Exception {
        ChangeConstantVisitor visitor = new ChangeConstantVisitor("template", "new value");

        Object generated = new RuntimeGeneratedClassBuilder(TestCase_Template.class)
                .withVisitor(visitor)
                .buildInstance();

        assertGetterValue(generated, "getTemplate").isEqualTo("new value");
        assertFieldValue(generated, "field").isEqualTo("new value");
        assertStaticFieldValue(generated.getClass(), "CONSTANT").isEqualTo("new value");
    }

}
