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

import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoClassVisitor
    extends ClassVisitor
{
    private Logger logger;

    private MojoAnnotatedClass mojoAnnotatedClass;

    private Map<String, MojoAnnotationVisitor> annotationVisitorMap = new HashMap<>();

    private List<MojoFieldVisitor> fieldVisitors = new ArrayList<>();

    public MojoClassVisitor( Logger logger )
    {
        super( Opcodes.ASM7 );
        this.logger = logger;
    }

    public MojoAnnotatedClass getMojoAnnotatedClass()
    {
        return mojoAnnotatedClass;
    }

    public void setMojoAnnotatedClass( MojoAnnotatedClass mojoAnnotatedClass )
    {
        this.mojoAnnotatedClass = mojoAnnotatedClass;
    }

    public Map<String, MojoAnnotationVisitor> getAnnotationVisitorMap()
    {
        return annotationVisitorMap;
    }

    public MojoAnnotationVisitor getAnnotationVisitor( Class<?> annotation )
    {
        return annotationVisitorMap.get( annotation.getName() );
    }

    public void setAnnotationVisitorMap( Map<String, MojoAnnotationVisitor> annotationVisitorMap )
    {
        this.annotationVisitorMap = annotationVisitorMap;
    }

    public List<MojoFieldVisitor> getFieldVisitors()
    {
        return fieldVisitors;
    }

    public void setFieldVisitors( List<MojoFieldVisitor> fieldVisitors )
    {
        this.fieldVisitors = fieldVisitors;
    }

    public List<MojoFieldVisitor> findFieldWithAnnotation( Class<?> annotation )
    {
        String annotationClassName = annotation.getName();

        List<MojoFieldVisitor> mojoFieldVisitors = new ArrayList<MojoFieldVisitor>();

        for ( MojoFieldVisitor mojoFieldVisitor : this.fieldVisitors )
        {
            MojoAnnotationVisitor mojoAnnotationVisitor = mojoFieldVisitor.getMojoAnnotationVisitor();
            if ( mojoAnnotationVisitor != null && StringUtils.equals( annotationClassName,
                                                                      mojoAnnotationVisitor.getAnnotationClassName() ) )
            {
                mojoFieldVisitors.add( mojoFieldVisitor );
            }
        }

        return mojoFieldVisitors;
    }

    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
    {
        mojoAnnotatedClass = new MojoAnnotatedClass();
        mojoAnnotatedClass.setClassName( Type.getObjectType( name ).getClassName() );
        if ( superName != null )
        {
            mojoAnnotatedClass.setParentClassName( Type.getObjectType( superName ).getClassName() );
        }
    }

    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        String annotationClassName = Type.getType( desc ).getClassName();
        if ( !MojoAnnotationsScanner.CLASS_LEVEL_ANNOTATIONS.contains( annotationClassName ) )
        {
            return null;
        }
        MojoAnnotationVisitor mojoAnnotationVisitor = new MojoAnnotationVisitor( logger, annotationClassName );
        annotationVisitorMap.put( annotationClassName, mojoAnnotationVisitor );
        return mojoAnnotationVisitor;
    }

    public FieldVisitor visitField( int access, String name, String desc, String signature, Object value )
    {
        MojoFieldVisitor mojoFieldVisitor = new MojoFieldVisitor( logger, name, Type.getType( desc ).getClassName() );
        fieldVisitors.add( mojoFieldVisitor );
        return mojoFieldVisitor;
    }

    public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions )
    {
        // we don't need methods informations
        return null;
    }

    public void visitAttribute( Attribute attr )
    {
        // no op
    }

    public void visitSource( String source, String debug )
    {
        // no op
    }

    public void visitOuterClass( String owner, String name, String desc )
    {
        // no op
    }

    public void visitInnerClass( String name, String outerName, String innerName, int access )
    {
        // no op
    }

    public void visitEnd()
    {
        // no op
    }

}
