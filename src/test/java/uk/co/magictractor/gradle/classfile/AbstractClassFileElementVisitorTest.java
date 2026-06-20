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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.OptionalAssert;

import uk.co.magictractor.gradle.accessors.RuntimeGeneratedClassBuilder;

public class AbstractClassFileElementVisitorTest {

    protected <T> T generate(Class<?> templateClass, Object... constructorParameters) {
        RuntimeGeneratedClassBuilder builder = new RuntimeGeneratedClassBuilder(templateClass);
        return (T) builder.buildInstance(constructorParameters);
    }

    protected ObjectAssert<Object> assertGetterValue(Object generatedObject, String getterName) throws ReflectiveOperationException {
        Method getter = generatedObject.getClass().getMethod(getterName);
        Object actual = getter.invoke(generatedObject);

        return assertThat(actual);
    }

    protected ObjectAssert<Object> assertFieldValue(Object generatedObject, String fieldName) throws ReflectiveOperationException {
        Field field = generatedObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object actual = field.get(generatedObject);

        return assertThat(actual);
    }

    protected ObjectAssert<Object> assertStaticFieldValue(Class<?> generatedClass, String fieldName) throws ReflectiveOperationException {
        Field field = generatedClass.getField(fieldName);
        Object actual = field.get(generatedClass);

        return assertThat(actual);
    }

    protected OptionalAssert<Method> assertGetterMethod(Object generatedObject, String getterName) throws ReflectiveOperationException {
        return assertThat(getGetterOptional(generatedObject, getterName));
    }

    protected Optional<Method> getGetterOptional(Object generatedObject, String getterName) {
        try {
            return Optional.of(generatedObject.getClass().getMethod(getterName));
        }
        catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

}
