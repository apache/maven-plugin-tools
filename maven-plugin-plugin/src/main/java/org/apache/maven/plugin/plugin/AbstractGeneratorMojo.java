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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Abstract class for this Plugin.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * 
 * @threadSafe
 */
public abstract class AbstractGeneratorMojo
    extends AbstractMojo
{
    /**
     * The project currently being built.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The component used for scanning the source tree for mojos.
     *
     * @component
     * @required
     */
    protected MojoScanner mojoScanner;

    /**
     * The file encoding of the source files.
     * 
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.5
     */
    protected String encoding;

    /**
     * The goal prefix that will appear before the ":".
     *
     * @parameter
     */
    protected String goalPrefix;

    /**
     * The role names of mojo extractors to use.
     * <p/>
     * If not set, all mojo extractors will be used. If set to an empty extractor name, no mojo extractors
     * will be used.
     * <p/>
     * Example:
     * <p/>
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
     *
     * @parameter
     */
    protected Set/* <String> */extractors;

    /**
     * @return the output directory where files will be generated.
     */
    protected abstract File getOutputDirectory();

    /**
     * @return the wanted <code>Generator</code> implementation.
     */
    protected abstract Generator createGenerator();

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( !"maven-plugin".equals( project.getPackaging() ) )
        {
            return;
        }

        if ( project.getArtifactId().toLowerCase().startsWith( "maven-" ) 
            && project.getArtifactId().toLowerCase().endsWith( "-plugin" )
            && !"org.apache.maven.plugins".equals( project.getGroupId() ) )
        {
            getLog().error( "\n\nArtifact Ids of the format maven-___-plugin are reserved for \n" 
                                + "plugins in the Group Id org.apache.maven.plugins\n"
                                + "Please change your artifactId to the format ___-maven-plugin\n"
                                + "In the future this error will break the build.\n\n" );
        }

        String defaultGoalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        if ( goalPrefix == null )
        {
            goalPrefix = defaultGoalPrefix;
        }
        else if ( !goalPrefix.equals( defaultGoalPrefix ) )
        {
            getLog().warn(
                           "\n\nGoal prefix is specified as: '" + goalPrefix + "'. "
                               + "Maven currently expects it to be '" + defaultGoalPrefix + "'.\n" );
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
                                  + " actually) to read mojo metadata, i.e. build is platform dependent!" );
        }
        else
        {
            getLog().info( "Using '" + encoding + "' encoding to read mojo metadata." );
        }
        
        try
        {
            pluginDescriptor.setDependencies( PluginUtils.toComponentDependencies( project.getRuntimeDependencies() ) );
            
            PluginToolsRequest request = new DefaultPluginToolsRequest( project, pluginDescriptor );
            request.setEncoding( encoding );

            mojoScanner.populatePluginDescriptor( request );

            getOutputDirectory().mkdirs();

            createGenerator().execute( getOutputDirectory(), request );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing plugin descriptor", e );
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new MojoExecutionException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                              e );
        }
        catch ( ExtractionException e )
        {
            throw new MojoExecutionException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                              e );
        }
        catch ( LinkageError e )
        {
            throw new MojoExecutionException( "The API of the mojo scanner is not compatible with this plugin version."
                + " Please check the plugin dependencies configured in the POM and ensure the versions match.", e );
        }
    }

}
