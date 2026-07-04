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
package uk.co.magictractor.classy;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassFileElement;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ClassFileElementVisitorList implements ClassFileElementVisitor {

    private final List<ClassFileElementVisitor> visitors = new ArrayList<>();
    // No key means not checked, empty list for checked and has no visitors.
    private final Map<Class<? extends ClassFileElement>, List<ClassFileElementVisitor>> visitorsForElement = new HashMap<>();

    public void add(ClassFileElementVisitor visitor) {
        visitors.add(visitor);
    }

    public void addAll(List<ClassFileElementVisitor> visitors) {
        this.visitors.addAll(visitors);
    }

    @Override
    public boolean acceptsElementType(Class<? extends ClassFileElement> elementType) {
        for (ClassFileElementVisitor visitor : visitors) {
            if (visitor.acceptsElementType(elementType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ClassElement visitClassElement(ClassElement element, ClassBuilder classBuilder) {
        return visit(element, (v, e) -> v.visitClassElement(e, classBuilder));
    }

    @Override
    public MethodModelParameters visitMethodModelParameters(MethodModelParameters parameters, ClassBuilder classBuilder) {
        List<ClassFileElementVisitor> elementVisitors = getVisitorList(MethodModel.class);
        if (elementVisitors.isEmpty()) {
            return parameters;
        }

        MethodModelParameters result = parameters;
        for (ClassFileElementVisitor elementVisitor : elementVisitors) {
            result = elementVisitor.visitMethodModelParameters(parameters, classBuilder);
            if (result == null) {
                return null;
            }
        }

        return result;
    }

    @Override
    public MethodElement visitMethodElement(MethodElement element, MethodBuilder methodBuilder) {
        return visit(element, (v, e) -> v.visitMethodElement(e, methodBuilder));
    }

    @Override
    public FieldElement visitFieldElement(FieldElement element, FieldBuilder fieldBuilder) {
        return visit(element, (v, e) -> v.visitFieldElement(e, fieldBuilder));
    }

    @Override
    public CodeElement visitCodeElement(CodeElement element, CodeBuilder codeBuilder) {
        return visit(element, (v, e) -> v.visitCodeElement(e, codeBuilder));
    }

    private <E extends ClassFileElement, B extends ClassFileBuilder<E, B>> E visit(E element, BiFunction<ClassFileElementVisitor, E, E> visitFunction) {
        //        List<ClassFileElementVisitor> elementVisitors = visitorsForElement.get(element.getClass());
        //        if (elementVisitors == null || elementVisitors.isEmpty()) {
        //            //return element;
        //            throw new IllegalStateException("Call to visit() unexpected because acceptsElement() should have returned false for type " + element.getClass());
        //        }

        // TODO! when ClassFileTraversale has a single entry point ensure it calls accept() then the code directly above may be restored.
        List<ClassFileElementVisitor> elementVisitors = getVisitorList(element.getClass());
        if (elementVisitors.isEmpty()) {
            return element;
        }

        E result = element;
        for (ClassFileElementVisitor elementVisitor : elementVisitors) {
            result = visitFunction.apply(elementVisitor, result);
            if (result == null) {
                return null;
            }
        }

        // TODO! check that the type does not get changed by a visitor.

        return result;
    }

    private List<ClassFileElementVisitor> getVisitorList(Class<? extends ClassFileElement> elementType) {
        List<ClassFileElementVisitor> result = visitorsForElement.get(elementType);
        if (result == null) {
            result = visitors.stream().filter(t -> t.acceptsElementType(elementType)).toList();
            visitorsForElement.put(elementType, result);
        }
        return result;
    }

}
