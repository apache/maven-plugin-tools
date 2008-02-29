package org.apache.maven.tools.plugin.javadoc.stubs;

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
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DefaultMavenProjectStub
    extends MavenProjectStub
{
    private Build build;

    public DefaultMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir(), "javadoc-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );

        Build build = new Build();

        build.setFinalName( model.getArtifactId() );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );

        Resource resource = new Resource();
        resource.setDirectory( getBasedir() + "/src/main/resources" );
        build.setResources( Collections.singletonList( resource ) );
        build.setDirectory( super.getBasedir() + "/target/test/unit/javadoc/target" );
        build.setOutputDirectory( super.getBasedir() + "/target/test/unit/javadoc/target/classes" );

        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        resource = new Resource();
        resource.setDirectory( getBasedir() + "/src/test/resources" );
        build.setTestResources( Collections.singletonList( resource ) );
        build.setTestOutputDirectory( super.getBasedir() + "/target/test/unit/javadoc/target/test-classes" );

        setBuild( build );

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /** {@inheritDoc} */
    public Build getBuild()
    {
        return build;
    }

    /** {@inheritDoc} */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/javadoc" );
    }

    /** {@inheritDoc} */
    public List getRemoteArtifactRepositories()
    {
        ArtifactRepository repository = new DefaultArtifactRepository( "central", "http://repo1.maven.org/maven2",
                                                                       new DefaultRepositoryLayout() );

        return Collections.singletonList( repository );
    }

    /** {@inheritDoc} */
    public List getCompileArtifacts()
    {
        Artifact art = new DefaultArtifact( "org.apache.maven", "maven-plugin-api", VersionRange.createFromVersion( "2.0" ),
                                            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler( "jar" ), false );
        art.setFile( new File( super.getBasedir() + "/target/local-repo/org/apache/maven/maven-plugin-api/2.0/maven-plugin-api-2.0.jar" ) );
        return Collections.singletonList( art );
    }
}
