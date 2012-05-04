package org.apache.maven.tools.plugin.annotations.scanner;
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

import org.apache.maven.tools.plugin.annotations.Execute;
import org.apache.maven.tools.plugin.annotations.LifecyclePhase;
import org.apache.maven.tools.plugin.annotations.Mojo;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class MojoClassVisitor
    implements ClassVisitor
{
    private Logger logger;

    private MojoAnnotatedClass mojoAnnotatedClass;

    private Map<String, MojoAnnotationVisitor> annotationVisitorMap = new HashMap<String, MojoAnnotationVisitor>();

    private List<MojoFieldVisitor> fieldVisitors = new ArrayList<MojoFieldVisitor>();

    public MojoClassVisitor( Logger logger )
    {
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

    public List<MojoFieldVisitor> findFieldWithAnnotationClass( String annotationClassName )
    {
        List<MojoFieldVisitor> mojoFieldVisitors = new ArrayList<MojoFieldVisitor>();

        for ( MojoFieldVisitor mojoFieldVisitor : this.fieldVisitors )
        {
            MojoAnnotationVisitor mojoAnnotationVisitor = mojoFieldVisitor.getMojoAnnotationVisitor();
            if ( mojoAnnotationVisitor != null && StringUtils.equals( annotationClassName,
                                                                      mojoAnnotationVisitor.annotationClassName ) )
            {
                mojoFieldVisitors.add( mojoFieldVisitor );
            }
        }

        return mojoFieldVisitors;
    }

    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
    {
        mojoAnnotatedClass = new MojoAnnotatedClass();
        mojoAnnotatedClass.setClassName( Type.getObjectType( name ).getClassName() ).setParentClassName(
            Type.getObjectType( superName ).getClassName() );
        logger.debug( "MojoClassVisitor#visit" );
    }

    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        logger.debug( "MojoClassVisitor#visitAnnotation" );
        String annotationClassName = Type.getType( desc ).getClassName();
        if ( !MojoAnnotationsScanner.acceptedClassLevelAnnotationClasses.contains( annotationClassName ) )
        {
            return null;
        }
        MojoAnnotationVisitor mojoAnnotationVisitor = new MojoAnnotationVisitor( logger, annotationClassName );
        annotationVisitorMap.put( annotationClassName, mojoAnnotationVisitor );
        return mojoAnnotationVisitor;
    }

    public FieldVisitor visitField( int access, String name, String desc, String signature, Object value )
    {
        // Type.getType( desc ).getClassName()
        logger.debug( "MojoClassVisitor#visitField" );
        MojoFieldVisitor mojoFieldVisitor = new MojoFieldVisitor( logger, name );
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
    }

    public void visitSource( String source, String debug )
    {
    }

    public void visitOuterClass( String owner, String name, String desc )
    {
    }

    public void visitInnerClass( String name, String outerName, String innerName, int access )
    {
    }

    public void visitEnd()
    {
        logger.debug( "MojoClassVisitor#visitEnd" );
    }

    public static class MojoAnnotationContent
        implements Mojo
    {

        private String name;

        private LifecyclePhase defaultPhase = LifecyclePhase.NONE;

        private String requiresDependencyResolution = "runtime";

        private String requiresDependencyCollection = "runtime";

        private String instantiationStrategy = "per-lookup";

        private String executionStrategy = "once-per-session";

        private boolean requiresProject = true;

        private boolean requiresReports = false;

        private boolean aggregator = false;

        private boolean requiresDirectInvocation = false;

        private boolean requiresOnline = false;

        private boolean inheritByDefault = true;

        private String configurator;

        private boolean threadSafe = false;

        public Class<? extends Annotation> annotationType()
        {
            return null;
        }

        public LifecyclePhase defaultPhase()
        {
            return defaultPhase;
        }

        public void defaultPhase( String phase )
        {
            this.defaultPhase = LifecyclePhase.valueOf( phase );
        }

        public String requiresDependencyResolution()
        {
            return requiresDependencyResolution;
        }

        public void requiresDependencyResolution( String requiresDependencyResolution )
        {
            this.requiresDependencyResolution = requiresDependencyResolution;
        }

        public String requiresDependencyCollection()
        {
            return requiresDependencyCollection;
        }

        public void requiresDependencyCollection( String requiresDependencyCollection )
        {
            this.requiresDependencyCollection = requiresDependencyCollection;
        }

        public String instantiationStrategy()
        {
            return instantiationStrategy;
        }

        public void instantiationStrategy( String instantiationStrategy )
        {
            this.instantiationStrategy = instantiationStrategy;
        }

        public String executionStrategy()
        {
            return executionStrategy;
        }

        public void executionStrategy( String executionStrategy )
        {
            this.executionStrategy = executionStrategy;
        }

        public boolean requiresProject()
        {
            return requiresProject;
        }

        public void requiresProject( boolean requiresProject )
        {
            this.requiresProject = requiresProject;
        }

        public boolean requiresReports()
        {
            return requiresReports;
        }

        public void requiresReports( boolean requiresReports )
        {
            this.requiresReports = requiresReports;
        }

        public boolean aggregator()
        {
            return aggregator;
        }

        public void aggregator( boolean aggregator )
        {
            this.aggregator = aggregator;
        }

        public boolean requiresDirectInvocation()
        {
            return requiresDirectInvocation;
        }

        public void requiresDirectInvocation( boolean requiresDirectInvocation )
        {
            this.requiresDirectInvocation = requiresDirectInvocation;
        }

        public boolean requiresOnline()
        {
            return requiresOnline;
        }

        public void requiresOnline( boolean requiresOnline )
        {
            this.requiresOnline = requiresOnline;
        }

        public boolean inheritByDefault()
        {
            return inheritByDefault;
        }

        public void inheritByDefault( boolean inheritByDefault )
        {
            this.inheritByDefault = inheritByDefault;
        }

        public String configurator()
        {
            return configurator;
        }

        public void configurator( String configurator )
        {
            this.configurator = configurator;
        }

        public boolean threadSafe()
        {
            return threadSafe;
        }

        public void threadSafe( boolean threadSafe )
        {
            this.threadSafe = threadSafe;
        }

        public String name()
        {
            return this.name;
        }

        public void name( String name )
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "MojoAnnotationContent" );
            sb.append( "{name='" ).append( name ).append( '\'' );
            sb.append( ", defaultPhase=" ).append( defaultPhase );
            sb.append( ", requiresDependencyResolution='" ).append( requiresDependencyResolution ).append( '\'' );
            sb.append( ", requiresDependencyCollection='" ).append( requiresDependencyCollection ).append( '\'' );
            sb.append( ", instantiationStrategy='" ).append( instantiationStrategy ).append( '\'' );
            sb.append( ", executionStrategy='" ).append( executionStrategy ).append( '\'' );
            sb.append( ", requiresProject=" ).append( requiresProject );
            sb.append( ", requiresReports=" ).append( requiresReports );
            sb.append( ", aggregator=" ).append( aggregator );
            sb.append( ", requiresDirectInvocation=" ).append( requiresDirectInvocation );
            sb.append( ", requiresOnline=" ).append( requiresOnline );
            sb.append( ", inheritByDefault=" ).append( inheritByDefault );
            sb.append( ", configurator='" ).append( configurator ).append( '\'' );
            sb.append( ", threadSafe=" ).append( threadSafe );
            sb.append( '}' );
            return sb.toString();
        }
    }

    public static class ExecuteAnnotationContent
        implements Execute
    {

        private String goal;

        private String lifecycle;

        private LifecyclePhase phase;

        public LifecyclePhase phase()
        {
            return this.phase;
        }

        public String goal()
        {
            return this.goal;
        }

        public String lifecycle()
        {
            return this.lifecycle;
        }


        public void phase( String phase )
        {
            this.phase = LifecyclePhase.valueOf( phase );
        }

        public void goal( String goal )
        {
            this.goal = goal;
        }

        public void lifecycle( String lifecycle )
        {
            this.lifecycle = lifecycle;
        }


        public Class<? extends Annotation> annotationType()
        {
            return null;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "ExecuteAnnotationContent" );
            sb.append( "{goal='" ).append( goal ).append( '\'' );
            sb.append( ", lifecycle='" ).append( lifecycle ).append( '\'' );
            sb.append( ", phase=" ).append( phase );
            sb.append( '}' );
            return sb.toString();
        }
    }

    //-------------------------------------
    // internal classes
    //-------------------------------------
    static class MojoAnnotationVisitor
        implements AnnotationVisitor
    {

        private Logger logger;

        private String annotationClassName;

        private Map<String, Object> annotationValues = new HashMap<String, Object>();

        MojoAnnotationVisitor( Logger logger, String annotationClassName )
        {
            this.logger = logger;
            this.annotationClassName = annotationClassName;
        }

        public Map<String, Object> getAnnotationValues()
        {
            return annotationValues;
        }

        public void visit( String name, Object value )
        {
            annotationValues.put( name, value );
            logger.debug( "MojoAnnotationVisitor#visit:" + name + ":" + value );
        }

        public void visitEnum( String name, String desc, String value )
        {
            annotationValues.put( name, value );
            logger.debug( "MojoAnnotationVisitor#visitEnum:" + name + ":" + desc + ":" + value );
        }

        public AnnotationVisitor visitAnnotation( String name, String desc )
        {
            logger.debug( "MojoAnnotationVisitor#visitAnnotation:" + name + ":" + desc );
            return new MojoAnnotationVisitor( logger, this.annotationClassName );
        }

        public AnnotationVisitor visitArray( String s )
        {
            logger.debug( "MojoAnnotationVisitor#visitArray" );
            return new MojoAnnotationVisitor( logger, this.annotationClassName );
        }

        public void visitEnd()
        {
            logger.debug( "MojoAnnotationVisitor#visitEnd" );
        }
    }

    static class MojoFieldVisitor
        implements FieldVisitor
    {
        private Logger logger;

        private String fieldName;

        private MojoAnnotationVisitor mojoAnnotationVisitor;

        MojoFieldVisitor( Logger logger, String fieldName )
        {
            this.logger = logger;
            this.fieldName = fieldName;
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
            logger.debug( "MojoFieldVisitor#visitAnnotation:" + desc );
            String annotationClassName = Type.getType( desc ).getClassName();
            if ( !MojoAnnotationsScanner.acceptedFieldLevelAnnotationClasses.contains( annotationClassName ) )
            {
                return null;
            }
            mojoAnnotationVisitor = new MojoAnnotationVisitor( logger, annotationClassName );
            return mojoAnnotationVisitor;
        }

        public void visitAttribute( Attribute attribute )
        {
            logger.debug( "MojoFieldVisitor#visitAttribute" );
        }

        public void visitEnd()
        {
            logger.debug( "MojoFieldVisitor#visitEnd" );
        }
    }
}
