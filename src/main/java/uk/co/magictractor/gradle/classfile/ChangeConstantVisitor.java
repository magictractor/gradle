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

import java.lang.classfile.ClassFileElement;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.constantpool.ConstantValueEntry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.constant.ConstantDesc;
import java.util.HashMap;
import java.util.Map;

/**
 * Changes references to a specified {@code Class} to a different {@code Class}.
 *
 * @see https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-6.html#jvms-6.5.ldc
 */
public class ChangeConstantVisitor implements ClassFileElementVisitor {

    private Map<ConstantDesc, ConstantDesc> valueMap = new HashMap<>();

    public <T extends ConstantDesc> ChangeConstantVisitor(T from, T to) {
        withMapping(from, to);
    }

    public <T extends ConstantDesc> ChangeConstantVisitor withMapping(T from, T to) {
        valueMap.put(from, to);
        return this;
    }

    @Override
    public boolean acceptsElementType(Class<? extends ClassFileElement> elementType) {
        // There could be more we haven't encountered yet...
        return ConstantInstruction.class.isAssignableFrom(elementType)
                || ConstantValueAttribute.class.isAssignableFrom(elementType);
    }

    @Override
    public FieldElement visitFieldElement(FieldElement element, FieldBuilder fieldBuilder) {
        FieldElement result = element;
        if (element instanceof ConstantValueAttribute cva) {
            if (valueMap.containsKey(cva.constant().constantValue())) {
                result = ConstantValueAttribute.of(valueMap.get(cva.constant().constantValue()));
            }
        }
        else {
            throw new IllegalStateException();
        }

        return result;
    }

    @Override
    public CodeElement visitCodeElement(CodeElement element, CodeBuilder codeBuilder) {
        CodeElement result = element;
        if (element instanceof LoadConstantInstruction lci) {
            if (valueMap.containsKey(lci.constantValue())) {
                ConstantValueEntry newValueConstantEntry = codeBuilder.constantPool().constantValueEntry(valueMap.get(lci.constantValue()));
                result = ConstantInstruction.ofLoad(lci.opcode(), newValueConstantEntry);
            }
        }
        else {
            throw new IllegalStateException("Code needs modification to handle instruction of type " + element.getClass().getName());
        }

        return result;
    }

}
