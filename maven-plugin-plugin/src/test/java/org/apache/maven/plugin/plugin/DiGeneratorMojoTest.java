/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiGeneratorMojoTest {

    @Test
    void testMojoGenerator() throws Exception {
        byte[] data = DescriptorGeneratorMojo.computeGeneratorClassBytes(
                "org.foo",
                "MojoGenerator",
                "the-mojo-full-id",
                MyMojo.class.getName().replace('.', '/'));

        CustomClassLoader cl = new CustomClassLoader(getClass().getClassLoader());
        Class<?> generator = cl.loadClassFromBytes(data);
        Object result = generator.getConstructor().newInstance();
        assertNotNull(result);
        assertInstanceOf(MyMojo.class, result);
    }

    static class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> loadClassFromBytes(byte[] classBytes) {
            return defineClass(null, classBytes, 0, classBytes.length); // Define the class
        }
    }

    public static class MyMojo implements org.apache.maven.api.plugin.Mojo {
        @Override
        public void execute() {}
    }
}
