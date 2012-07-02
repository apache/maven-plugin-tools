package org.apache.maven.tools.plugin.annotations;

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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScannerRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * JavaMojoDescriptorExtractor, a MojoDescriptor extractor to read descriptors from java classes with annotations.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Component( role = MojoDescriptorExtractor.class, hint = "java-annotations" )
public class JavaAnnotationsMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor
{

    @org.codehaus.plexus.component.annotations.Requirement
    private MojoAnnotationsScanner mojoAnnotationsScanner;

    @org.codehaus.plexus.component.annotations.Requirement
    private ArtifactResolver artifactResolver;

    @org.codehaus.plexus.component.annotations.Requirement
    private ArtifactFactory artifactFactory;

    @org.codehaus.plexus.component.annotations.Requirement
    private ArchiverManager archiverManager;

    public List<MojoDescriptor> execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        return execute( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
    }

    public List<MojoDescriptor> execute( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = scanAnnotations( request );

        Map<String, JavaClass> javaClassesMap = scanJavadoc( request, mojoAnnotatedClasses.values() );

        populateDataFromJavadoc( mojoAnnotatedClasses, javaClassesMap );

        return toMojoDescriptors( mojoAnnotatedClasses, request.getPluginDescriptor() );
    }

    private Map<String, MojoAnnotatedClass> scanAnnotations( PluginToolsRequest request )
        throws ExtractionException
    {
        MojoAnnotationsScannerRequest mojoAnnotationsScannerRequest = new MojoAnnotationsScannerRequest();

        File output = new File( request.getProject().getBuild().getOutputDirectory() );
        mojoAnnotationsScannerRequest.setClassesDirectories( Arrays.asList( output ) );

        mojoAnnotationsScannerRequest.setDependencies( request.getDependencies() );

        mojoAnnotationsScannerRequest.setProject( request.getProject() );

        return mojoAnnotationsScanner.scan( mojoAnnotationsScannerRequest );
    }

    private Map<String, JavaClass> scanJavadoc( PluginToolsRequest request,
                                                Collection<MojoAnnotatedClass> mojoAnnotatedClasses )
        throws ExtractionException
    {
        // found artifact from reactors to scan sources
        // we currently only scan sources from reactors
        List<MavenProject> mavenProjects = new ArrayList<MavenProject>();

        // if we need to scan sources from external artifacts
        Set<Artifact> externalArtifacts = new HashSet<Artifact>();

        for ( MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses )
        {
            if ( StringUtils.equals( mojoAnnotatedClass.getArtifact().getArtifactId(),
                                     request.getProject().getArtifact().getArtifactId() ) )
            {
                continue;
            }

            if ( !isMojoAnnnotatedClassCandidate( mojoAnnotatedClass ) )
            {
                // we don't scan sources for classes without mojo annotations
                continue;
            }

            MavenProject mavenProject =
                getFromProjectReferences( mojoAnnotatedClass.getArtifact(), request.getProject() );

            if ( mavenProject != null )
            {
                mavenProjects.add( mavenProject );
            }
            else
            {
                externalArtifacts.add( mojoAnnotatedClass.getArtifact() );
            }
        }

        Map<String, JavaClass> javaClassesMap = new HashMap<String, JavaClass>();

        // try to get artifact with sources classifier, extract somewhere then scan for @since, @deprecated
        for ( Artifact artifact : externalArtifacts )
        {
            // parameter for test-sources too ?? olamy I need that for it test only
            if ( StringUtils.equalsIgnoreCase( "tests", artifact.getClassifier() ) )
            {
                javaClassesMap.putAll( discoverClassesFromSourcesJar( artifact, request, "test-sources" ) );
            }
            else
            {
                javaClassesMap.putAll( discoverClassesFromSourcesJar( artifact, request, "sources" ) );
            }

        }

        for ( MavenProject mavenProject : mavenProjects )
        {
            javaClassesMap.putAll( discoverClasses( request.getEncoding(), mavenProject ) );
        }

        javaClassesMap.putAll( discoverClasses( request ) );

        return javaClassesMap;
    }

    private boolean isMojoAnnnotatedClassCandidate( MojoAnnotatedClass mojoAnnotatedClass )
    {
        if ( mojoAnnotatedClass == null )
        {
            return false;
        }
        return ( !mojoAnnotatedClass.getComponents().isEmpty() || !mojoAnnotatedClass.getParameters().isEmpty()
            || mojoAnnotatedClass.getExecute() != null || mojoAnnotatedClass.getMojo() != null );

    }

    protected Map<String, JavaClass> discoverClassesFromSourcesJar( Artifact artifact, PluginToolsRequest request,
                                                                    String classifier )
        throws ExtractionException
    {
        try
        {
            Artifact sourcesArtifact =
                artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                              artifact.getVersion(), artifact.getType(), classifier );

            artifactResolver.resolve( sourcesArtifact, request.getRemoteRepos(), request.getLocal() );

            if ( sourcesArtifact.getFile() == null || !sourcesArtifact.getFile().exists() )
            {
                // could not get artifact sources
                return Collections.emptyMap();
            }

            // extract sources to target/maven-plugin-plugin-sources/${groupId}/${artifact}/sources
            File extractDirectory = new File( request.getProject().getBuild().getDirectory(),
                                              "maven-plugin-plugin-sources/" + sourcesArtifact.getGroupId() + "/"
                                                  + sourcesArtifact.getArtifactId() + "/" + sourcesArtifact.getVersion()
                                                  + "/" + sourcesArtifact.getClassifier() );
            extractDirectory.mkdirs();

            UnArchiver unArchiver = archiverManager.getUnArchiver( "jar" );
            unArchiver.setSourceFile( sourcesArtifact.getFile() );
            unArchiver.setDestDirectory( extractDirectory );
            unArchiver.extract();

            return discoverClasses( request.getEncoding(), Arrays.asList( extractDirectory ) );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            //throw new ExtractionException( e.getMessage(), e );
            getLogger().debug( "skip ArtifactNotFoundException:" + e.getMessage() );
            getLogger().warn(
                "Unable to get sources artifact for " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                    + artifact.getVersion() + ". Some javadoc tags (@since, @deprecated and comments) won't be used" );
            return Collections.emptyMap();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
    }

    /**
     * from sources scan to get @since and @deprecated and description of classes and fields.
     *
     * @param mojoAnnotatedClasses
     * @param javaClassesMap
     */
    protected void populateDataFromJavadoc( Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
                                            Map<String, JavaClass> javaClassesMap )
    {

        for ( Map.Entry<String, MojoAnnotatedClass> entry : mojoAnnotatedClasses.entrySet() )
        {
            JavaClass javaClass = javaClassesMap.get( entry.getKey() );
            if ( javaClass == null )
            {
                continue;
            }

            // populate class-level content
            MojoAnnotationContent mojoAnnotationContent = entry.getValue().getMojo();
            if ( mojoAnnotationContent != null )
            {
                mojoAnnotationContent.setDescription( javaClass.getComment() );

                DocletTag since = findInClassHierarchy( javaClass, "since" );
                if ( since != null )
                {
                    mojoAnnotationContent.setSince( since.getValue() );
                }

                DocletTag deprecated = findInClassHierarchy( javaClass, "deprecated" );
                if ( deprecated != null )
                {
                    mojoAnnotationContent.setDeprecated( deprecated.getValue() );
                }
            }

            Map<String, JavaField> fieldsMap = extractFieldParameterTags( javaClass, javaClassesMap );

            // populate parameters
            Map<String, ParameterAnnotationContent> parameters =
                getParametersParentHierarchy( entry.getValue(), new HashMap<String, ParameterAnnotationContent>(),
                                              mojoAnnotatedClasses );
            for ( Map.Entry<String, ParameterAnnotationContent> parameter : new TreeMap<String, ParameterAnnotationContent>(
                parameters ).entrySet() )
            {
                JavaField javaField = fieldsMap.get( parameter.getKey() );
                if ( javaField == null )
                {
                    continue;
                }

                ParameterAnnotationContent parameterAnnotationContent = parameter.getValue();
                parameterAnnotationContent.setDescription( javaField.getComment() );

                DocletTag deprecated = javaField.getTagByName( "deprecated" );
                if ( deprecated != null )
                {
                    parameterAnnotationContent.setDeprecated( deprecated.getValue() );
                }

                DocletTag since = javaField.getTagByName( "since" );
                if ( since != null )
                {
                    parameterAnnotationContent.setSince( since.getValue() );
                }
            }

            // populate components
            for ( Map.Entry<String, ComponentAnnotationContent> component : entry.getValue().getComponents().entrySet() )
            {
                JavaField javaField = fieldsMap.get( component.getKey() );
                if ( javaField == null )
                {
                    continue;
                }

                ComponentAnnotationContent componentAnnotationContent = component.getValue();
                componentAnnotationContent.setDescription( javaField.getComment() );

                DocletTag deprecated = javaField.getTagByName( "deprecated" );
                if ( deprecated != null )
                {
                    componentAnnotationContent.setDeprecated( deprecated.getValue() );
                }

                DocletTag since = javaField.getTagByName( "since" );
                if ( since != null )
                {
                    componentAnnotationContent.setSince( since.getValue() );
                }
            }

        }

    }

    /**
     * @param javaClass not null
     * @param tagName   not null
     * @return docletTag instance
     */
    private DocletTag findInClassHierarchy( JavaClass javaClass, String tagName )
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
     * extract fields that are either parameters or components.
     *
     * @param javaClass not null
     * @return map with Mojo parameters names as keys
     */
    private Map<String, JavaField> extractFieldParameterTags( JavaClass javaClass,
                                                              Map<String, JavaClass> javaClassesMap )
    {
        Map<String, JavaField> rawParams = new TreeMap<String, com.thoughtworks.qdox.model.JavaField>();

        // we have to add the parent fields first, so that they will be overwritten by the local fields if
        // that actually happens...
        JavaClass superClass = javaClass.getSuperJavaClass();

        if ( superClass != null )
        {
            if ( superClass.getFields().length > 0 )
            {
                rawParams = extractFieldParameterTags( superClass, javaClassesMap );
            }
            // maybe sources comes from scan of sources artifact
            superClass = javaClassesMap.get( superClass.getFullyQualifiedName() );
            if ( superClass != null )
            {
                rawParams = extractFieldParameterTags( superClass, javaClassesMap );
            }
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
                rawParams.put( field.getName(), field );
            }
        }
        return rawParams;
    }

    protected Map<String, JavaClass> discoverClasses( final PluginToolsRequest request )
    {
        return discoverClasses( request.getEncoding(), request.getProject() );
    }

    @SuppressWarnings( "unchecked" )
    protected Map<String, JavaClass> discoverClasses( final String encoding, final MavenProject project )
    {
        List<File> sources = new ArrayList<File>();

        for ( String source : (List<String>) project.getCompileSourceRoots() )
        {
            sources.add( new File( source ) );
        }

        // TODO be more dynamic
        File generatedPlugin = new File( project.getBasedir(), "target/generated-sources/plugin" );
        if ( !project.getCompileSourceRoots().contains( generatedPlugin.getAbsolutePath() )
            && generatedPlugin.exists() )
        {
            sources.add( generatedPlugin );
        }

        return discoverClasses( encoding, sources );
    }

    protected Map<String, JavaClass> discoverClasses( final String encoding, List<File> sourceDirectories )
    {
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( encoding );

        for ( File source : sourceDirectories )
        {
            builder.addSourceTree( source );
        }

        JavaClass[] javaClasses = builder.getClasses();

        if ( javaClasses == null || javaClasses.length < 1 )
        {
            return Collections.emptyMap();
        }

        Map<String, JavaClass> javaClassMap = new HashMap<String, JavaClass>( javaClasses.length );

        for ( JavaClass javaClass : javaClasses )
        {
            javaClassMap.put( javaClass.getFullyQualifiedName(), javaClass );
        }

        return javaClassMap;
    }

    private List<MojoDescriptor> toMojoDescriptors( Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
                                                    PluginDescriptor pluginDescriptor )
        throws DuplicateParameterException, InvalidParameterException
    {
        List<MojoDescriptor> mojoDescriptors = new ArrayList<MojoDescriptor>( mojoAnnotatedClasses.size() );
        for ( MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses.values() )
        {
            // no mojo so skip it
            if ( mojoAnnotatedClass.getMojo() == null )
            {
                continue;
            }

            ExtendedMojoDescriptor mojoDescriptor = new ExtendedMojoDescriptor();

            //mojoDescriptor.setRole( mojoAnnotatedClass.getClassName() );
            //mojoDescriptor.setRoleHint( "default" );
            mojoDescriptor.setImplementation( mojoAnnotatedClass.getClassName() );
            mojoDescriptor.setLanguage( "java" );

            MojoAnnotationContent mojo = mojoAnnotatedClass.getMojo();

            mojoDescriptor.setDescription( mojo.getDescription() );
            mojoDescriptor.setSince( mojo.getSince() );
            mojo.setDeprecated( mojo.getDeprecated() );

            mojoDescriptor.setProjectRequired( mojo.requiresProject() );

            mojoDescriptor.setRequiresReports( mojo.requiresReports() );

            mojoDescriptor.setComponentConfigurator( mojo.configurator() );

            mojoDescriptor.setInheritedByDefault( mojo.inheritByDefault() );

            String instantiationStrategy = mojo.instantiationStrategy().id();
            if ( instantiationStrategy == null )
            {
                /* Perhaps the original spelling is there. */
                instantiationStrategy = mojo.instanciationStrategy().id();
            }
            mojoDescriptor.setInstantiationStrategy( instantiationStrategy );

            mojoDescriptor.setAggregator( mojo.aggregator() );
            mojoDescriptor.setDependencyResolutionRequired( mojo.requiresDependencyResolution().id() );
            mojoDescriptor.setDependencyCollectionRequired( mojo.requiresDependencyCollection().id() );

            mojoDescriptor.setDirectInvocationOnly( mojo.requiresDirectInvocation() );
            mojoDescriptor.setDeprecated( mojo.getDeprecated() );
            mojoDescriptor.setThreadSafe( mojo.threadSafe() );

            ExecuteAnnotationContent execute = findExecuteInParentHierarchy( mojoAnnotatedClass, mojoAnnotatedClasses );
            if ( execute != null )
            {
                mojoDescriptor.setExecuteGoal( execute.goal() );
                mojoDescriptor.setExecuteLifecycle( execute.lifecycle() );
                if ( execute.phase() != null )
                {
                    mojoDescriptor.setExecutePhase( execute.phase().id() );
                }
            }

            mojoDescriptor.setExecutionStrategy( mojo.executionStrategy() );
            // ???
            //mojoDescriptor.alwaysExecute(mojo.a)

            mojoDescriptor.setGoal( mojo.name() );
            mojoDescriptor.setOnlineRequired( mojo.requiresOnline() );

            mojoDescriptor.setPhase( mojo.defaultPhase().id() );

            // Parameter annotations
            Map<String, ParameterAnnotationContent> parameters =
                getParametersParentHierarchy( mojoAnnotatedClass, new HashMap<String, ParameterAnnotationContent>(),
                                              mojoAnnotatedClasses );

            for ( ParameterAnnotationContent parameterAnnotationContent : new TreeSet<ParameterAnnotationContent>(
                parameters.values() ) )
            {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                    new org.apache.maven.plugin.descriptor.Parameter();
                parameter.setName( parameterAnnotationContent.getFieldName() );
                parameter.setAlias( parameterAnnotationContent.alias() );
                parameter.setDefaultValue( parameterAnnotationContent.defaultValue() );
                parameter.setDeprecated( parameterAnnotationContent.getDeprecated() );
                parameter.setDescription( parameterAnnotationContent.getDescription() );
                parameter.setEditable( !parameterAnnotationContent.readonly() );
                String property = parameterAnnotationContent.property();
                if ( StringUtils.contains( property, '$' ) || StringUtils.contains( property, '{' )
                    || StringUtils.contains( property, '}' ) )
                {
                    throw new InvalidParameterException(
                        "Invalid property for parameter '" + parameter.getName() + "', " + "forbidden characters ${}: "
                            + property, null );
                }
                parameter.setExpression( StringUtils.isEmpty( property ) ? "" : "${" + property + "}" );
                parameter.setType( parameterAnnotationContent.getClassName() );
                parameter.setSince( parameterAnnotationContent.getSince() );
                parameter.setRequired( parameterAnnotationContent.required() );

                mojoDescriptor.addParameter( parameter );
            }

            // Component annotations
            Map<String, ComponentAnnotationContent> components =
                getComponentsParentHierarchy( mojoAnnotatedClass, new HashMap<String, ComponentAnnotationContent>(),
                                              mojoAnnotatedClasses );

            for ( ComponentAnnotationContent componentAnnotationContent : new TreeSet<ComponentAnnotationContent>(
                components.values() ) )
            {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                    new org.apache.maven.plugin.descriptor.Parameter();
                parameter.setName( componentAnnotationContent.getFieldName() );

                // recognize Maven-injected objects as components annotations instead of parameters
                String expression = PluginUtils.MAVEN_COMPONENTS.get( componentAnnotationContent.getRoleClassName() );
                if ( expression == null )
                {
                    // normal component
                    parameter.setRequirement( new Requirement( componentAnnotationContent.getRoleClassName(),
                                                               componentAnnotationContent.hint() ) );
                }
                else
                {
                    // not a component but a Maven object to be transformed into an expression/property
                    parameter.setDefaultValue( expression );
                    parameter.setType( componentAnnotationContent.getRoleClassName() );
                    parameter.setRequired( true );
                }
                parameter.setDeprecated( componentAnnotationContent.getDeprecated() );
                parameter.setSince( componentAnnotationContent.getSince() );

                // same behaviour as JavaMojoDescriptorExtractor
                //parameter.setRequired( ... );
                parameter.setEditable( false );

                mojoDescriptor.addParameter( parameter );
            }

            mojoDescriptor.setPluginDescriptor( pluginDescriptor );

            mojoDescriptors.add( mojoDescriptor );
        }
        return mojoDescriptors;
    }

    protected ExecuteAnnotationContent findExecuteInParentHierarchy( MojoAnnotatedClass mojoAnnotatedClass,
                                                                     Map<String, MojoAnnotatedClass> mojoAnnotatedClasses )
    {

        if ( mojoAnnotatedClass.getExecute() != null )
        {
            return mojoAnnotatedClass.getExecute();
        }
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if ( StringUtils.isEmpty( parentClassName ) )
        {
            return null;
        }
        MojoAnnotatedClass parent = mojoAnnotatedClasses.get( parentClassName );
        if ( parent == null )
        {
            return null;
        }
        return findExecuteInParentHierarchy( parent, mojoAnnotatedClasses );
    }


    protected Map<String, ParameterAnnotationContent> getParametersParentHierarchy(
        MojoAnnotatedClass mojoAnnotatedClass, Map<String, ParameterAnnotationContent> parameters,
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses )
    {
        List<ParameterAnnotationContent> parameterAnnotationContents = new ArrayList<ParameterAnnotationContent>();

        parameterAnnotationContents =
            getParametersParent( mojoAnnotatedClass, parameterAnnotationContents, mojoAnnotatedClasses );

        // move to parent first to build the Map
        Collections.reverse( parameterAnnotationContents );

        Map<String, ParameterAnnotationContent> map =
            new HashMap<String, ParameterAnnotationContent>( parameterAnnotationContents.size() );

        for ( ParameterAnnotationContent parameterAnnotationContent : parameterAnnotationContents )
        {
            map.put( parameterAnnotationContent.getFieldName(), parameterAnnotationContent );
        }
        return map;
    }

    protected List<ParameterAnnotationContent> getParametersParent( MojoAnnotatedClass mojoAnnotatedClass,
                                                                    List<ParameterAnnotationContent> parameterAnnotationContents,
                                                                    Map<String, MojoAnnotatedClass> mojoAnnotatedClasses )
    {
        parameterAnnotationContents.addAll( mojoAnnotatedClass.getParameters().values() );
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if ( parentClassName != null )
        {
            MojoAnnotatedClass parent = mojoAnnotatedClasses.get( parentClassName );
            if ( parent != null )
            {
                return getParametersParent( parent, parameterAnnotationContents, mojoAnnotatedClasses );
            }
        }
        return parameterAnnotationContents;
    }

    protected Map<String, ComponentAnnotationContent> getComponentsParentHierarchy(
        MojoAnnotatedClass mojoAnnotatedClass, Map<String, ComponentAnnotationContent> components,
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses )
    {
        List<ComponentAnnotationContent> componentAnnotationContents = new ArrayList<ComponentAnnotationContent>();

        componentAnnotationContents =
            getComponentParent( mojoAnnotatedClass, componentAnnotationContents, mojoAnnotatedClasses );

        // move to parent first to build the Map
        Collections.reverse( componentAnnotationContents );

        Map<String, ComponentAnnotationContent> map =
            new HashMap<String, ComponentAnnotationContent>( componentAnnotationContents.size() );

        for ( ComponentAnnotationContent componentAnnotationContent : componentAnnotationContents )
        {
            map.put( componentAnnotationContent.getFieldName(), componentAnnotationContent );
        }
        return map;
    }

    protected List<ComponentAnnotationContent> getComponentParent( MojoAnnotatedClass mojoAnnotatedClass,
                                                                   List<ComponentAnnotationContent> componentAnnotationContents,
                                                                   Map<String, MojoAnnotatedClass> mojoAnnotatedClasses )
    {
        componentAnnotationContents.addAll( mojoAnnotatedClass.getComponents().values() );
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if ( parentClassName != null )
        {
            MojoAnnotatedClass parent = mojoAnnotatedClasses.get( parentClassName );
            if ( parent != null )
            {
                return getComponentParent( parent, componentAnnotationContents, mojoAnnotatedClasses );
            }
        }
        return componentAnnotationContents;
    }

    protected MavenProject getFromProjectReferences( Artifact artifact, MavenProject project )
    {
        if ( project.getProjectReferences() == null || project.getProjectReferences().isEmpty() )
        {
            return null;
        }
        @SuppressWarnings( "unchecked" ) Collection<MavenProject> mavenProjects =
            project.getProjectReferences().values();
        for ( MavenProject mavenProject : mavenProjects )
        {
            if ( StringUtils.equals( mavenProject.getId(), artifact.getId() ) )
            {
                return mavenProject;
            }
        }
        return null;
    }

}
