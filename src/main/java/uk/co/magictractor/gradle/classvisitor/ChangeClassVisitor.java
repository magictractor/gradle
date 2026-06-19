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
package uk.co.magictractor.gradle.classvisitor;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.constant.ClassDesc;

/**
 * Changes references to a specified {@code Class} to a different {@code Class}.
 */
public class ChangeClassVisitor implements ClassFileElementVisitor {

    private final ClassDesc fromClassDesc;
    private final ClassDesc toClassDesc;

    private ClassEntry _fromClassEntry;
    private ClassEntry _toClassEntry;

    public ChangeClassVisitor(ClassDesc fromClassDesc, ClassDesc toClassDesc) {
        this.fromClassDesc = fromClassDesc;
        this.toClassDesc = toClassDesc;
    }

    @Override
    public boolean acceptsElement(Class<? extends ClassFileElement> elementType) {
        // There could be more we haven't encountered yet...
        return FieldInstruction.class.isAssignableFrom(elementType)
                || InvokeInstruction.class.isAssignableFrom(elementType)
                || LocalVariable.class.isAssignableFrom(elementType);
    }

    @Override
    public ClassElement visitClassElement(ClassElement element, ClassBuilder codeBuilder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodElement visitMethodElement(MethodElement element, MethodBuilder codeBuilder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeElement visitCodeElement(CodeElement element, CodeBuilder codeBuilder) {
        ensureClassEntriesCreated(codeBuilder);

        CodeElement result = element;
        if (element instanceof FieldInstruction fi) {
            if (fi.field().owner().equals(_fromClassEntry)) {
                result = FieldInstruction.of(fi.opcode(), _toClassEntry, fi.field().nameAndType());
            }
        }
        else if (element instanceof InvokeInstruction ii) {
            if (ii.owner().equals(_fromClassEntry)) {
                result = InvokeInstruction.of(ii.opcode(), _toClassEntry, ii.method().nameAndType(), false);
            }
        }
        else if (element instanceof LocalVariable lv) {
            if (lv.typeSymbol().equals(_fromClassEntry)) {
                element = LocalVariable.of(lv.slot(), "this", toClassDesc, lv.startScope(), lv.endScope());
            }
        }
        else {
            throw new IllegalArgumentException("Unexpected visit to element of type " + element.getClass().getName());
        }

        if (result != element) {
            // logging?
        }

        return result;
    }

    private void ensureClassEntriesCreated(ClassFileBuilder<?, ?> builder) {
        if (_fromClassEntry == null) {
            _fromClassEntry = builder.constantPool().classEntry(fromClassDesc);
            _toClassEntry = builder.constantPool().classEntry(toClassDesc);
        }
    }

}
