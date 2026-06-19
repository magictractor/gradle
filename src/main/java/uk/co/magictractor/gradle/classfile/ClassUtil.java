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

import java.io.IOException;
import java.io.InputStream;

import org.gradle.internal.impldep.com.google.common.io.ByteStreams;

public final class ClassUtil {

    private ClassUtil() {
    }

    public static byte[] readClassBytes(Class<?> clazz) {
        String classResourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getResourceAsStream(classResourceName)) {
            return ByteStreams.toByteArray(in);
        }
        catch (IOException e) {
            throw new IllegalStateException("No .class file found for " + clazz.getName());
        }
    }

}
