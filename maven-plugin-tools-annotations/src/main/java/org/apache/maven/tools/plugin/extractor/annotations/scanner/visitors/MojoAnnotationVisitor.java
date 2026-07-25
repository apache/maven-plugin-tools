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
package org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Visitor for annotations.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoAnnotationVisitor extends AnnotationVisitor {
    private String annotationClassName;

    private Map<String, Object> annotationValues = new HashMap<>();

    private List<MojoAnnotationVisitor> nestedAnnotationVisitors;

    private Map<String, MojoAnnotationVisitor> arrayVisitors;

    MojoAnnotationVisitor(String annotationClassName) {
        super(Opcodes.ASM9);
        this.annotationClassName = annotationClassName;
    }

    public Map<String, Object> getAnnotationValues() {
        return annotationValues;
    }

    @Override
    public void visit(String name, Object value) {
        annotationValues.put(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        annotationValues.put(name, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        String className = desc != null ? Type.getType(desc).getClassName() : this.annotationClassName;
        MojoAnnotationVisitor nested = new MojoAnnotationVisitor(className);
        if (nestedAnnotationVisitors == null) {
            nestedAnnotationVisitors = new ArrayList<>();
        }
        nestedAnnotationVisitors.add(nested);
        return nested;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        MojoAnnotationVisitor arrayVisitor = new MojoAnnotationVisitor(this.annotationClassName);
        if (this.arrayVisitors == null) {
            this.arrayVisitors = new HashMap<>();
        }
        this.arrayVisitors.put(name, arrayVisitor);
        return arrayVisitor;
    }

    /**
     * Returns the sub-visitor created by {@link #visitArray(String)} for the given array attribute.
     * The sub-visitor's {@link #getNestedAnnotationVisitors()} contains the annotation elements.
     *
     * @param name the array attribute name
     * @return the array sub-visitor, or {@code null} if not visited
     */
    public MojoAnnotationVisitor getArrayVisitor(String name) {
        return arrayVisitors != null ? arrayVisitors.get(name) : null;
    }

    /**
     * Returns the nested annotation visitors collected by {@link #visitAnnotation(String, String)}
     * and by array sub-visitors.  Used to extract elements from repeatable annotation containers
     * such as {@code @Afters}.
     *
     * @return list of nested annotation visitors, never {@code null}
     */
    public List<MojoAnnotationVisitor> getNestedAnnotationVisitors() {
        return nestedAnnotationVisitors != null ? nestedAnnotationVisitors : Collections.emptyList();
    }
}
