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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.gradle.internal.impldep.com.google.common.base.Strings;
import org.gradle.internal.impldep.com.google.common.io.ByteStreams;

import uk.co.magictractor.gradle.classvisitor.ChangeClassVisitor;
import uk.co.magictractor.gradle.classvisitor.ClassFileElementVisitor;

/**
 * Builder that copies a given class, transforms it using
 * {@code ClassFile.transformClass()} and loads the new class via a custom
 * {@code ClassLoader}.
 */
public final class RuntimeGeneratedClassBuilder {

    private final AccessorClassLoader ACCESSOR_CLASS_LOADER = new AccessorClassLoader();

    private Class<?> templateClass;
    private String generatedClassName = "uk.co.magictractor.Play";

    public RuntimeGeneratedClassBuilder(Class<?> templateClass) {
        this.templateClass = templateClass;
    }

    public Class<?> buildClass() {
        try {
            return buildClass0();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private Class<?> buildClass0() throws IOException, ClassNotFoundException {
        byte[] templateClassBytes = readTemplateClassBytes0();

        VisitorLists visitorLists = new VisitorLists();

        ClassDesc templateClassDesc = ClassDesc.of(templateClass.getName());
        ClassDesc generatedClassDesc = ClassDesc.of(generatedClassName);
        visitorLists.visitors.add(new ChangeClassVisitor(templateClassDesc, generatedClassDesc));

        ClassModel classModel = ClassFile.of().parse(templateClassBytes);
        byte[] binaryRepresentation = ClassFile.of().transformClass(classModel, generatedClassDesc, (b, e) -> {
            visitClassElement(e, visitorLists, b);
        });

        // temp - check that a second pass compresses the constant pool, removing references to the template.
        // looks OK - create a unit test to verify and make second pass configurable
        // second pass would not need a transform, just a new ConstantPool.
        ClassModel secondPassModel = ClassFile.of().parse(binaryRepresentation);
        //binaryRepresentation = ClassFile.of(ConstantPoolSharingOption.NEW_POOL)
        //        .transformClass(secondPassModel, _generatedClassDesc, this::transformTemplateClass0);

        // temp - see what changed
        //System.out.println("----------------------");
        dump(ClassFile.of().parse(binaryRepresentation));

        return ACCESSOR_CLASS_LOADER.loadClass(generatedClassName, binaryRepresentation);
    }

    private byte[] readTemplateClassBytes0() throws IOException {
        String classResourceName = "/" + templateClass.getName().replace('.', '/') + ".class";
        try (InputStream in = templateClass.getResourceAsStream(classResourceName)) {
            return ByteStreams.toByteArray(in);
        }
    }

    private void visitClassElement(ClassElement classElement, VisitorLists visitorLists, ClassBuilder classBuilder) {
        ClassElement transformedElement = visit(classElement, visitorLists, (v, e) -> v.visitClassElement(e, classBuilder));
        if (transformedElement == null) {
            return;
        }
        else if (transformedElement instanceof MethodModel mm) {
            classBuilder.transformMethod(mm, (b, e) -> {
                visitMethodElement(e, visitorLists, b);
            });
        }
        else {
            classBuilder.with(transformedElement);
        }
    }

    private void visitMethodElement(MethodElement methodElement, VisitorLists visitorLists, MethodBuilder methodBuilder) {
        MethodElement transformedElement = visit(methodElement, visitorLists, (v, e) -> v.visitMethodElement(e, methodBuilder));
        if (transformedElement == null) {
            return;
        }
        else if (methodElement instanceof CodeModel cm) {
            methodBuilder.transformCode(cm, (b, e) -> {
                visitCodeElement(e, visitorLists, b);
            });
        }
    }

    private void visitCodeElement(CodeElement codeElement, VisitorLists visitorLists, CodeBuilder codeBuilder) {
        CodeElement transformedElement = visit(codeElement, visitorLists, (v, e) -> v.visitCodeElement(e, codeBuilder));
        if (transformedElement == null) {
            return;
        }
        else {
            codeBuilder.with(transformedElement);
        }
    }

    private <ELEMENT extends ClassFileElement> ELEMENT visit(ELEMENT element, VisitorLists visitorLists, BiFunction<ClassFileElementVisitor, ELEMENT, ELEMENT> visitFunction) {
        List<ClassFileElementVisitor> elementVisitors = visitorLists.getVisitorList(element.getClass());
        if (elementVisitors.isEmpty()) {
            // Quick exit for common case.
            return element;
        }

        ELEMENT result = element;
        for (ClassFileElementVisitor elementVisitor : elementVisitors) {
            // result = elementTransform.visit(result, ctx);
            result = visitFunction.apply(elementVisitor, result);
            if (result == null) {
                return null;
            }
        }

        // TODO! check that the type does not get changed by a visitor.

        return result;
    }

    // TEMP - create a DumpTransform (suggests Transform might not be the best name - we might just traverse)
    private void dump(ClassModel classModel) {
        for (PoolEntry poolEntry : classModel.constantPool()) {
            // toStrings() for the pool are scruffy
            System.out.println(poolEntry);
        }
        dump0(0, classModel);
    }

    private void dump0(int indentSize, ClassFileElement element) {
        System.out.print(Strings.repeat("  ", indentSize));
        System.out.println(element);
        if (element instanceof Iterable) {
            Iterable<? extends ClassFileElement> iterableElement = (Iterable<? extends ClassFileElement>) element;
            for (ClassFileElement subElement : iterableElement) {
                dump0(indentSize + 1, subElement);
            }
        }
    }

    private static class VisitorLists {
        private List<ClassFileElementVisitor> visitors = new ArrayList<>();
        // No key means not checked, empty list for checked and has no visitors.
        private Map<Class<? extends ClassFileElement>, List<ClassFileElementVisitor>> visitorsForElement = new HashMap<>();

        private List<ClassFileElementVisitor> getVisitorList(Class<? extends ClassFileElement> elementType) {
            List<ClassFileElementVisitor> result = visitorsForElement.get(elementType);
            if (result == null) {
                result = visitors.stream().filter(t -> t.acceptsElement(elementType)).toList();
                visitorsForElement.put(elementType, result);
            }
            return result;
        }
    }

}
