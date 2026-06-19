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
package uk.co.magictractor.gradle.accessors;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class RuntimeGeneratedClassBuilderTest {

    private static final String TEMPLATE_VALUE = "example.org:template:1.2.3";

    private static final Map<String, String> MAP = Map.of("template", TEMPLATE_VALUE);

    @Test
    public void t() throws Exception {
        // Class<?> templateClass = TestCase_Template.class;
        Class<?> templateClass = MapAccessor_Template.class;

        RuntimeGeneratedClassBuilder builder = new RuntimeGeneratedClassBuilder(templateClass);

        Object generatedInstance = builder.buildInstance(MAP);

        assertThat(generatedInstance.getClass().getName()).isEqualTo("uk.co.magictractor.Play");
    }

    private <T> T reflectiveGet(Object object, String getterName) throws ReflectiveOperationException {
        Method getter = object.getClass().getMethod(getterName);
        return (T) getter.invoke(object);
    }

}
