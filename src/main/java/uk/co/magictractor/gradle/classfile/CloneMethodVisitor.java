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
import java.util.Collection;
import java.util.function.Function;

/**
 * Clones multiple copies of a method.
 */
public class CloneMethodVisitor implements ClassFileElementVisitor {

    private final String templateMethodName;
    private final Collection<String> cloneMethodNames;
    private final Function<String, ClassFileElementVisitor> cloneVisitorFunction;

    public <T extends ConstantDesc> CloneMethodVisitor(String templateMethodName, Collection<String> cloneMethodNames,
            Function<String, ClassFileElementVisitor> cloneVisitorFunction) {
        this.templateMethodName = templateMethodName;
        this.cloneMethodNames = cloneMethodNames;
        this.cloneVisitorFunction = cloneVisitorFunction;
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
        if (!templateMethodName.equals(parameters.name().stringValue())) {
            return parameters;
        }

        for (String cloneMethodName : cloneMethodNames) {
            ClassFileElementVisitor cloneVisitor = cloneVisitorFunction.apply(cloneMethodName);
            Utf8Entry cloneMethodNameEntry = classBuilder.constantPool().utf8Entry(cloneMethodName);
            ClassFileTraversal.visitMethod(cloneMethodNameEntry, parameters.descriptor(), parameters.flags(), parameters.elements(), cloneVisitor, classBuilder);
        }

        // null so template is discarded
        return null;
    }

}
