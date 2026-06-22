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

import java.lang.classfile.ClassFileElement;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.quality.Preconditions;

import org.junit.jupiter.api.Test;

public class ClassUtilTest {

    @Test
    public void testToClassFileElementInterface() {
        ClassModel model = ClassUtil.parse(TestCase_Template.class);
        Class<? extends ClassFileElement> modelClass = model.getClass();

        // Should be an internal JDK Class that cannot be explictly referenced.
        Preconditions.checkArgument(!model.getClass().getName().startsWith("java.lang."));

        Class<?> actual = ClassUtil.toClassFileElementInterface(modelClass);
        assertThat(actual).isEqualTo(ClassModel.class);
    }

    @Test
    public void testToClassFileElementInterface_all() {
        Set<Class<?>> all = new HashSet<>();
        gatherClassFileElementInterfaces(ClassFileElement.class, new HashSet<>(), all);
        System.out.println(all.size());

        for (Class<?> elementImplType : all) {
            Class<?> actual = ClassUtil.toClassFileElementInterface((Class<? extends ClassFileElement>) elementImplType);
            assertThat(actual.getPackageName()).startsWith("java.lang.");
            // System.out.println(elementImplType + " -> " + actual);
        }
    }

    private void gatherClassFileElementInterfaces(Class<?> c, Set<Class<?>> done, Set<Class<?>> all) {
        if (done.contains(c)) {
            return;
        }
        done.add(c);

        if (c.isSealed()) {
            for (Class<?> subclass : c.getPermittedSubclasses()) {
                gatherClassFileElementInterfaces(subclass, done, all);
            }
            for (Class<?> inner : c.getDeclaredClasses()) {
                if (ClassFileElement.class.isAssignableFrom(inner)) {
                    gatherClassFileElementInterfaces(inner, done, all);
                }
            }
        }
        else {
            for (Class<?> inner : c.getDeclaredClasses()) {
                if (ClassFileElement.class.isAssignableFrom(inner)) {
                    throw new IllegalStateException("Inner ClassFileElement in unsealed class " + c.getName());
                }
            }
            if (Modifier.isAbstract(c.getModifiers())) {
                // For CustomAttribute and UnboundAttribute$AdHocAttribute.
                //System.out.println("Skipped " + c.getName());
            }
            else {
                boolean added = all.add(c);
                if (!added) {
                    throw new IllegalStateException();
                }
            }
        }
    }

}
