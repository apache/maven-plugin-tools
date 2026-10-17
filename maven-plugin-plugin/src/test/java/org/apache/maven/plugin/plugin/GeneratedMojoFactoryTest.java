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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the factory generated for Maven 4 API mojos, using mojo classes built with ASM so that the Maven 4 API
 * does not need to be on the test classpath.
 */
class GeneratedMojoFactoryTest {
    private static final String INJECT = "Lorg/apache/maven/api/di/Inject;";
    private static final String NAMED = "Lorg/apache/maven/api/di/Named;";
    private static final String MOJO = "org.example.TestMojo";

    @Test
    void factoryPassesInjectConstructorArgumentsToMojo() throws Exception {
        Class<?> factory = loadFactory(mojoClass(constructor(
                "(Ljava/lang/String;JLjava/util/List;)V",
                "(Ljava/lang/String;JLjava/util/List<Ljava/lang/String;>;)V",
                true)));

        Constructor<?> constructor = factory.getConstructor(String.class, long.class, List.class);
        assertEquals("java.util.List<java.lang.String>", constructor.getGenericParameterTypes()[2].getTypeName());

        Object mojo = constructor.newInstance("name", 42L, Collections.singletonList("item"));
        assertEquals("name", field(mojo, "p0"));
        assertEquals(42L, field(mojo, "p1"));
        assertEquals(Collections.singletonList("item"), field(mojo, "p2"));
    }

    @Test
    void factoryConstructorKeepsInjectAndParameterAnnotations() throws Exception {
        byte[] factory = generateFactory(
                mojoClass(constructor("(Ljava/lang/String;Ljava/lang/String;)V", null, true, null, "second")));

        assertEquals(Arrays.asList("@" + INJECT, "1:@" + NAMED + "(value=second)"), constructorAnnotations(factory));
    }

    @Test
    void factoryUsesInjectConstructorOverNoArgConstructor() throws Exception {
        Class<?> factory = loadFactory(
                mojoClass(constructor("()V", null, false), constructor("(Ljava/lang/String;)V", null, true)));

        assertEquals(1, factory.getDeclaredConstructors().length);
        assertEquals("value", field(factory.getConstructor(String.class).newInstance("value"), "p0"));
    }

    @Test
    void factoryUsesSoleConstructorWithoutInject() throws Exception {
        Class<?> factory = loadFactory(mojoClass(constructor("(Ljava/lang/String;)V", null, false)));

        assertEquals("value", field(factory.getConstructor(String.class).newInstance("value"), "p0"));
    }

    @Test
    void factoryForMojoWithNoArgConstructorIsUnchanged() throws Exception {
        byte[] mojo = mojoClass(constructor("()V", null, false), constructor("(Ljava/lang/String;)V", null, false));

        assertArrayEquals(generateFactory(null), generateFactory(mojo));
    }

    @Test
    void mojoWithoutUsableConstructorIsRejected() {
        byte[] mojo = mojoClass(constructor("(Ljava/lang/String;)V", null, false), constructor("(J)V", null, false));

        GeneratorException e = assertThrows(GeneratorException.class, () -> generateFactory(mojo));
        assertTrue(e.getMessage().contains(MOJO), e.getMessage());
    }

    @Test
    void mojoWithTwoInjectConstructorsIsRejected() {
        byte[] mojo = mojoClass(constructor("(Ljava/lang/String;)V", null, true), constructor("(J)V", null, true));

        GeneratorException e = assertThrows(GeneratorException.class, () -> generateFactory(mojo));
        assertTrue(e.getMessage().contains(MOJO), e.getMessage());
    }

    private static byte[] generateFactory(byte[] mojoClassBytes) throws GeneratorException {
        return DescriptorGeneratorMojo.computeGeneratorClassBytes(
                "org.example", "TestMojoFactory", "org.example:test:1.0:run", MOJO, mojoClassBytes);
    }

    private static Class<?> loadFactory(byte[] mojoClassBytes) throws GeneratorException {
        byte[] factoryClassBytes = generateFactory(mojoClassBytes);
        TestClassLoader loader = new TestClassLoader();
        loader.define(MOJO, mojoClassBytes);
        return loader.define(MOJO + "Factory", factoryClassBytes);
    }

    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        return instance.getClass().getSuperclass().getField(name).get(instance);
    }

    private static List<String> constructorAnnotations(byte[] classBytes) {
        List<String> annotations = new ArrayList<>();
        new ClassReader(classBytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public MethodVisitor visitMethod(
                                    int access, String name, String descriptor, String signature, String[] exceptions) {
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                                        return recorder("@" + annotation);
                                    }

                                    @Override
                                    public AnnotationVisitor visitParameterAnnotation(
                                            int parameter, String annotation, boolean visible) {
                                        return recorder(parameter + ":@" + annotation);
                                    }
                                };
                            }

                            private AnnotationVisitor recorder(String prefix) {
                                StringBuilder values = new StringBuilder();
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        values.append('(')
                                                .append(name)
                                                .append('=')
                                                .append(value)
                                                .append(')');
                                    }

                                    @Override
                                    public void visitEnd() {
                                        annotations.add(prefix + values);
                                    }
                                };
                            }
                        },
                        0);
        return annotations;
    }

    private static MojoConstructor constructor(
            String descriptor, String signature, boolean inject, String... namedParameters) {
        return new MojoConstructor(descriptor, signature, inject, namedParameters);
    }

    /**
     * Builds a mojo class whose constructors store each argument {@code i} in a public field {@code p<i>}.
     */
    private static byte[] mojoClass(MojoConstructor... constructors) {
        String mojo = MOJO.replace('.', '/');
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, mojo, null, "java/lang/Object", null);
        Set<String> fields = new HashSet<>();
        for (MojoConstructor constructor : constructors) {
            MethodVisitor mv =
                    cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", constructor.descriptor, constructor.signature, null);
            if (constructor.inject) {
                mv.visitAnnotation(INJECT, true).visitEnd();
            }
            for (int i = 0; i < constructor.namedParameters.length; i++) {
                if (constructor.namedParameters[i] != null) {
                    AnnotationVisitor av = mv.visitParameterAnnotation(i, NAMED, true);
                    av.visit("value", constructor.namedParameters[i]);
                    av.visitEnd();
                }
            }
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            Type[] argumentTypes = Type.getArgumentTypes(constructor.descriptor);
            int slot = 1;
            for (int i = 0; i < argumentTypes.length; i++) {
                String fieldDescriptor = argumentTypes[i].getDescriptor();
                if (fields.add("p" + i + fieldDescriptor)) {
                    cw.visitField(Opcodes.ACC_PUBLIC, "p" + i, fieldDescriptor, null, null)
                            .visitEnd();
                }
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(argumentTypes[i].getOpcode(Opcodes.ILOAD), slot);
                mv.visitFieldInsn(Opcodes.PUTFIELD, mojo, "p" + i, fieldDescriptor);
                slot += argumentTypes[i].getSize();
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static final class MojoConstructor {
        final String descriptor;
        final String signature;
        final boolean inject;
        final String[] namedParameters;

        MojoConstructor(String descriptor, String signature, boolean inject, String[] namedParameters) {
            this.descriptor = descriptor;
            this.signature = signature;
            this.inject = inject;
            this.namedParameters = namedParameters;
        }
    }

    private static final class TestClassLoader extends ClassLoader {
        TestClassLoader() {
            super(GeneratedMojoFactoryTest.class.getClassLoader());
        }

        Class<?> define(String name, byte[] classBytes) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
    }
}
