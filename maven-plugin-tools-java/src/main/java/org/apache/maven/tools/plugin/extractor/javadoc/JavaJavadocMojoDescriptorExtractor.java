package org.apache.maven.tools.plugin.extractor.javadoc;

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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaType;

/**
 * <p>
 * Extracts Mojo descriptors from <a href="http://java.sun.com/">Java</a> sources.
 * </p>
 * For more information about the usage tag, have a look to:
 * <a href="http://maven.apache.org/developers/mojo-api-specification.html">
 * http://maven.apache.org/developers/mojo-api-specification.html</a>
 *
 * @see org.apache.maven.plugin.descriptor.MojoDescriptor
 */
@Component( role = MojoDescriptorExtractor.class, hint = "java-javadoc" )
public class JavaJavadocMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor, JavadocMojoAnnotation
{
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
        DocletTag aggregator = findInClassHierarchy( javaClass, JavadocMojoAnnotation.AGGREGATOR );
        if ( aggregator != null )
        {
            mojoDescriptor.setAggregator( true );
        }

        // Configurator hint
        DocletTag configurator = findInClassHierarchy( javaClass, JavadocMojoAnnotation.CONFIGURATOR );
        if ( configurator != null )
        {
            mojoDescriptor.setComponentConfigurator( configurator.getValue() );
        }

        // Additional phase to execute first
        DocletTag execute = findInClassHierarchy( javaClass, JavadocMojoAnnotation.EXECUTE );
        if ( execute != null )
        {
            String executePhase = execute.getNamedParameter( JavadocMojoAnnotation.EXECUTE_PHASE );
            String executeGoal = execute.getNamedParameter( JavadocMojoAnnotation.EXECUTE_GOAL );

            if ( executePhase == null && executeGoal == null )
            {
                throw new InvalidPluginDescriptorException( javaClass.getFullyQualifiedName()
                    + ": @execute tag requires either a 'phase' or 'goal' parameter" );
            }
            else if ( executePhase != null && executeGoal != null )
            {
                throw new InvalidPluginDescriptorException( javaClass.getFullyQualifiedName()
                    + ": @execute tag can have only one of a 'phase' or 'goal' parameter" );
            }
            mojoDescriptor.setExecutePhase( executePhase );
            mojoDescriptor.setExecuteGoal( executeGoal );

            String lifecycle = execute.getNamedParameter( JavadocMojoAnnotation.EXECUTE_LIFECYCLE );
            if ( lifecycle != null )
            {
                mojoDescriptor.setExecuteLifecycle( lifecycle );
                if ( mojoDescriptor.getExecuteGoal() != null )
                {
                    throw new InvalidPluginDescriptorException( javaClass.getFullyQualifiedName()
                        + ": @execute lifecycle requires a phase instead of a goal" );
                }
            }
        }

        // Goal name
        DocletTag goal = findInClassHierarchy( javaClass, JavadocMojoAnnotation.GOAL );
        if ( goal != null )
        {
            mojoDescriptor.setGoal( goal.getValue() );
        }

        // inheritByDefault flag
        boolean value =
            getBooleanTagValue( javaClass, JavadocMojoAnnotation.INHERIT_BY_DEFAULT,
                                mojoDescriptor.isInheritedByDefault() );
        mojoDescriptor.setInheritedByDefault( value );

        // instantiationStrategy
        DocletTag tag = findInClassHierarchy( javaClass, JavadocMojoAnnotation.INSTANTIATION_STRATEGY );
        if ( tag != null )
        {
            mojoDescriptor.setInstantiationStrategy( tag.getValue() );
        }

        // executionStrategy (and deprecated @attainAlways)
        tag = findInClassHierarchy( javaClass, JavadocMojoAnnotation.MULTI_EXECUTION_STRATEGY );
        if ( tag != null )
        {
            getLogger().warn( "@" + JavadocMojoAnnotation.MULTI_EXECUTION_STRATEGY + " in "
                                  + javaClass.getFullyQualifiedName() + " is deprecated: please use '@"
                                  + JavadocMojoAnnotation.EXECUTION_STATEGY + " always' instead." );
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.MULTI_PASS_EXEC_STRATEGY );
        }
        else
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.SINGLE_PASS_EXEC_STRATEGY );
        }
        tag = findInClassHierarchy( javaClass, JavadocMojoAnnotation.EXECUTION_STATEGY );
        if ( tag != null )
        {
            mojoDescriptor.setExecutionStrategy( tag.getValue() );
        }

        // Phase name
        DocletTag phase = findInClassHierarchy( javaClass, JavadocMojoAnnotation.PHASE );
        if ( phase != null )
        {
            mojoDescriptor.setPhase( phase.getValue() );
        }

        // Dependency resolution flag
        DocletTag requiresDependencyResolution =
            findInClassHierarchy( javaClass, JavadocMojoAnnotation.REQUIRES_DEPENDENCY_RESOLUTION );
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
            findInClassHierarchy( javaClass, JavadocMojoAnnotation.REQUIRES_DEPENDENCY_COLLECTION );
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
            getBooleanTagValue( javaClass, JavadocMojoAnnotation.REQUIRES_DIRECT_INVOCATION,
                                mojoDescriptor.isDirectInvocationOnly() );
        mojoDescriptor.setDirectInvocationOnly( value );

        // Online flag
        value =
            getBooleanTagValue( javaClass, JavadocMojoAnnotation.REQUIRES_ONLINE, mojoDescriptor.isOnlineRequired() );
        mojoDescriptor.setOnlineRequired( value );

        // Project flag
        value =
            getBooleanTagValue( javaClass, JavadocMojoAnnotation.REQUIRES_PROJECT, mojoDescriptor.isProjectRequired() );
        mojoDescriptor.setProjectRequired( value );

        // requiresReports flag
        value =
            getBooleanTagValue( javaClass, JavadocMojoAnnotation.REQUIRES_REPORTS, mojoDescriptor.isRequiresReports() );
        mojoDescriptor.setRequiresReports( value );

        // ----------------------------------------------------------------------
        // Javadoc annotations in alphabetical order
        // ----------------------------------------------------------------------

        // Deprecation hint
        DocletTag deprecated = javaClass.getTagByName( JavadocMojoAnnotation.DEPRECATED );
        if ( deprecated != null )
        {
            mojoDescriptor.setDeprecated( deprecated.getValue() );
        }

        // What version it was introduced in
        DocletTag since = findInClassHierarchy( javaClass, JavadocMojoAnnotation.SINCE );
        if ( since != null )
        {
            mojoDescriptor.setSince( since.getValue() );
        }

        // Thread-safe mojo 

        value = getBooleanTagValue( javaClass, JavadocMojoAnnotation.THREAD_SAFE, true, mojoDescriptor.isThreadSafe() );
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

            JavaType type = field.getType();

            Parameter pd = new Parameter();

            pd.setName( entry.getKey() );

            pd.setType( type.getFullyQualifiedName() );

            pd.setDescription( field.getComment() );

            DocletTag deprecationTag = field.getTagByName( JavadocMojoAnnotation.DEPRECATED );

            if ( deprecationTag != null )
            {
                pd.setDeprecated( deprecationTag.getValue() );
            }

            DocletTag sinceTag = field.getTagByName( JavadocMojoAnnotation.SINCE );
            if ( sinceTag != null )
            {
                pd.setSince( sinceTag.getValue() );
            }

            DocletTag componentTag = field.getTagByName( JavadocMojoAnnotation.COMPONENT );

            if ( componentTag != null )
            {
                // Component tag
                String role = componentTag.getNamedParameter( JavadocMojoAnnotation.COMPONENT_ROLE );

                if ( role == null )
                {
                    role = field.getType().toString();
                }

                String roleHint = componentTag.getNamedParameter( JavadocMojoAnnotation.COMPONENT_ROLEHINT );

                if ( roleHint == null )
                {
                    // support alternate syntax for better compatibility with the Plexus CDC.
                    roleHint = componentTag.getNamedParameter( "role-hint" );
                }

                // recognize Maven-injected objects as components annotations instead of parameters
                // Note: the expressions we are looking for, i.e. "${project}", are in the values of the Map,
                // so the lookup mechanism is different here than in maven-plugin-tools-annotations
                boolean isDeprecated = PluginUtils.MAVEN_COMPONENTS.containsValue( role );

                if ( !isDeprecated )
                {
                    // normal component
                    pd.setRequirement( new Requirement( role, roleHint ) );
                }
                else
                {
                    // not a component but a Maven object to be transformed into an expression/property
                    getLogger().warn( "Deprecated @component Javadoc tag for '" + pd.getName() + "' field in "
                                          + javaClass.getFullyQualifiedName()
                                          + ": replace with @Parameter( defaultValue = \"" + role
                                          + "\", readonly = true )" );
                    pd.setDefaultValue( role );
                    pd.setRequired( true );
                }

                pd.setEditable( false );
                /* TODO: or better like this? Need @component fields be editable for the user?
                pd.setEditable( field.getTagByName( READONLY ) == null );
                */
            }
            else
            {
                // Parameter tag
                DocletTag parameter = field.getTagByName( JavadocMojoAnnotation.PARAMETER );

                pd.setRequired( field.getTagByName( JavadocMojoAnnotation.REQUIRED ) != null );

                pd.setEditable( field.getTagByName( JavadocMojoAnnotation.READONLY ) == null );

                String name = parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_NAME );

                if ( !StringUtils.isEmpty( name ) )
                {
                    pd.setName( name );
                }

                String alias = parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_ALIAS );

                if ( !StringUtils.isEmpty( alias ) )
                {
                    pd.setAlias( alias );
                }

                String expression = parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_EXPRESSION );
                String property = parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_PROPERTY );

                if ( StringUtils.isNotEmpty( expression ) && StringUtils.isNotEmpty( property ) )
                {
                    getLogger().error( javaClass.getFullyQualifiedName() + "#" + field.getName() + ":" );
                    getLogger().error( "  Cannot use both:" );
                    getLogger().error( "    @parameter expression=\"${property}\"" );
                    getLogger().error( "  and" );
                    getLogger().error( "    @parameter property=\"property\"" );
                    getLogger().error( "  Second syntax is preferred." );
                    throw new InvalidParameterException( javaClass.getFullyQualifiedName() + "#" + field.getName()
                        + ": cannot" + " use both @parameter expression and property", null );
                }

                if ( StringUtils.isNotEmpty( expression ) )
                {
                    getLogger().warn( javaClass.getFullyQualifiedName() + "#" + field.getName() + ":" );
                    getLogger().warn( "  The syntax" );
                    getLogger().warn( "    @parameter expression=\"${property}\"" );
                    getLogger().warn( "  is deprecated, please use" );
                    getLogger().warn( "    @parameter property=\"property\"" );
                    getLogger().warn( "  instead." );

                }
                else if ( StringUtils.isNotEmpty( property ) )
                {
                    expression = "${" + property + "}";
                }

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

                pd.setDefaultValue( parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_DEFAULT_VALUE ) );

                pd.setImplementation( parameter.getNamedParameter( JavadocMojoAnnotation.PARAMETER_IMPLEMENTATION ) );
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

        for ( JavaField field : javaClass.getFields() )
        {
            if ( field.getTagByName( JavadocMojoAnnotation.PARAMETER ) != null
                || field.getTagByName( JavadocMojoAnnotation.COMPONENT ) != null )
            {
                rawParams.put( field.getName(), field );
            }
        }
        return rawParams;
    }

    /** {@inheritDoc} */
    public List<MojoDescriptor> execute( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Collection<JavaClass> javaClasses = discoverClasses( request );

        List<MojoDescriptor> descriptors = new ArrayList<>();

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
    protected Collection<JavaClass> discoverClasses( final PluginToolsRequest request )
    {
        JavaProjectBuilder builder = new JavaProjectBuilder( new SortedClassLibraryBuilder() );
        builder.setEncoding( request.getEncoding() );
        
         // Build isolated Classloader with only the artifacts of the project (none of this plugin) 
        List<URL> urls = new ArrayList<>( request.getDependencies().size() );
        for ( Artifact artifact : request.getDependencies() )
        {
            try
            {
                urls.add( artifact.getFile().toURI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                // noop
            }
        }
        builder.addClassLoader( new URLClassLoader( urls.toArray( new URL[0] ), ClassLoader.getSystemClassLoader() ) );
        
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
