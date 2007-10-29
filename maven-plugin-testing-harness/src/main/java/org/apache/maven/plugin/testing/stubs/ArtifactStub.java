package org.apache.maven.plugin.testing.stubs;

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
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Stub class for {@link Artifact} testing.
 *
 * @author jesse
 * @version $Id$
 */
public class ArtifactStub
    implements Artifact
{
    private String groupId;

    private String artifactId;

    private String version;

    private String scope;

    private String type;

    private String classifier;

    private File file;

    private ArtifactRepository artifactRepository;

    /**
     * By default, return <code>0</code>
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object object )
    {
        return 0;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getGroupId()
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getArtifactId()
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getVersion()
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#setVersion(java.lang.String)
     */
    public void setVersion( String version )
    {
        this.version = version;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getScope()
     */
    public String getScope()
    {
        return scope;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getType()
     */
    public String getType()
    {
        return type;
    }

    /**
     * Set a new type
     *
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getClassifier()
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#hasClassifier()
     */
    public boolean hasClassifier()
    {
        return classifier != null;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#getFile()
     */
    public File getFile()
    {
        return file;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#setFile(java.io.File)
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getBaseVersion()
     */
    public String getBaseVersion()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setBaseVersion(java.lang.String)
     */
    public void setBaseVersion( String string )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getId()
     */
    public String getId()
    {
        return null;
    }

    /**
     * By default, return <code>groupId:artifactId:type:classifier</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getDependencyConflictId()
     */
    public String getDependencyConflictId()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append( getGroupId() );
        buffer.append( ":" ).append( getArtifactId() );
        buffer.append( ":" ).append( getType() );
        buffer.append( ":" ).append( getClassifier() );
        
        return buffer.toString();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#addMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata)
     */
    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getMetadataList()
     */
    public Collection getMetadataList()
    {
        return null;
    }

    /**
     * Set a new artifact repository
     *
     * @see org.apache.maven.artifact.Artifact#setRepository(org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void setRepository( ArtifactRepository artifactRepository )
    {
        this.artifactRepository = artifactRepository;
    }

    /**
     * Returns repository for artifact 
     *
     * @see org.apache.maven.artifact.Artifact#getRepository()
     */
    public ArtifactRepository getRepository()
    {
        return artifactRepository;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#updateVersion(java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void updateVersion( String string, ArtifactRepository artifactRepository )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getDownloadUrl()
     */
    public String getDownloadUrl()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDownloadUrl(java.lang.String)
     */
    public void setDownloadUrl( String string )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getDependencyFilter()
     */
    public ArtifactFilter getDependencyFilter()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyFilter(org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getArtifactHandler()
     */
    public ArtifactHandler getArtifactHandler()
    {
        return null;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getDependencyTrail()
     */
    public List getDependencyTrail()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyTrail(java.util.List)
     */
    public void setDependencyTrail( List list )
    {
        // nop
    }

    /**
     * @see org.apache.maven.artifact.Artifact#setScope(java.lang.String)
     */
    public void setScope( String scope )
    {
        this.scope = scope;
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getVersionRange()
     */
    public VersionRange getVersionRange()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setVersionRange(org.apache.maven.artifact.versioning.VersionRange)
     */
    public void setVersionRange( VersionRange versionRange )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#selectVersion(java.lang.String)
     */
    public void selectVersion( String string )
    {
        // nop
    }

    /**
     * @see org.apache.maven.artifact.Artifact#setGroupId(java.lang.String)
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * @see org.apache.maven.artifact.Artifact#setArtifactId(java.lang.String)
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * By default, return <code>false</code>.
     *
     * @see org.apache.maven.artifact.Artifact#isSnapshot()
     */
    public boolean isSnapshot()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setResolved(boolean)
     */
    public void setResolved( boolean b )
    {
        // nop
    }

    /**
     * By default, return <code>false</code>.
     *
     * @see org.apache.maven.artifact.Artifact#isResolved()
     */
    public boolean isResolved()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setResolvedVersion(java.lang.String)
     */
    public void setResolvedVersion( String string )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setArtifactHandler(org.apache.maven.artifact.handler.ArtifactHandler)
     */
    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        // nop
    }

    /**
     * By default, return <code>false</code>.
     *
     * @see org.apache.maven.artifact.Artifact#isRelease()
     */
    public boolean isRelease()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setRelease(boolean)
     */
    public void setRelease( boolean b )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getAvailableVersions()
     */
    public List getAvailableVersions()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setAvailableVersions(java.util.List)
     */
    public void setAvailableVersions( List list )
    {
        // nop
    }

    /**
     * By default, return <code>false</code>.
     *
     * @see org.apache.maven.artifact.Artifact#isOptional()
     */
    public boolean isOptional()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @param b
     */
    public void setOptional( boolean b )
    {
        // nop
    }

    /**
     * By default, return <code>null</code>.
     *
     * @see org.apache.maven.artifact.Artifact#getSelectedVersion()
     */
    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return null;
    }

    /**
     * By default, return <code>false</code>.
     *
     * @see org.apache.maven.artifact.Artifact#isSelectedVersionKnown()
     */
    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return false;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( getGroupId() != null )
        {
            sb.append( getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        if ( version != null )
        {
            sb.append( ":" );
            sb.append( getVersion() );
        }
        if ( scope != null )
        {
            sb.append( ":" );
            sb.append( scope );
        }
        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( getArtifactId() );
        sb.append( ":" );
        sb.append( getType() );
        if ( hasClassifier() )
        {
            sb.append( ":" );
            sb.append( getClassifier() );
        }
    }

}
