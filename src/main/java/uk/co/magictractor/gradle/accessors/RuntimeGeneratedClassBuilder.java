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

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.internal.impldep.com.google.common.base.Strings;

import uk.co.magictractor.gradle.classfile.ChangeClassVisitor;
import uk.co.magictractor.gradle.classfile.ClassFileElementVisitorList;
import uk.co.magictractor.gradle.classfile.ClassFileTraversal;

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

        ClassFileElementVisitorList visitorList = new ClassFileElementVisitorList();

        ClassDesc templateClassDesc = ClassDesc.of(templateClass.getName());
        ClassDesc generatedClassDesc = ClassDesc.of(generatedClassName);
        visitorList.add(new ChangeClassVisitor(templateClassDesc, generatedClassDesc));

        byte[] binaryRepresentation = new ClassFileTraversal().visitClass(templateClass, generatedClassDesc, visitorList);

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

}
