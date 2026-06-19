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
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;

public class ClassFileTraversal {

    public byte[] visitClass(Class<?> traversalClass, ClassDesc generatedClassDesc, ClassFileElementVisitor visitor) {
        byte[] traversalClassBytes = ClassUtil.readClassBytes(traversalClass);
        ClassModel traversalClassModel = ClassFile.of().parse(traversalClassBytes);
        // TODO! ClassFile.of() can take options. Specifically, a second pass would need a new constant pool in order to discard redundant values.
        byte[] binaryRepresentation = ClassFile.of().transformClass(traversalClassModel, generatedClassDesc, (b, e) -> {
            visitClassElement(e, visitor, b);
        });

        return binaryRepresentation;
    }

    private void visitClassElement(ClassElement classElement, ClassFileElementVisitor visitor, ClassBuilder classBuilder) {
        ClassElement transformedElement = classElement;
        if (visitor.acceptsElementType(classElement.getClass())) {
            transformedElement = visitor.visitClassElement(classElement, classBuilder);
            if (transformedElement == null) {
                return;
            }
        }

        if (transformedElement instanceof MethodModel mm) {
            classBuilder.transformMethod(mm, (b, e) -> {
                visitMethodElement(e, visitor, b);
            });
        }
        else if (transformedElement instanceof FieldModel fm) {
            classBuilder.transformField(fm, (b, e) -> {
                visitFieldElement(e, visitor, b);
            });
        }
        else if (transformedElement instanceof Iterable) {
            throw new IllegalStateException("visitClassElement() should maybe handle Iterable element " + transformedElement.getClass().getName());
        }
        else {
            classBuilder.with(transformedElement);
        }
    }

    public void visitFieldElement(FieldElement fieldElement, ClassFileElementVisitor visitor, FieldBuilder fieldBuilder) {
        FieldElement transformedElement = fieldElement;
        if (visitor.acceptsElementType(fieldElement.getClass())) {
            transformedElement = visitor.visitFieldElement(fieldElement, fieldBuilder);
            if (transformedElement == null) {
                return;
            }
        }

        fieldBuilder.with(transformedElement);
    }

    public void visitMethodElement(MethodElement methodElement, ClassFileElementVisitor visitor, MethodBuilder methodBuilder) {
        MethodElement transformedElement = methodElement;
        if (visitor.acceptsElementType(methodElement.getClass())) {
            transformedElement = visitor.visitMethodElement(methodElement, methodBuilder);
            if (transformedElement == null) {
                return;
            }
        }

        if (transformedElement instanceof CodeModel cm) {
            methodBuilder.transformCode(cm, (b, e) -> {
                visitCodeElement(e, visitor, b);
            });
        }
        else {
            methodBuilder.with(transformedElement);
        }
    }

    private void visitCodeElement(CodeElement codeElement, ClassFileElementVisitor visitor, CodeBuilder codeBuilder) {
        CodeElement transformedElement = codeElement;
        if (visitor.acceptsElementType(codeElement.getClass())) {
            transformedElement = visitor.visitCodeElement(codeElement, codeBuilder);
            if (transformedElement == null) {
                return;
            }
        }

        codeBuilder.with(transformedElement);
    }

}
