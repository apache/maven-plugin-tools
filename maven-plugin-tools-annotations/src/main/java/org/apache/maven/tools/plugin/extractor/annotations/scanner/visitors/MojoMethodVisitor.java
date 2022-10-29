package org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Method visitor.
 *
 * @author Slawomir Jaranowski
 */
public class MojoMethodVisitor extends MethodVisitor implements MojoParameterVisitor
{
    private final String className;
    private final String fieldName;
    private final List<String> typeParameters;
    private Map<String, MojoAnnotationVisitor> annotationVisitorMap = new HashMap<>();

    public MojoMethodVisitor( String fieldName, String className, List<String> typeParameters )
    {
        super( Opcodes.ASM9 );
        this.fieldName = fieldName;
        this.className = className;
        this.typeParameters = typeParameters;
    }

    @Override
    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        String annotationClassName = Type.getType( desc ).getClassName();
        if ( !MojoAnnotationsScanner.METHOD_LEVEL_ANNOTATIONS.contains( annotationClassName ) )
        {
            return null;
        }

        MojoAnnotationVisitor mojoAnnotationVisitor = new MojoAnnotationVisitor( annotationClassName );
        annotationVisitorMap.put( annotationClassName, mojoAnnotationVisitor );
        return mojoAnnotationVisitor;
    }

    @Override
    public String getFieldName()
    {
        return fieldName;
    }

    @Override
    public String getClassName()
    {
        return className;
    }

    @Override
    public List<String> getTypeParameters()
    {
        return typeParameters;
    }

    @Override
    public Map<String, MojoAnnotationVisitor> getAnnotationVisitorMap()
    {
        return annotationVisitorMap;
    }

    @Override
    public boolean isAnnotationOnMethod()
    {
        return true;
    }
}
