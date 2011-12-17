package org.apache.maven.tools.plugin.extractor.java;

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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.Type;

import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracts Mojo descriptors from <a href="http://java.sun.com/">Java</a> sources.
 * <br/>
 * For more information about the usage tag, have a look to:
 * <a href="http://maven.apache.org/developers/mojo-api-specification.html">
 * http://maven.apache.org/developers/mojo-api-specification.html</a>
 *
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 * @version $Id$
 * @see org.apache.maven.plugin.descriptor.MojoDescriptor
 */
public class JavaMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor, JavaMojoAnnotation
{
    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#INSTANTIATION_STRATEGY} instead of. */
    public static final String MAVEN_PLUGIN_INSTANTIATION = JavaMojoAnnotation.INSTANTIATION_STRATEGY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#CONFIGURATOR} instead of. */
    public static final String CONFIGURATOR = JavaMojoAnnotation.CONFIGURATOR;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER} instead of. */
    public static final String PARAMETER = JavaMojoAnnotation.PARAMETER;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_EXPRESSION} instead of. */
    public static final String PARAMETER_EXPRESSION = JavaMojoAnnotation.PARAMETER_EXPRESSION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_DEFAULT_VALUE} instead of. */
    public static final String PARAMETER_DEFAULT_VALUE = JavaMojoAnnotation.PARAMETER_DEFAULT_VALUE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_ALIAS} instead of. */
    public static final String PARAMETER_ALIAS = JavaMojoAnnotation.PARAMETER_ALIAS;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#SINCE} instead of. */
    public static final String SINCE = JavaMojoAnnotation.SINCE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_IMPLEMENTATION} instead of. */
    public static final String PARAMETER_IMPLEMENTATION = JavaMojoAnnotation.PARAMETER_IMPLEMENTATION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_PROPERTY} instead of. */
    public static final String PARAMETER_PROPERTY = JavaMojoAnnotation.PARAMETER_PROPERTY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRED} instead of. */
    public static final String REQUIRED = JavaMojoAnnotation.REQUIRED;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#DEPRECATED} instead of. */
    public static final String DEPRECATED = JavaMojoAnnotation.DEPRECATED;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#READONLY} instead of. */
    public static final String READONLY = JavaMojoAnnotation.READONLY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#GOAL} instead of. */
    public static final String GOAL = JavaMojoAnnotation.GOAL;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PHASE} instead of. */
    public static final String PHASE = JavaMojoAnnotation.PHASE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE} instead of. */
    public static final String EXECUTE = JavaMojoAnnotation.EXECUTE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_LIFECYCLE} instead of. */
    public static final String EXECUTE_LIFECYCLE = JavaMojoAnnotation.EXECUTE_LIFECYCLE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_PHASE} instead of. */
    public static final String EXECUTE_PHASE = JavaMojoAnnotation.EXECUTE_PHASE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_GOAL} instead of. */
    public static final String EXECUTE_GOAL = JavaMojoAnnotation.EXECUTE_GOAL;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#DESCRIPTION} instead of. */
    public static final String GOAL_DESCRIPTION = JavaMojoAnnotation.DESCRIPTION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_DEPENDENCY_RESOLUTION} instead of. */
    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = JavaMojoAnnotation.REQUIRES_DEPENDENCY_RESOLUTION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_PROJECT} instead of. */
    public static final String GOAL_REQUIRES_PROJECT = JavaMojoAnnotation.REQUIRES_PROJECT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_REPORTS} instead of. */
    public static final String GOAL_REQUIRES_REPORTS = JavaMojoAnnotation.REQUIRES_REPORTS;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#AGGREGATOR} instead of. */
    public static final String GOAL_IS_AGGREGATOR = JavaMojoAnnotation.AGGREGATOR;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_ONLINE} instead of. */
    public static final String GOAL_REQUIRES_ONLINE = JavaMojoAnnotation.REQUIRES_ONLINE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#INHERIT_BY_DEFAULT} instead of. */
    public static final String GOAL_INHERIT_BY_DEFAULT = JavaMojoAnnotation.INHERIT_BY_DEFAULT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#MULTI_EXECUTION_STRATEGY} instead of. */
    public static final String GOAL_MULTI_EXECUTION_STRATEGY = JavaMojoAnnotation.MULTI_EXECUTION_STRATEGY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_DIRECT_INVOCATION} instead of. */
    public static final String GOAL_REQUIRES_DIRECT_INVOCATION = JavaMojoAnnotation.REQUIRES_DIRECT_INVOCATION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT} instead of. */
    public static final String COMPONENT = JavaMojoAnnotation.COMPONENT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT_ROLE} instead of. */
    public static final String COMPONENT_ROLE = JavaMojoAnnotation.COMPONENT_ROLE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT_ROLEHINT} instead of. */
    public static final String COMPONENT_ROLEHINT = JavaMojoAnnotation.COMPONENT_ROLEHINT;

    /**
     * @param parameter not null
     * @param i positive number
     * @throws InvalidParameterException if any
     */
    protected void validateParameter( Parameter parameter, int i )
        throws InvalidParameterException
    {
        // TODO: remove when backward compatibility is no longer an issue.
        String name = parameter.getName();

        if ( name == null )
        {
            throw new InvalidParameterException( "name", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
        String type = parameter.getType();

        if ( type == null )
        {
            throw new InvalidParameterException( "type", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
        String description = parameter.getDescription();

        if ( description == null )
        {
            throw new InvalidParameterException( "description", i );
        }
    }

    // ----------------------------------------------------------------------
    // Mojo descriptor creation from @tags
    // ----------------------------------------------------------------------

    /**
     * @param javaClass not null
     * @return a mojo descriptor
     * @throws InvalidPluginDescriptorException if any
     */
    protected MojoDescriptor createMojoDescriptor( JavaClass javaClass )
        throws InvalidPluginDescriptorException
    {
        ExtendedMojoDescriptor mojoDescriptor = new ExtendedMojoDescriptor();
        mojoDescriptor.setLanguage( "java" );
        mojoDescriptor.setImplementation( javaClass.getFullyQualifiedName() );
        mojoDescriptor.setDescription( javaClass.getComment() );

        // ----------------------------------------------------------------------
        // Mojo annotations in alphabetical order
        // ----------------------------------------------------------------------

        // Aggregator flag
        DocletTag aggregator = findInClassHierarchy( javaClass, JavaMojoAnnotation.AGGREGATOR );
        if ( aggregator != null )
        {
            mojoDescriptor.setAggregator( true );
        }

        // Configurator hint
        DocletTag configurator = findInClassHierarchy( javaClass, JavaMojoAnnotation.CONFIGURATOR );
        if ( configurator != null )
        {
            mojoDescriptor.setComponentConfigurator( configurator.getValue() );
        }

        // Additional phase to execute first
        DocletTag execute = findInClassHierarchy( javaClass, JavaMojoAnnotation.EXECUTE );
        if ( execute != null )
        {
            String executePhase = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_PHASE );
            String executeGoal = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_GOAL );

            if ( executePhase == null && executeGoal == null )
            {
                throw new InvalidPluginDescriptorException( "@execute tag requires a 'phase' or 'goal' parameter" );
            }
            else if ( executePhase != null && executeGoal != null )
            {
                throw new InvalidPluginDescriptorException(
                    "@execute tag can have only one of a 'phase' or 'goal' parameter" );
            }
            mojoDescriptor.setExecutePhase( executePhase );
            mojoDescriptor.setExecuteGoal( executeGoal );

            String lifecycle = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_LIFECYCLE );
            if ( lifecycle != null )
            {
                mojoDescriptor.setExecuteLifecycle( lifecycle );
                if ( mojoDescriptor.getExecuteGoal() != null )
                {
                    throw new InvalidPluginDescriptorException(
                        "@execute lifecycle requires a phase instead of a goal" );
                }
            }
        }

        // Goal name
        DocletTag goal = findInClassHierarchy( javaClass, JavaMojoAnnotation.GOAL );
        if ( goal != null )
        {
            mojoDescriptor.setGoal( goal.getValue() );
        }

        // inheritByDefault flag
        boolean value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.INHERIT_BY_DEFAULT,
                                mojoDescriptor.isInheritedByDefault() );
        mojoDescriptor.setInheritedByDefault( value );

        // instantiationStrategy
        DocletTag tag = findInClassHierarchy( javaClass, JavaMojoAnnotation.INSTANTIATION_STRATEGY );
        if ( tag != null )
        {
            mojoDescriptor.setInstantiationStrategy( tag.getValue() );
        }

        // executionStrategy (and deprecated @attainAlways)
        tag = findInClassHierarchy( javaClass, JavaMojoAnnotation.MULTI_EXECUTION_STRATEGY );
        if ( tag != null )
        {
            getLogger().warn( "@" + JavaMojoAnnotation.MULTI_EXECUTION_STRATEGY + " in "
                                  + javaClass.getFullyQualifiedName() + " is deprecated: please use '@"
                                  + JavaMojoAnnotation.EXECUTION_STATEGY + " always' instead.");
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.MULTI_PASS_EXEC_STRATEGY );
        }
        else
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.SINGLE_PASS_EXEC_STRATEGY );
        }
        tag = findInClassHierarchy( javaClass, JavaMojoAnnotation.EXECUTION_STATEGY );
        if ( tag != null )
        {
            mojoDescriptor.setExecutionStrategy( tag.getValue() );
        }

        // Phase name
        DocletTag phase = findInClassHierarchy( javaClass, JavaMojoAnnotation.PHASE );
        if ( phase != null )
        {
            mojoDescriptor.setPhase( phase.getValue() );
        }

        // Dependency resolution flag
        DocletTag requiresDependencyResolution =
            findInClassHierarchy( javaClass, JavaMojoAnnotation.REQUIRES_DEPENDENCY_RESOLUTION );
        if ( requiresDependencyResolution != null )
        {
            String v = requiresDependencyResolution.getValue();

            if ( StringUtils.isEmpty( v ) )
            {
                v = "runtime";
            }

            mojoDescriptor.setDependencyResolutionRequired( v );
        }

        // Dependency collection flag
        DocletTag requiresDependencyCollection =
            findInClassHierarchy( javaClass, JavaMojoAnnotation.REQUIRES_DEPENDENCY_COLLECTION );
        if ( requiresDependencyCollection != null )
        {
            String v = requiresDependencyCollection.getValue();

            if ( StringUtils.isEmpty( v ) )
            {
                v = "runtime";
            }

            mojoDescriptor.setDependencyCollectionRequired( v );
        }

        // requiresDirectInvocation flag
        value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_DIRECT_INVOCATION,
                                mojoDescriptor.isDirectInvocationOnly() );
        mojoDescriptor.setDirectInvocationOnly( value );

        // Online flag
        value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_ONLINE, mojoDescriptor.isOnlineRequired() );
        mojoDescriptor.setOnlineRequired( value );

        // Project flag
        value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_PROJECT, mojoDescriptor.isProjectRequired() );
        mojoDescriptor.setProjectRequired( value );

        // requiresReports flag
        value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_REPORTS, mojoDescriptor.isRequiresReports() );
        mojoDescriptor.setRequiresReports( value );

        // ----------------------------------------------------------------------
        // Javadoc annotations in alphabetical order
        // ----------------------------------------------------------------------

        // Deprecation hint
        DocletTag deprecated = javaClass.getTagByName( JavaMojoAnnotation.DEPRECATED );
        if ( deprecated != null )
        {
            mojoDescriptor.setDeprecated( deprecated.getValue() );
        }

        // What version it was introduced in
        DocletTag since = findInClassHierarchy( javaClass, JavaMojoAnnotation.SINCE );
        if ( since != null )
        {
            mojoDescriptor.setSince( since.getValue() );
        }

        // Thread-safe mojo 

        value = getBooleanTagValue( javaClass, JavaMojoAnnotation.THREAD_SAFE, true, mojoDescriptor.isThreadSafe() );
        mojoDescriptor.setThreadSafe( value );

        extractParameters( mojoDescriptor, javaClass );

        return mojoDescriptor;
    }

    /**
     * @param javaClass not null
     * @param tagName not null
     * @param defaultValue the wanted default value
     * @return the boolean value of the given tagName
     * @see #findInClassHierarchy(JavaClass, String)
     */
    private static boolean getBooleanTagValue( JavaClass javaClass, String tagName, boolean defaultValue )
    {
        DocletTag tag = findInClassHierarchy( javaClass, tagName );

        if ( tag != null )
        {
            String value = tag.getValue();

            if ( StringUtils.isNotEmpty( value ) )
            {
                defaultValue = Boolean.valueOf( value ).booleanValue();
            }
        }
        return defaultValue;
    }

    /**
     * @param javaClass     not null
     * @param tagName       not null
     * @param defaultForTag The wanted default value when only the tagname is present
     * @param defaultValue  the wanted default value when the tag is not specified
     * @return the boolean value of the given tagName
     * @see #findInClassHierarchy(JavaClass, String)
     */
    private static boolean getBooleanTagValue( JavaClass javaClass, String tagName, boolean defaultForTag,
                                               boolean defaultValue )
    {
        DocletTag tag = findInClassHierarchy( javaClass, tagName );

        if ( tag != null )
        {
            String value = tag.getValue();

            if ( StringUtils.isNotEmpty( value ) )
            {
                return Boolean.valueOf( value ).booleanValue();
            }
            else
            {
                return defaultForTag;
            }
        }
        return defaultValue;
    }

    /**
     * @param javaClass not null
     * @param tagName not null
     * @return docletTag instance
     */
    private static DocletTag findInClassHierarchy( JavaClass javaClass, String tagName )
    {
        DocletTag tag = javaClass.getTagByName( tagName );

        if ( tag == null )
        {
            JavaClass superClass = javaClass.getSuperJavaClass();

            if ( superClass != null )
            {
                tag = findInClassHierarchy( superClass, tagName );
            }
        }

        return tag;
    }

    /**
     * @param mojoDescriptor not null
     * @param javaClass not null
     * @throws InvalidPluginDescriptorException if any
     */
    private void extractParameters( MojoDescriptor mojoDescriptor, JavaClass javaClass )
        throws InvalidPluginDescriptorException
    {
        // ---------------------------------------------------------------------------------
        // We're resolving class-level, ancestor-class-field, local-class-field order here.
        // ---------------------------------------------------------------------------------

        Map<String, JavaField> rawParams = extractFieldParameterTags( javaClass );

        for ( Map.Entry<String, JavaField> entry : rawParams.entrySet() )
        {
            JavaField field = entry.getValue();

            Type type = field.getType();

            Parameter pd = new Parameter();

            if ( !type.isArray() )
            {
                pd.setType( type.getValue() );
            }
            else
            {
                StringBuffer value = new StringBuffer( type.getValue() );

                int remaining = type.getDimensions();

                while ( remaining-- > 0 )
                {
                    value.append( "[]" );
                }

                pd.setType( value.toString() );
            }

            pd.setDescription( field.getComment() );

            DocletTag componentTag = field.getTagByName( JavaMojoAnnotation.COMPONENT );
            if ( componentTag != null )
            {
                String role = componentTag.getNamedParameter( JavaMojoAnnotation.COMPONENT_ROLE );

                if ( role == null )
                {
                    role = field.getType().toString();
                }

                String roleHint = componentTag.getNamedParameter( JavaMojoAnnotation.COMPONENT_ROLEHINT );

                if ( roleHint == null )
                {
                    // support alternate syntax for better compatibility with the Plexus CDC.
                    roleHint = componentTag.getNamedParameter( "role-hint" );
                }

                pd.setRequirement( new Requirement( role, roleHint ) );

                pd.setName( entry.getKey() );

                pd.setEditable( false );
                /* TODO: or better like this? Need @component fields be editable for the user?
                pd.setEditable( field.getTagByName( READONLY ) == null );
                */
            }
            else
            {
                DocletTag parameter = field.getTagByName( JavaMojoAnnotation.PARAMETER );

                // ----------------------------------------------------------------------
                // We will look for a property name here first and use that if present
                // i.e:
                //
                // @parameter property="project"
                //
                // Which will become the name used for the configuration element which
                // will in turn will allow plexus to use the corresponding setter.
                // ----------------------------------------------------------------------

                String property = parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_PROPERTY );

                if ( !StringUtils.isEmpty( property ) )
                {
                    pd.setName( property );
                }
                else
                {
                    pd.setName( entry.getKey() );
                }

                pd.setRequired( field.getTagByName( JavaMojoAnnotation.REQUIRED ) != null );

                pd.setEditable( field.getTagByName( JavaMojoAnnotation.READONLY ) == null );

                DocletTag deprecationTag = field.getTagByName( JavaMojoAnnotation.DEPRECATED );

                if ( deprecationTag != null )
                {
                    pd.setDeprecated( deprecationTag.getValue() );
                }

                DocletTag sinceTag = field.getTagByName( JavaMojoAnnotation.SINCE );
                if ( sinceTag != null )
                {
                    pd.setSince( sinceTag.getValue() );
                }

                String alias = parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_ALIAS );

                if ( !StringUtils.isEmpty( alias ) )
                {
                    pd.setAlias( alias );
                }

                String expression = parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_EXPRESSION );
                pd.setExpression( expression );

                if ( StringUtils.isNotEmpty( expression ) && expression.startsWith( "${component." ) )
                {
                    getLogger().warn( javaClass.getFullyQualifiedName() + "#" + field.getName() + ":" );
                    getLogger().warn( "  The syntax" );
                    getLogger().warn( "    @parameter expression=\"${component.<role>#<roleHint>}\"" );
                    getLogger().warn( "  is deprecated, please use" );
                    getLogger().warn( "    @component role=\"<role>\" roleHint=\"<roleHint>\"" );
                    getLogger().warn( "  instead." );
                }

                if ( "${reports}".equals( pd.getExpression() ) )
                {
                    mojoDescriptor.setRequiresReports( true );
                }

                pd.setDefaultValue( parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_DEFAULT_VALUE ) );

                pd.setImplementation( parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_IMPLEMENTATION ) );
            }

            mojoDescriptor.addParameter( pd );
        }
    }

    /**
     * extract fields that are either parameters or components.
     * 
     * @param javaClass not null
     * @return map with Mojo parameters names as keys
     */
    private Map<String, JavaField> extractFieldParameterTags( JavaClass javaClass )
    {
        Map<String, JavaField> rawParams;

        // we have to add the parent fields first, so that they will be overwritten by the local fields if
        // that actually happens...
        JavaClass superClass = javaClass.getSuperJavaClass();

        if ( superClass != null )
        {
            rawParams = extractFieldParameterTags( superClass );
        }
        else
        {
            rawParams = new TreeMap<String, JavaField>();
        }

        JavaField[] classFields = javaClass.getFields();

        if ( classFields != null )
        {
            for ( JavaField field : classFields )
            {
                if ( field.getTagByName( JavaMojoAnnotation.PARAMETER ) != null
                    || field.getTagByName( JavaMojoAnnotation.COMPONENT ) != null )
                {
                    rawParams.put( field.getName(), field );
                }
            }
        }
        return rawParams;
    }

    /** {@inheritDoc} */
    public List<MojoDescriptor> execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        return execute( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
    }
    
    /** {@inheritDoc} */
    public List<MojoDescriptor> execute( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        JavaClass[] javaClasses = discoverClasses( request );

        List<MojoDescriptor> descriptors = new ArrayList<MojoDescriptor>();

        for ( JavaClass javaClass : javaClasses )
        {
            DocletTag tag = javaClass.getTagByName( GOAL );

            if ( tag != null )
            {
                MojoDescriptor mojoDescriptor = createMojoDescriptor( javaClass );
                mojoDescriptor.setPluginDescriptor( request.getPluginDescriptor() );

                // Validate the descriptor as best we can before allowing it to be processed.
                validate( mojoDescriptor );

                descriptors.add( mojoDescriptor );
            }
        }

        return descriptors;
    }

    /**
     * @param request The plugin request.
     * @return an array of java class
     */
    @SuppressWarnings( "unchecked" )
    protected JavaClass[] discoverClasses( final PluginToolsRequest request )
    {
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( request.getEncoding() );
        
        MavenProject project = request.getProject();

        for ( String source : (List<String>) project.getCompileSourceRoots() )
        {
            builder.addSourceTree( new File( source ) );
        }

        // TODO be more dynamic
        File generatedPlugin = new File( project.getBasedir(), "target/generated-sources/plugin" );
        if ( !project.getCompileSourceRoots().contains( generatedPlugin.getAbsolutePath() ) )
        {
            builder.addSourceTree( generatedPlugin );
        }

        return builder.getClasses();
    }

    /**
     * @param mojoDescriptor not null
     * @throws InvalidParameterException if any
     */
    protected void validate( MojoDescriptor mojoDescriptor )
        throws InvalidParameterException
    {
        @SuppressWarnings( "unchecked" )
        List<Parameter> parameters = mojoDescriptor.getParameters();

        if ( parameters != null )
        {
            for ( int j = 0; j < parameters.size(); j++ )
            {
                validateParameter( parameters.get( j ), j );
            }
        }
    }
}
