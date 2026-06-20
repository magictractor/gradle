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

import org.junit.jupiter.api.Test;

import uk.co.magictractor.gradle.accessors.RuntimeGeneratedClassBuilder;

public class ChangeMethodNameVisitorTest extends AbstractClassFileElementVisitorTest {

    @Test
    public void testMethodNameChanged() throws Exception {
        ChangeMethodNameVisitor visitor = new ChangeMethodNameVisitor("getTemplate", "getValue");

        Object generated = new RuntimeGeneratedClassBuilder(TestCase_Template.class)
                .withVisitor(visitor)
                .buildInstance();

        assertGetterValue(generated, "getValue").isEqualTo("template");
        assertGetterMethod(generated, "getTemplate").isEmpty();
    }

    @Test
    public void testMethodBodyVisitedToo() throws Exception {
        ChangeMethodNameVisitor visitor1 = new ChangeMethodNameVisitor("getTemplate", "getValue");
        ChangeConstantVisitor visitor2 = new ChangeConstantVisitor("template", "value");

        Object generated = new RuntimeGeneratedClassBuilder(TestCase_Template.class)
                .withVisitors(visitor1, visitor2)
                .buildInstance();

        assertGetterValue(generated, "getValue").isEqualTo("value");
    }

}
