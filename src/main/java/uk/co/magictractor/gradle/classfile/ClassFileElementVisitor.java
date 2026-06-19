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
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;

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
public interface ClassFileElementVisitor {

    /**
     * <p>
     * Returns a flag indicating whether this visitor should visit
     * {@code ClassFileElement}s of the given type. If this returns false, then
     * {@code ClassFileTraversal} will not call
     * {@link #visitClassElement()},{@link #visitMethodElement(MethodElement,
     * MethodBuilder()} or {@link #visitCodeElement()} with elements of the
     * given type.
     * </p>
     * <p>
     * This is used to find and cache short lists of applicable visitors rather
     * than looping through all visitors for every element.
     * </p>
     */
    boolean acceptsElement(Class<? extends ClassFileElement> elementType);

    /**
     * <p>
     * Returns given {@code ClassElement} if it is to be passed through without
     * modification and subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns a different instance if a change has been made to it and
     * subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns {@code null} if the {@code ClassElement} has already been handled
     * using {@link ClassBuilder#with()} or is to be discarded.
     * </p>
     */
    ClassElement visitClassElement(ClassElement element, ClassBuilder classBuilder);

    /**
     * <p>
     * Returns given {@code MethodElement} if it is to be passed through without
     * modification and subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns a different instance if a change has been made to it and
     * subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns {@code null} if the {@code MethodElement} has already been
     * handled using {@link MethodBuilder#with()} or is to be discarded.
     * </p>
     */
    MethodElement visitMethodElement(MethodElement element, MethodBuilder methodBuilder);

    /**
     * <p>
     * Returns given {@code CodeElement} if it is to be passed through without
     * modification and subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns a different instance if a change has been made to it and
     * subsequent {@code ClassFileElementVisitor}s applied.
     * </p>
     * <p>
     * Returns {@code null} if the {@code CodeElement} has already been handled
     * using {@link CodeBuilder#with()} or is to be discarded.
     * </p>
     */
    CodeElement visitCodeElement(CodeElement element, CodeBuilder codeBuilder);

}
