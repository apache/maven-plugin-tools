package org.apache.maven.tools.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link PluginToolsRequest}, which is used to pass parameters to components used to extract
 * {@link org.apache.maven.plugin.descriptor.MojoDescriptor MojoDescriptor} instances from different types of metadata
 * for a given plugin.
 *
 * @author jdcasey
 * @since 2.5
 */
public class DefaultPluginToolsRequest
    implements PluginToolsRequest
{

    private static final String DEFAULT_ENCODING = ReaderFactory.FILE_ENCODING;

    private PluginDescriptor pluginDescriptor;

    private MavenProject project;

    private String encoding = DEFAULT_ENCODING;

    private boolean skipErrorNoDescriptorsFound;

    private Set<Artifact> dependencies;

    private List<ArtifactRepository> remoteRepos;

    private ArtifactRepository local;

    private URI internalJavadocBaseUrl;
    
    private String internalJavadocVersion;
    
    private List<URI> externalJavadocBaseUrls;

    private Settings settings;

    public DefaultPluginToolsRequest( MavenProject project, PluginDescriptor pluginDescriptor )
    {
        this.project = project;
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginToolsRequest setPluginDescriptor( PluginDescriptor pluginDescriptor )
    {
        this.pluginDescriptor = pluginDescriptor;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginToolsRequest setProject( MavenProject project )
    {
        this.project = project;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginToolsRequest setEncoding( String encoding )
    {
        if ( StringUtils.isNotEmpty( encoding ) )
        {
            this.encoding = encoding;
        }
        else
        {
            this.encoding = DEFAULT_ENCODING;
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkipErrorNoDescriptorsFound()
    {
        return skipErrorNoDescriptorsFound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginToolsRequest setSkipErrorNoDescriptorsFound( boolean skipErrorNoDescriptorsFound )
    {
        this.skipErrorNoDescriptorsFound = skipErrorNoDescriptorsFound;
        return this;
    }

    @Override
    public Set<Artifact> getDependencies()
    {
        if ( this.dependencies == null )
        {
            this.dependencies = new HashSet<>();
        }
        return dependencies;
    }

    @Override
    public PluginToolsRequest setDependencies( Set<Artifact> dependencies )
    {
        this.dependencies = dependencies;
        return this;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepos()
    {
        return remoteRepos;
    }

    @Override
    public PluginToolsRequest setRemoteRepos( List<ArtifactRepository> remoteRepos )
    {
        this.remoteRepos = remoteRepos;
        return this;
    }

    @Override
    public ArtifactRepository getLocal()
    {
        return local;
    }

    @Override
    public PluginToolsRequest setLocal( ArtifactRepository local )
    {
        this.local = local;
        return this;
    }

    @Override
    public PluginToolsRequest setInternalJavadocBaseUrl( URI baseUrl )
    {
        internalJavadocBaseUrl = baseUrl;
        return this;
    }

    @Override
    public URI getInternalJavadocBaseUrl()
    {
        return internalJavadocBaseUrl;
    }

    @Override
    public PluginToolsRequest setInternalJavadocVersion( String javadocVersion )
    {
        this.internalJavadocVersion = javadocVersion;
        return this;
    }

    @Override
    public String getInternalJavadocVersion()
    {
        return internalJavadocVersion;
    }

    @Override
    public PluginToolsRequest setExternalJavadocBaseUrls( List<URI> javadocLinks )
    {
        this.externalJavadocBaseUrls = javadocLinks;
        return this;
    }

    @Override
    public List<URI> getExternalJavadocBaseUrls()
    {
        return externalJavadocBaseUrls;
    }

    @Override
    public PluginToolsRequest setSettings( Settings settings )
    {
        this.settings = settings;
        return this;
    }

    @Override
    public Settings getSettings()
    {
        return settings;
    }
}
