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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.registry.MavenPluginRegistryBuilder;
import org.apache.maven.plugin.registry.Plugin;
import org.apache.maven.plugin.registry.PluginRegistry;
import org.apache.maven.plugin.registry.PluginRegistryUtils;
import org.apache.maven.plugin.registry.TrackableBase;
import org.apache.maven.plugin.registry.io.xpp3.PluginRegistryXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Update the user plugin registry (if it's in use) to reflect the version we're installing.
 *
 * @version $Id$
 * @since 2.0
 * @goal updateRegistry
 * @phase install
 */
public class UpdatePluginRegistryMojo
    extends AbstractMojo
{
    /**
     * Indicates whether the <code>plugin-registry.xml</code> file is used by Maven or not
     * to manage plugin versions.
     *
     * @parameter default-value="${settings.usePluginRegistry}"
     * @required
     * @readonly
     */
    private boolean usePluginRegistry;

    /**
     * The group id of the project currently being built.
     *
     * @parameter default-value="${project.groupId}"
     * @required
     * @readonly
     */
    private String groupId;

    /**
     * The artifact id of the project currently being built.
     *
     * @parameter default-value="${project.artifactId}"
     * @required
     * @readonly
     */
    private String artifactId;

    /**
     * The version of the project currently being built.
     *
     * @parameter default-value="${project.artifact.version}"
     * @required
     * @readonly
     */
    private String version;

    /**
     * Plexus component for retrieving the plugin registry info.
     *
     * @component role="org.apache.maven.plugin.registry.MavenPluginRegistryBuilder"
     */
    private MavenPluginRegistryBuilder pluginRegistryBuilder;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @parameter default-value="false" expression="${maven.plugin.skip}"
     * @since 2.8
     */
    private boolean skip;

    /**
     * Set this to "true" to skip updating the plugin registry.
     *
     * @parameter default-value="false" expression="${maven.plugin.update.registry.skip}"
     * @since 2.8
     */
    private boolean skipUpdatePluginRegistry;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( usePluginRegistry )
        {
            if ( skip || skipUpdatePluginRegistry )
            {
                getLog().warn( "Execution skipped" );
                return;
            }
            updatePluginVersionInRegistry( groupId, artifactId, version );
        }
    }

    /**
     * @param aGroupId not null
     * @param anArtifactId not null
     * @param aVersion not null
     * @throws MojoExecutionException if any
     */
    private void updatePluginVersionInRegistry( String aGroupId, String anArtifactId, String aVersion )
        throws MojoExecutionException
    {
        PluginRegistry pluginRegistry;
        try
        {
            pluginRegistry = getPluginRegistry( aGroupId, anArtifactId );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to read plugin registry.", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Failed to parse plugin registry.", e );
        }

        String pluginKey = ArtifactUtils.versionlessKey( aGroupId, anArtifactId );
        Plugin plugin = (Plugin) pluginRegistry.getPluginsByKey().get( pluginKey );

        // if we can find the plugin, but we've gotten here, the useVersion must be missing; fill it in.
        if ( plugin != null )
        {
            if ( TrackableBase.GLOBAL_LEVEL.equals( plugin.getSourceLevel() ) )
            {
                // do nothing. We don't rewrite the globals, under any circumstances.
                getLog().warn( "Cannot update registered version for plugin {" + aGroupId + ":" + anArtifactId
                    + "}; it is specified in the global registry." );
            }
            else
            {
                plugin.setUseVersion( aVersion );

                SimpleDateFormat format =
                    new SimpleDateFormat( org.apache.maven.plugin.registry.Plugin.LAST_CHECKED_DATE_FORMAT );

                plugin.setLastChecked( format.format( new Date() ) );
            }
        }
        else
        {
            plugin = new org.apache.maven.plugin.registry.Plugin();

            plugin.setGroupId( aGroupId );
            plugin.setArtifactId( anArtifactId );
            plugin.setUseVersion( aVersion );

            pluginRegistry.addPlugin( plugin );

            pluginRegistry.flushPluginsByKey();
        }

        writeUserRegistry( aGroupId, anArtifactId, pluginRegistry );
    }

    /**
     * @param aGroupId not null
     * @param anArtifactId not null
     * @param pluginRegistry not null
     */
    private void writeUserRegistry( String aGroupId, String anArtifactId, PluginRegistry pluginRegistry )
    {
        File pluginRegistryFile = pluginRegistry.getRuntimeInfo().getFile();

        PluginRegistry extractedUserRegistry = PluginRegistryUtils.extractUserPluginRegistry( pluginRegistry );

        // only rewrite the user-level registry if one existed before, or if we've created user-level data here.
        if ( extractedUserRegistry != null )
        {
            Writer fWriter = null;

            try
            {
                pluginRegistryFile.getParentFile().mkdirs();
                fWriter = WriterFactory.newXmlWriter( pluginRegistryFile );

                PluginRegistryXpp3Writer writer = new PluginRegistryXpp3Writer();

                writer.write( fWriter, extractedUserRegistry );
            }
            catch ( IOException e )
            {
                getLog().warn( "Cannot rewrite user-level plugin-registry.xml with new plugin version of plugin: \'"
                    + aGroupId + ":" + anArtifactId + "\'.", e );
            }
            finally
            {
                IOUtil.close( fWriter );
            }
        }
    }

    /**
     * @param aGroupId not null
     * @param anArtifactId not null
     * @return the plugin registry instance
     * @throws IOException if any
     * @throws XmlPullParserException if any
     */
    private PluginRegistry getPluginRegistry( String aGroupId, String anArtifactId )
        throws IOException, XmlPullParserException
    {
        PluginRegistry pluginRegistry = null;

        pluginRegistry = pluginRegistryBuilder.buildPluginRegistry();

        if ( pluginRegistry == null )
        {
            pluginRegistry = pluginRegistryBuilder.createUserPluginRegistry();
        }

        return pluginRegistry;
    }
}
