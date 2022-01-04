package org.apache.maven.plugin.plugin;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.ReaderFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class for this Plugin.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 */
public abstract class AbstractGeneratorMojo
    extends AbstractMojo
{
    /**
     * The project currently being built.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     * The component used for scanning the source tree for mojos.
     */
    @Component
    protected MojoScanner mojoScanner;

    @Component
    protected BuildContext buildContext;

    /**
     * The file encoding of the source files.
     *
     * @since 2.5
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

    /**
     * The goal prefix that will appear before the ":".
     */
    @Parameter
    protected String goalPrefix;

    /**
     * By default an exception is throw if no mojo descriptor is found. As the maven-plugin is defined in core, the
     * descriptor generator mojo is bound to generate-resources phase.
     * But for annotations, the compiled classes are needed, so skip error
     *
     * @since 3.0
     */
    @Parameter( property = "maven.plugin.skipErrorNoDescriptorsFound", defaultValue = "false" )
    protected boolean skipErrorNoDescriptorsFound;

    /**
     * <p>
     * The role names of mojo extractors to use.
     * </p>
     * <p>
     * If not set, all mojo extractors will be used. If set to an empty extractor name, no mojo extractors
     * will be used.
     * </p>
     * Example:
     * <pre>
     *  &lt;!-- Use all mojo extractors --&gt;
     *  &lt;extractors/&gt;
     *
     *  &lt;!-- Use no mojo extractors --&gt;
     *  &lt;extractors&gt;
     *      &lt;extractor/&gt;
     *  &lt;/extractors&gt;
     *
     *  &lt;!-- Use only bsh mojo extractor --&gt;
     *  &lt;extractors&gt;
     *      &lt;extractor&gt;bsh&lt;/extractor&gt;
     *  &lt;/extractors&gt;
     * </pre>
     */
    @Parameter
    protected Set<String> extractors;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.skip" )
    protected boolean skip;

    /**
     * Specify the dependencies as {@code groupId:artifactId} containing (abstract) Mojos, to filter
     * dependencies scanned at runtime and focus on dependencies that are really useful to Mojo analysis.
     * By default, the value is {@code null} and all dependencies are scanned (as before this parameter was added).
     * If specified in the configuration with no children, no dependencies are scanned.
     * 
     * @since 3.5
     */
    @Parameter
    private List<String> mojoDependencies = null;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Location of the local repository.
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository local;

    /**
     * Maven plugin packaging types. Default is single "maven-plugin".
     * 
     * @since 3.3
     */
    @Parameter
    protected List<String> packagingTypes = Collections.singletonList( "maven-plugin" );

    /**
     * Flag controlling is "expected dependencies in provided scope" check to be performed or not. Default value:
     * {@code true}.
     *
     * @since 3.6.3
     */
    @Parameter( defaultValue = "true", property = "maven.plugin.checkExpectedProvidedScope" )
    private boolean checkExpectedProvidedScope = true;

    /**
     * List of {@code groupId} strings of artifact coordinates that are expected to be in "provided" scope. Default
     * value: {@code ["org.apache.maven"]}.
     *
     * @since 3.6.3
     */
    @Parameter
    private List<String> expectedProvidedScopeGroupIds = Collections.singletonList( "org.apache.maven" );

    /**
     * List of {@code groupId:artifactId} strings of artifact coordinates that are to be excluded from "expected
     * provided scope" check. Default value: {@code ["org.apache.maven:maven-archiver", "org.apache.maven:maven-jxr"]}.
     *
     * @since 3.6.3
     */
    @Parameter
    private List<String> expectedProvidedScopeExclusions = Arrays.asList(
            "org.apache.maven:maven-archiver",
            "org.apache.maven:maven-jxr" );

    /**
     * @return the output directory where files will be generated.
     */
    protected abstract File getOutputDirectory();

    /**
     * @return the wanted <code>Generator</code> implementation.
     */
    protected abstract Generator createGenerator();

    /**
     * System/OS line separator: used to format console messages.
     */
    private static final String LS = System.lineSeparator();

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( !packagingTypes.contains( project.getPackaging() ) )
        {
            getLog().info( "Unsupported packaging type " + project.getPackaging() + ", execution skipped" );
            return;
        }
        if ( skip )
        {
            getLog().warn( "Execution skipped" );
            return;
        }

        if ( !"maven-plugin".equalsIgnoreCase( project.getArtifactId() )
            && project.getArtifactId().toLowerCase().startsWith( "maven-" )
            && project.getArtifactId().toLowerCase().endsWith( "-plugin" ) 
            && !"org.apache.maven.plugins".equals( project.getGroupId() ) )
        {
            getLog().error( LS + LS + "Artifact Ids of the format maven-___-plugin are reserved for" + LS
                                + "plugins in the Group Id org.apache.maven.plugins" + LS
                                + "Please change your artifactId to the format ___-maven-plugin" + LS
                                + "In the future this error will break the build." + LS + LS );
        }

        if ( checkExpectedProvidedScope )
        {
            Set<Artifact> wrongScopedArtifacts = dependenciesNotInProvidedScope();
            if ( !wrongScopedArtifacts.isEmpty() )
            {
                StringBuilder errorMessage = new StringBuilder(
                        LS + LS + "Some dependencies of Maven Plugins are expected to be in provided scope." + LS
                        + "Please make sure that dependencies listed below declared in POM" + LS
                        + "have set '<scope>provided</scope>' as well." + LS + LS
                        + "The following dependencies are in wrong scope:" + LS
                );
                for ( Artifact artifact : wrongScopedArtifacts )
                {
                    errorMessage.append( " * " ).append( artifact ).append( LS );
                }
                errorMessage.append( LS ).append( LS );

                getLog().error( errorMessage.toString() );
            }
        }

        String defaultGoalPrefix = getDefaultGoalPrefix( project );
          
        if ( goalPrefix == null )
        {
            goalPrefix = defaultGoalPrefix;
        }
        else if ( !goalPrefix.equals( defaultGoalPrefix ) )
        {
            getLog().warn(
                LS + LS + "Goal prefix is specified as: '" + goalPrefix + "'. " + "Maven currently expects it to be '"
                    + defaultGoalPrefix + "'." + LS );
        }

        mojoScanner.setActiveExtractors( extractors );

        // TODO: could use this more, eg in the writing of the plugin descriptor!
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setGroupId( project.getGroupId() );

        pluginDescriptor.setArtifactId( project.getArtifactId() );

        pluginDescriptor.setVersion( project.getVersion() );

        pluginDescriptor.setGoalPrefix( goalPrefix );

        pluginDescriptor.setName( project.getName() );

        pluginDescriptor.setDescription( project.getDescription() );

        if ( encoding == null || encoding.length() < 1 )
        {
            getLog().warn( "Using platform encoding (" + ReaderFactory.FILE_ENCODING
                               + " actually) to read mojo source files, i.e. build is platform dependent!" );
        }
        else
        {
            getLog().info( "Using '" + encoding + "' encoding to read mojo source files." );
        }

        try
        {
            List<ComponentDependency> deps = GeneratorUtils.toComponentDependencies( project.getArtifacts() );
            pluginDescriptor.setDependencies( deps );

            PluginToolsRequest request = new DefaultPluginToolsRequest( project, pluginDescriptor );
            request.setEncoding( encoding );
            request.setSkipErrorNoDescriptorsFound( skipErrorNoDescriptorsFound );
            request.setDependencies( filterMojoDependencies() );
            request.setLocal( this.local );
            request.setRemoteRepos( this.remoteRepos );

            mojoScanner.populatePluginDescriptor( request );

            File outputDirectory = getOutputDirectory();
            outputDirectory.mkdirs();

            createGenerator().execute( outputDirectory, request );
            buildContext.refresh( outputDirectory );
        }
        catch ( GeneratorException e )
        {
            throw new MojoExecutionException( "Error writing plugin descriptor", e );
        }
        catch ( InvalidPluginDescriptorException | ExtractionException e )
        {
            throw new MojoExecutionException( "Error extracting plugin descriptor: '" + e.getLocalizedMessage() + "'",
                                              e );
        }
        catch ( LinkageError e )
        {
            throw new MojoExecutionException( "The API of the mojo scanner is not compatible with this plugin version."
                + " Please check the plugin dependencies configured in the POM and ensure the versions match.", e );
        }
    }

    static String getDefaultGoalPrefix( MavenProject project )
    {
        String defaultGoalPrefix;
        if ( "maven-plugin".equalsIgnoreCase( project.getArtifactId() ) )
        {
            defaultGoalPrefix = project.getGroupId().substring( project.getGroupId().lastIndexOf( '.' ) + 1 );
        }
        else
        {
            defaultGoalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        }
        return defaultGoalPrefix;
    }

    /**
     * Collects all dependencies expected to be in "provided" scope but are NOT in "provided" scope.
     */
    private Set<Artifact> dependenciesNotInProvidedScope()
    {
        LinkedHashSet<Artifact> wrongScopedDependencies = new LinkedHashSet<>();

        for ( Artifact dependency : project.getArtifacts() )
        {
            String ga = dependency.getGroupId() + ":" + dependency.getArtifactId();
            if ( expectedProvidedScopeGroupIds.contains( dependency.getGroupId() )
                && !expectedProvidedScopeExclusions.contains( ga )
                && !Artifact.SCOPE_PROVIDED.equals( dependency.getScope() ) )
            {
                wrongScopedDependencies.add( dependency );
            }
        }

        return wrongScopedDependencies;
    }

    /**
     * Get dependencies filtered with mojoDependencies configuration.
     * 
     * @return eventually filtered dependencies, or even <code>null</code> if configured with empty mojoDependencies
     * list
     * @see #mojoDependencies
     */
    private Set<Artifact> filterMojoDependencies()
    {
        Set<Artifact> filteredArtifacts;
        if ( mojoDependencies == null )
        {
            filteredArtifacts = new LinkedHashSet<>( project.getArtifacts() );
        }
        else if ( mojoDependencies.size() == 0 )
        {
            filteredArtifacts = null;
        }
        else
        {
            filteredArtifacts = new LinkedHashSet<>();
            
            ArtifactFilter filter = new IncludesArtifactFilter( mojoDependencies );

            for ( Artifact artifact : project.getArtifacts() )
            {
                if ( filter.include( artifact ) )
                {
                    filteredArtifacts.add( artifact );
                }
            }
        }

        return filteredArtifacts;
    }
}
