package org.apache.maven.tools.plugin.javadoc;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.javadoc.JavadocReport;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test the taglets by running Maven Javadoc Plugin.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class JavadocReportTest
    extends AbstractMojoTestCase
{
    private static final String LINE_SEPARATOR = "";

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        createTestRepo();
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Create test repository in target directory.
     */
    private void createTestRepo()
    {
        File f = new File( getBasedir(), "target/local-repo/" );
        f.mkdirs();
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>""</code> as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile( File file )
        throws IOException
    {
        String str = "", strTmp = "";
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        while ( ( strTmp = in.readLine() ) != null )
        {
            str = str + LINE_SEPARATOR + strTmp;
        }
        in.close();

        return str;
    }

    /**
     * Test the default javadoc renderer using the Maven plugin
     * <code>org.apache.maven.plugins:maven-javadoc-plugin:2.3</code>
     *
     * @throws Exception
     */
    public void testMojoTaglets()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/javadoc/javadoc-plugin-config.xml" );
        PlexusConfiguration pluginConfiguration = extractPluginConfiguration( "maven-javadoc-plugin", testPom );

        JavadocReport mojo = (JavadocReport) lookupMojo( "org.apache.maven.plugins", "maven-javadoc-plugin", "2.3",
                                                         "javadoc", pluginConfiguration );

        // Don't know why we need to specify that
        ArtifactRepository remoteRepositories = new DefaultArtifactRepository( "central",
                                                                               "http://repo1.maven.org/maven2",
                                                                               new DefaultRepositoryLayout() );
        setVariableValueToObject( mojo, "remoteRepositories", Collections.singletonList( remoteRepositories ) );

        ArtifactRepository localRepository = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );
        ArtifactResolver resolver = (ArtifactResolver) getVariableValueFromObject( mojo, "resolver" );
        ArtifactFactory factory = (ArtifactFactory) getVariableValueFromObject( mojo, "factory" );
        Artifact artifact = factory.createArtifact( "org.apache.maven", "maven-plugin-api", "2.0", "compile", "jar" );
        resolver.resolve( artifact, Collections.singletonList( remoteRepositories ), localRepository );

        mojo.execute();

        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/javadoc/target/site/apidocs/org/apache/maven/plugin/my/MyMojo.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );

        // Verify mojo type
        String mojoType = "<dl><dt><b>" + MojoAggregatorTypeTaglet.HEADER + "</b></dt><dd></dd><dt><b>"
            + MojoConfiguratorTypeTaglet.HEADER + ":</b></dt><dd>roleHint</dd><dt><b>" + MojoExecuteTypeTaglet.HEADER
            + ":</b></dt><dd><dl><dt><b>phase:</b></dt><dd>validate</dd>"
            + "<dt><b>lifecycle:</b></dt><dd>default</dd></dl></dd><dt><b>" + MojoExecutionStrategyTypeTaglet.HEADER
            + ":</b></dt><dd>always</dd>" + "<dt><b>" + MojoGoalTypeTaglet.HEADER + ":</b></dt><dd>touch</dd>"
            + "<dt><b>" + MojoInheritByDefaultTypeTaglet.HEADER + ":</b></dt><dd>true</dd><dt><b>"
            + MojoInstantiationStrategyTypeTaglet.HEADER + ":</b></dt><dd>per-lookup</dd><dt><b>"
            + MojoPhaseTypeTaglet.HEADER + ":</b></dt><dd>phaseName</dd><dt><b>"
            + MojoRequiresDependencyResolutionTypeTaglet.HEADER + ":</b></dt><dd>compile</dd><dt><b>"
            + MojoRequiresDirectInvocationTypeTaglet.HEADER + ":</b></dt><dd>false</dd><dt><b>"
            + MojoRequiresOnLineTypeTaglet.HEADER + ":</b></dt><dd>true</dd><dt><b>"
            + MojoRequiresProjectTypeTaglet.HEADER + ":</b></dt><dd>true</dd><dt><b>"
            + MojoRequiresReportsTypeTaglet.HEADER + ":</b></dt><dd>false</dd></dl>";
        assertTrue( str.toLowerCase().indexOf( ( mojoType ).toLowerCase() ) != -1 );

        // Verify mojo fields
        String mojoField = "<dl><dt><b>" + MojoParameterFieldTaglet.HEADER
            + ":</b></dt><dd><dl><dt><b>default-value:</b></dt>"
            + "<dd>value</dd><dt><b>expression:</b></dt><dd>${project.build.directory}</dd><dt><b>alias:</b>"
            + "</dt><dd>myAlias</dd></dl></dd><dt><b>" + MojoReadOnlyFieldTaglet.HEADER + "</b></dt><dd></dd><dt><b>"
            + MojoRequiredFieldTaglet.HEADER + "</b></dt><dd>" + "</dd></dl>";
        assertTrue( str.toLowerCase().indexOf( ( mojoField ).toLowerCase() ) != -1 );

        mojoField = "<dl><dt><b>" + MojoComponentFieldTaglet.HEADER + ":</b></dt><dd><dl><dt><b>role:</b>"
            + "</dt><dd>org.apacha.maven.MyComponent</dd><dt><b>roleHint:</b></dt><dd>default</dd></dl></dd>"
            + "<dt><b>" + MojoReadOnlyFieldTaglet.HEADER + "</b></dt><dd></dd><dt><b>" + MojoRequiredFieldTaglet.HEADER
            + "</b></dt><dd>" + "</dd></dl>";
        assertTrue( str.toLowerCase().indexOf( ( mojoField ).toLowerCase() ) != -1 );
    }
}
