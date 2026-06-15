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

import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.constant.ConstantDescs.CD_void;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFile.LineNumbersOption;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import java.util.function.Function;

import org.gradle.api.UncheckedIOException;
import org.gradle.internal.impldep.com.google.common.io.ByteStreams;

/**
 * Builder that copies a given class, transforms it using
 * {@code ClassFile.transformClass()} and loads the new class via a custom
 * {@code ClassLoader}.
 */
public final class GeneratedClassBuilder<T> {

    private final AccessorClassLoader ACCESSOR_CLASS_LOADER = new AccessorClassLoader();

    private ClassModel templateClassModel;

    // TODO! restore final
    //private final Class<T> accessorsForClass;
    private Class<T> accessorsForClass;
    private T accessorsForInstance;
    private String accessorsClassName = "uk.co.magictractor.Play";
    private byte[] templateClassBytes;

    /**
     * If {@code false} then accessors are to be added directly to an existing
     * class. Expected to be non-null when {@code build()} is called depending
     * on the methods called.
     */
    // Looks like it will have to be a new Class, unless I can persuade to custom ClassLoader
    // to change the Class. It think ByteBuddy/ASM or something was able to modify existing classes?
    //private Boolean createNewAccessorClass;

    // temp for viewing bytecode
    public GeneratedClassBuilder() {
    }

    public GeneratedClassBuilder(Class<T> accessorsForClass) {
        this.accessorsForClass = accessorsForClass;

        String classResourceName = "/" + accessorsForClass.getName().replace('.', '/') + ".class";
        try (InputStream in = accessorsForClass.getResourceAsStream(classResourceName)) {
            //if (in == null) {
            //    throw new IllegalStateException("Class resource not found: " + classResourceName);
            //}

            //ByteSource.
            byte[] templateClassBytes = ByteStreams.toByteArray(in);
            // TODO! other options might be useful too
            templateClassModel = ClassFile.of(LineNumbersOption.DROP_LINE_NUMBERS).parse(templateClassBytes);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<MethodModel> templateMethods = templateClassModel.methods()
                .stream()
                // maybe an annotation? pass in a Predicate?
                .filter(mm -> mm.methodName().stringValue().startsWith("__"))
                .toList();

        // TODO! want the line numbers now?
        // Different options here, keep the line numbers this time.
        ClassFile.of().transformClass(templateClassModel, this::transformTemplateClass);
    }

    private void transformTemplateClass(ClassBuilder builder, ClassElement element) {
        // TODO! MethodElement match - as above
        if (element instanceof MethodElement && ((MethodElement) element) == null) {
            // TODO! insert substitutions instead
        }
        else {
            builder.with(element);
        }
    }

    public GeneratedClassBuilder withAccessorsFor(T accessorsForInstance) {
        if (this.accessorsForInstance != null) {
            throw new IllegalStateException();
        }

        this.accessorsForInstance = accessorsForInstance;
        return this;
    }

    // https://blog.rasc.ch/2026/03/classfileapi.html
    public Object build() {

        ClassDesc classWithAccessors = ClassDesc.of(accessorsClassName);

        // byte[] bytes = ClassFile.of(classWithAccessors)
        // java.lan

        byte[] binaryRepresentation = ClassFile.of().build(classWithAccessors, this::buildClass);

        // getClass().getClassLoader().define

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Class<?> accessorClass = ACCESSOR_CLASS_LOADER.loadClass(accessorsClassName, binaryRepresentation);
            return accessorClass.getConstructor().newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    //    // access flags 0x1
    //    public <init>()V
    //     L0
    //      LINENUMBER 36 L0
    //      ALOAD 0
    //      INVOKESPECIAL java/lang/Object.<init>()V
    //      RETURN
    //     L1
    //      LOCALVARIABLE this Lorg/apache/batik/ext/awt/image/codec/imageio/ImageIODebugUtil; L0 L1 0
    //      MAXSTACK = 1
    //      MAXLOCALS = 1
    private void buildClass(ClassBuilder classBuilder) {
        classBuilder.withFlags(ACC_PUBLIC);
        classBuilder.withMethodBody("<init>", MethodTypeDesc.of(CD_void), ACC_PUBLIC, codeBuilder -> {
            codeBuilder
                    .aload(0)
                    .invokespecial(ClassDesc.of(Object.class.getName()), "<init>", MethodTypeDesc.of(CD_void))
                    .return_();
        });
    }

    /**
     * Creates an accessor method an adds it to the given class.
     */
    public static <T, ACCESSOR> void addAccessorMethod(String name, Class<T> clazz, Class<ACCESSOR> accessorType, Function<String, ACCESSOR> accessorFunction) {
        String capitalisedName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String methodName = "get" + capitalisedName;

        MethodTypeDesc d = MethodTypeDesc.of(ClassDesc.of(accessorType.getName()));
    }

}
