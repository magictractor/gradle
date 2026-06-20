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

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ConstantDesc;
import java.util.HashMap;
import java.util.Map;

/**
 * Changes references to a specified {@code Class} to a different {@code Class}.
 *
 * @see https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-6.html#jvms-6.5.ldc
 */
public class ChangeMethodNameVisitor implements ClassFileElementVisitor {

    private Map<String, String> methodNameMap = new HashMap<>();

    public <T extends ConstantDesc> ChangeMethodNameVisitor(String from, String to) {
        withMapping(from, to);
    }

    public <T extends ConstantDesc> ChangeMethodNameVisitor withMapping(String from, String to) {
        methodNameMap.put(from, to);
        return this;
    }

    @Override
    public boolean acceptsElementType(Class<? extends ClassFileElement> elementType) {
        return MethodModel.class.isAssignableFrom(elementType);
    }

    @Override
    public ClassElement visitClassElement(ClassElement element, ClassBuilder classBuilder) {
        // Cannot modify the MethodModel here, hence visitMethodModelParameters().
        return element;
    }

    @Override
    public MethodModelParameters visitMethodModelParameters(MethodModelParameters parameters, ClassBuilder classBuilder) {
        if (!methodNameMap.containsKey(parameters.name().stringValue())) {
            return parameters;
        }

        Utf8Entry newNameEntry = classBuilder.constantPool().utf8Entry(methodNameMap.get(parameters.name().stringValue()));
        return parameters.withMethodName(newNameEntry);
    }

}
