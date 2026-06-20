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

import java.lang.classfile.AccessFlags;
import java.lang.classfile.CompoundElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.Utf8Entry;

public class MethodModelParameters {

    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final AccessFlags flags;
    private final CompoundElement<MethodElement> elements;

    public MethodModelParameters(Utf8Entry name, Utf8Entry descriptor, AccessFlags flags, CompoundElement<MethodElement> elements) {
        this.name = name;
        this.descriptor = descriptor;
        this.flags = flags;
        this.elements = elements;
    }

    public MethodModelParameters(MethodModel methodModel) {
        this.name = methodModel.methodName();
        this.descriptor = methodModel.methodType();
        this.flags = methodModel.flags();
        this.elements = methodModel;
    }

    public Utf8Entry name() {
        return name;
    }

    public Utf8Entry descriptor() {
        return descriptor;
    }

    public AccessFlags flags() {
        return flags;
    }

    public CompoundElement<MethodElement> elements() {
        return elements;
    }

    public MethodModelParameters withMethodName(Utf8Entry newName) {
        return new MethodModelParameters(newName, descriptor, flags, elements);
    }

}
