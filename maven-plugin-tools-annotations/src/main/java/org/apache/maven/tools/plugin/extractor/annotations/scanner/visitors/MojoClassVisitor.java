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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.codehaus.plexus.util.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.util.TraceSignatureVisitor;

/**
 * Visitor for Mojo classes.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoClassVisitor
    extends ClassVisitor
{
    private MojoAnnotatedClass mojoAnnotatedClass;

    private Map<String, MojoAnnotationVisitor> annotationVisitorMap = new HashMap<>();

    private List<MojoFieldVisitor> fieldVisitors = new ArrayList<>();

    private List<MojoMethodVisitor> methodVisitors = new ArrayList<>();

    public MojoClassVisitor()
    {
        super( Opcodes.ASM9 );
    }

    public MojoAnnotatedClass getMojoAnnotatedClass()
    {
        return mojoAnnotatedClass;
    }

    public MojoAnnotationVisitor getAnnotationVisitor( Class<?> annotation )
    {
        return annotationVisitorMap.get( annotation.getName() );
    }

    public List<MojoFieldVisitor> findFieldWithAnnotation( Class<?> annotation )
    {
        String annotationClassName = annotation.getName();

        return fieldVisitors.stream()
            .filter( field -> field.getAnnotationVisitorMap().containsKey( annotationClassName ) )
            .collect( Collectors.toList() );
    }

    public List<MojoParameterVisitor> findParameterVisitors()
    {
        String annotationClassName = Parameter.class.getName();

        return Stream
            .concat(
                findFieldWithAnnotation( Parameter.class ).stream(),
                methodVisitors.stream()
                    .filter( method -> method.getAnnotationVisitorMap().containsKey( annotationClassName ) ) )
            .collect( Collectors.toList() );
    }

    @Override
    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
    {
        mojoAnnotatedClass = new MojoAnnotatedClass();
        mojoAnnotatedClass.setClassName( Type.getObjectType( name ).getClassName() );
        if ( superName != null )
        {
            mojoAnnotatedClass.setParentClassName( Type.getObjectType( superName ).getClassName() );
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        String annotationClassName = Type.getType( desc ).getClassName();
        if ( !MojoAnnotationsScanner.CLASS_LEVEL_ANNOTATIONS.contains( annotationClassName ) )
        {
            return null;
        }
        MojoAnnotationVisitor mojoAnnotationVisitor = new MojoAnnotationVisitor( annotationClassName );
        annotationVisitorMap.put( annotationClassName, mojoAnnotationVisitor );
        return mojoAnnotationVisitor;
    }

    @Override
    public FieldVisitor visitField( int access, String name, String desc, String signature, Object value )
    {
        List<String> typeParameters = extractTypeParameters( access, signature, true );
        MojoFieldVisitor mojoFieldVisitor = new MojoFieldVisitor( name, Type.getType( desc ).getClassName(),
                typeParameters );
        fieldVisitors.add( mojoFieldVisitor );
        return mojoFieldVisitor;
    }

    /**
     * Parses the signature according to 
     * <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.4">JVMS 4.3.4</a>
     * and returns the type parameters.
     * @param access
     * @param signature
     * @param isField
     * @return the list of type parameters (may be empty)
     */
    private List<String> extractTypeParameters( int access, String signature, boolean isField )
    {
        if ( StringUtils.isEmpty( signature ) )
        {
            return Collections.emptyList();
        }
        TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor( access );
        SignatureReader signatureReader = new SignatureReader( signature );
        if ( isField )
        {
            signatureReader.acceptType( traceSignatureVisitor );
        }
        else
        {
            signatureReader.accept( traceSignatureVisitor );
        }
        String declaration = traceSignatureVisitor.getDeclaration();
        int startTypeParameters = declaration.indexOf( '<' );
        if ( startTypeParameters == -1 )
        {
            return Collections.emptyList();
        }
        String typeParameters = declaration.substring( startTypeParameters + 1,
                                                       declaration.lastIndexOf( '>' ) );
        return Arrays.asList( typeParameters.split( ", " ) );
    }

    @Override
    public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions )
    {
        if ( ( access & Opcodes.ACC_PUBLIC ) != Opcodes.ACC_PUBLIC
            || ( access & Opcodes.ACC_STATIC ) == Opcodes.ACC_STATIC )
        {
            return null;
        }

        if ( name.length() < 4 || !( name.startsWith( "add" ) || name.startsWith( "set" ) ) )
        {
            return null;
        }

        Type type = Type.getType( desc );

        if ( "void".equals( type.getReturnType().getClassName() ) && type.getArgumentTypes().length == 1 )
        {
            String fieldName = StringUtils.lowercaseFirstLetter( name.substring( 3 ) );
            String className = type.getArgumentTypes()[0].getClassName();
            List<String> typeParameters = extractTypeParameters( access, signature, false );

            MojoMethodVisitor mojoMethodVisitor = new MojoMethodVisitor( fieldName, className, typeParameters );
            methodVisitors.add( mojoMethodVisitor );
            return mojoMethodVisitor;
        }

        return null;
    }
}
