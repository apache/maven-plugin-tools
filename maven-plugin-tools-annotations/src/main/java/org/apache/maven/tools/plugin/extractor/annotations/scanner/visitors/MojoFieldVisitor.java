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

import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.codehaus.plexus.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoFieldVisitor
    extends FieldVisitor
{
    private Logger logger;

    private String fieldName;

    private MojoAnnotationVisitor mojoAnnotationVisitor;

    private String className;

    MojoFieldVisitor( Logger logger, String fieldName, String className )
    {
        super( Opcodes.ASM7 );
        this.logger = logger;
        this.fieldName = fieldName;
        this.className = className;
    }

    public MojoAnnotationVisitor getMojoAnnotationVisitor()
    {
        return mojoAnnotationVisitor;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        String annotationClassName = Type.getType( desc ).getClassName();
        if ( !MojoAnnotationsScanner.FIELD_LEVEL_ANNOTATIONS.contains( annotationClassName ) )
        {
            return null;
        }
        mojoAnnotationVisitor = new MojoAnnotationVisitor( logger, annotationClassName );
        return mojoAnnotationVisitor;
    }

    public void visitAttribute( Attribute attribute )
    {
        // no op
    }

    public void visitEnd()
    {
        // no op
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }
}
