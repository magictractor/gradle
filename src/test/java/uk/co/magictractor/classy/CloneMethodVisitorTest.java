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
package uk.co.magictractor.classy;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import uk.co.magictractor.classy.ChangeConstantVisitor;
import uk.co.magictractor.classy.ClassFileElementVisitor;
import uk.co.magictractor.classy.CloneMethodVisitor;
import uk.co.magictractor.classy.RuntimeGeneratedClassBuilder;

public class CloneMethodVisitorTest extends AbstractClassFileElementVisitorTest {

    @Test
    public void testMethodNameChanged() throws Exception {
        List<String> clonedMethodNames = List.of("getOne", "getTwo", "getThree");
        Function<String, ClassFileElementVisitor> clonedMethodVisitorFunction = (methodName) -> {
            return new ChangeConstantVisitor("template", methodName + "Value");
        };
        CloneMethodVisitor visitor = new CloneMethodVisitor("getTemplate", clonedMethodNames, clonedMethodVisitorFunction);

        Object generated = new RuntimeGeneratedClassBuilder(TestCase_Template.class)
                .withVisitor(visitor)
                .buildInstance();

        assertGetterValue(generated, "getOne").isEqualTo("getOneValue");
        assertGetterValue(generated, "getTwo").isEqualTo("getTwoValue");
        assertGetterValue(generated, "getThree").isEqualTo("getThreeValue");
        assertGetterMethod(generated, "getTemplate").isEmpty();
    }

}
