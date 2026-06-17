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
import java.lang.classfile.ClassFile.ConstantPoolSharingOption;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.constant.ClassDesc;

import org.gradle.internal.impldep.com.google.common.base.Strings;
import org.gradle.internal.impldep.com.google.common.io.ByteStreams;

/**
 * Builder that copies a given class, transforms it using
 * {@code ClassFile.transformClass()} and loads the new class via a custom
 * {@code ClassLoader}.
 */
public final class RuntimeGeneratedClassBuilder {

    private final AccessorClassLoader ACCESSOR_CLASS_LOADER = new AccessorClassLoader();

    private Class<?> templateClass;
    private String generatedClassName = "uk.co.magictractor.Play";

    // User defined using withXxx() methods.
    private String templateClassType;
    //private String generatedClassType;

    // Created when building. These might get moved to a context (or something)?
    private transient ClassDesc _generatedClassDesc;
    // ClassEntry describes an entry in the constant pool.
    // Instructions using the templateClassEntry need to be changed to use the generatedClassEntry.
    private transient ClassEntry _generatedClassEntry;
    private transient ClassDesc _templateClassDesc;
    private transient ClassEntry _templateClassEntry;

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

    // Transforms required:
    //
    // Change constants in code
    //   LoadConstant[OP=LDC, val=mockito]
    //
    // Change method name
    //   MethodModel[methodName=getTemplate, methodType=()Ljava/lang/Object;, flags=1]
    //
    // Maybe add synthetic flag to class and generated methods
    //  MethodModel[methodName=get, methodType=(Ljava/lang/String;)Ljava/lang/Object;, flags=1]
    //
    // Add marker interface with const that points to docs. (configurable)
    //   Interfaces[interfaces=]
    //
    // Write info to writer (similar to initial printlns)
    // Add java version for ClassFileVersion[majorVersion=69, minorVersion=0]
    // Add desc for AccessFlags[flags=33]
    private Class<?> buildClass0() throws IOException, ClassNotFoundException {
        // Don't want leading "L" or trailing semicolon. Those might be needed need to be added in some contexts.
        templateClassType = templateClass.getName().replace('.', '/');
        //generatedClassType = generatedClassName.replace('.', '/');

        byte[] templateClassBytes = readTemplateClassBytes0();
        //templateClassModel = ClassFile.of(LineNumbersOption.DROP_LINE_NUMBERS).parse(templateClassBytes);
        // will want to drop line numbers in generated methods, but not the whole file?

        // ClassModel is sealed, so cannot wrap it for shennanigans
        // ConstantPoolBuilder is sealed too...
        ClassModel templateClassModel = ClassFile.of().parse(templateClassBytes);

        _templateClassDesc = ClassDesc.of(templateClass.getName());
        _generatedClassDesc = ClassDesc.of(generatedClassName);

        dump(templateClassModel);
        System.out.println();

        // transformClass() does not include the ClassModel, instead iterating over its elements
        System.out.println(templateClassModel);

        // TODO! NEW_POOL better?
        // Note that transformClass() delegates to build(), that might enable more control,
        // perhaps to prevent the template appearing in the constant pool
        // or to set templateClassEntry once up front.
        //
        // OR there are many methods on ClassTransform that could be overridden... and builder is using ClassTransform anyway...
        //
        // OR ClassRemapper? Looks to heavyweight for this case.
        byte[] binaryRepresentation = ClassFile.of(ConstantPoolSharingOption.NEW_POOL)
                .transformClass(templateClassModel, _generatedClassDesc, this::transformTemplateClass0);
        //                .transformClass(templateClassModel, _generatedClassDesc, new ClassTransform() {
        //                    @Override
        //                    public void accept(ClassBuilder builder, ClassElement element) {
        //                        transformTemplateClass0(builder, element);
        //                        //this.con
        //                    }
        //                });

        //        default byte[] transformClass(ClassModel model, ClassDesc newClassName, ClassTransform transform) {
        //            return transformClass(model, TemporaryConstantPool.INSTANCE.classEntry(newClassName), transform);
        //        }

        //        @Override
        //        public byte[] transformClass(ClassModel model, ClassEntry newClassName, ClassTransform transform) {
        //            ConstantPoolBuilder constantPool = sharedConstantPool() ? ConstantPoolBuilder.of(model)
        //                                                                    : ConstantPoolBuilder.of();
        //            return build(newClassName, constantPool,
        //                    new Consumer<ClassBuilder>() {
        //                        @Override
        //                        public void accept(ClassBuilder builder) {
        //                            ((DirectClassBuilder) builder).setOriginal((ClassImpl)model);
        //                            ((DirectClassBuilder) builder).setSizeHint(((ClassImpl)model).classfileLength());
        //                            builder.transform((ClassImpl)model, transform);
        //                        }
        //                    });
        //        }

        //        default byte[] build(ClassDesc thisClass,
        //                Consumer<? super ClassBuilder> handler) {
        //ConstantPoolBuilder pool = ConstantPoolBuilder.of();
        //return build(pool.classEntry(thisClass), pool, handler);
        //}
        // Takes ClassBuilder rather than ClassTransform, uses a new Pool
        //ClassFile.of().build(_generatedClassDesc, this::blah);

        // Not using ClassFile.transformClass() because we also want to transform "this" in the constant pool.
        // It seems strange that transformClass() so not do so.
        // transformClass() also delegates to build().
        // This also MORE...

        // temp - check that a second pass compresses the constant pool, removing references to the template.
        // looks OK - create a unit test to verify and make second pass configurable
        // second pass would not need a transform, just a new ConstantPool.
        ClassModel secondPassModel = ClassFile.of().parse(binaryRepresentation);
        //binaryRepresentation = ClassFile.of(ConstantPoolSharingOption.NEW_POOL)
        //        .transformClass(secondPassModel, _generatedClassDesc, this::transformTemplateClass0);

        // temp - see what changed
        System.out.println("----------------------");
        dump(ClassFile.of().parse(binaryRepresentation));

        return ACCESSOR_CLASS_LOADER.loadClass(generatedClassName, binaryRepresentation);
    }

    private byte[] readTemplateClassBytes0() throws IOException {
        String classResourceName = "/" + templateClass.getName().replace('.', '/') + ".class";
        try (InputStream in = templateClass.getResourceAsStream(classResourceName)) {
            return ByteStreams.toByteArray(in);
        }
    }

    private void transformTemplateClass0(ClassBuilder builder, ClassElement element) {
        if (element instanceof SourceFileAttribute attr) {
            System.out.println("  " + element + "  {sourceFile=" + attr.sourceFile() + "}");
        }
        else if (element instanceof java.lang.classfile.Attribute attr) {
            System.out.println("  " + element + "  (" + attr.getClass().getSimpleName() + ")");
        }
        else {
            System.out.println("  " + element);
        }

        if (element instanceof MethodModel mm) {
            builder.withMethod(mm.methodName(), mm.methodType(), mm.flags().flagsMask(), mb -> {
                for (MethodElement me : mm) {
                    transformTemplateMethod0(mb, me);
                }
            });
        }
        else {
            builder.with(element);
        }
    }

    private void transformTemplateMethod0(MethodBuilder builder, MethodElement element) {
        System.out.println("    " + element);

        if (element instanceof CodeModel cm) {
            builder.withCode(cb -> {
                for (CodeElement ce : cm) {
                    transformTemplateCode0(cb, ce);
                }
            });
        }
        else {
            builder.with(element);
        }
    }

    private void transformTemplateCode0(CodeBuilder builder, CodeElement element) {
        String indent = "      ";
        String indentPlus = indent + "  > ";
        System.out.println(indent + element);

        if (element instanceof FieldInstruction fi) {
            if (isTemplate(fi.field().owner(), builder)) {
                FieldRefEntry f = builder.constantPool().fieldRefEntry(_generatedClassDesc, fi.name().stringValue(), fi.typeSymbol());
                element = FieldInstruction.of(fi.opcode(), _generatedClassEntry, fi.name(), fi.type());
                System.out.println(indentPlus + element);
            }
        }
        else if (element instanceof InvokeInstruction ii) {
            if (isTemplate(ii.owner(), builder)) {
                element = InvokeInstruction.of(ii.opcode(), _generatedClassEntry, ii.name(), ii.type(), false);
                System.out.println(indentPlus + element);
            }
        }
        else if (element instanceof LocalVariable lv) {
            // Check type too in case the code is run again to display the class post-translation.
            // type() has leading "L" and trailing semicolon.
            if (lv.name().equalsString("this") && lv.typeSymbol().equals(_templateClassDesc)) {
                ensureGeneratedClassEntry(builder);
                element = LocalVariable.of(lv.slot(), "this", _generatedClassDesc, lv.startScope(), lv.endScope());
                System.out.println(indentPlus + element);
            }
        }

        // TEMP
        if (element.toString().contains("_Template")) {
            throw new IllegalStateException("Code needs modification to transform " + element);
        }

        builder.with(element);
        // and maybe  Attribute[name=SourceFile]  {sourceFile=MapAccessor_Template.java}
    }

    private boolean isTemplate(ClassEntry classEntry, CodeBuilder builder) {
        if (_templateClassEntry == null) {
            ConstantPoolBuilder constantPoolBuilder = builder.constantPool();
            Utf8Entry utf8 = constantPoolBuilder.utf8Entry(templateClassType);
            _templateClassEntry = constantPoolBuilder.classEntry(utf8);
            _generatedClassEntry = builder.constantPool().classEntry(_generatedClassDesc);
        }
        return classEntry.equals(_templateClassEntry);
    }

    private void ensureGeneratedClassEntry(CodeBuilder builder) {
        if (_generatedClassEntry == null) {
            _generatedClassEntry = builder.constantPool().classEntry(_generatedClassDesc);
        }
    }

    // TEMP
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
