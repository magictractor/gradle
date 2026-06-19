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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

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

    public <T> T buildInstance(Object... constructorParameters) {
        Class<T> builtClass = (Class<T>) buildClass();
        int parameterCount = constructorParameters.length;
        List<Constructor<?>> constructors = Stream.of(builtClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == parameterCount)
                .toList();

        // A long time ago I created a reflection util that would go further and
        // match parameter types.
        // TODO! find my old code. Maybe in a forgotten Bitbucket account. Best best would be to trawl
        // old hard drives. Ah - maybe on server that's still functional but unused?
        if (constructors.size() != 1) {
            StringBuilder msgBuilder = new StringBuilder(64);
            msgBuilder.append("No public constructors have ");
            if (constructors.isEmpty()) {
                msgBuilder.append(" no parameters.");
            }
            else {
                msgBuilder.append(parameterCount);
                msgBuilder.append(" parameters.");
            }

            throw new IllegalArgumentException(msgBuilder.toString());
        }

        try {
            return (T) constructors.get(0).newInstance(constructorParameters);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    public Class<?> buildClass() {
        byte[] binaryRepresentation = buildBytes();
        try {
            return ACCESSOR_CLASS_LOADER.loadClass(generatedClassName, binaryRepresentation);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] buildBytes() {
        byte[] templateClassBytes = readClassBytes(templateClass);

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
        //dump(ClassFile.of().parse(binaryRepresentation));

        return binaryRepresentation;
    }

    // could be moved to a static util
    private byte[] readClassBytes(Class<?> clazz) {
        String classResourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getResourceAsStream(classResourceName)) {
            return ByteStreams.toByteArray(in);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("No .class file found for " + clazz.getName());
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
