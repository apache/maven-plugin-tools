package org.apache.maven.tools.plugin.scanner;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author jdcasey
 */
public class DefaultMojoScannerTest
{
    private Map<String, MojoDescriptorExtractor> extractors;

    private Build build;

    private Model model;

    private MojoScanner scanner;

    private MavenProject project;

    @Before
    public void setUp()
    {
        extractors = new HashMap<>();
        extractors.put( "one", new ScannerTestExtractor( "one" ) );
        extractors.put( "two", new ScannerTestExtractor( "two" ) );
        extractors.put( "three", new ScannerTestExtractor( "three" ) );

        scanner = new DefaultMojoScanner( extractors );

        build = new Build();
        build.setSourceDirectory( "testdir" );

        model = new Model();
        model.setBuild( build );

        project = new MavenProject( model );
        project.setFile( new File( "." ) );
    }

    @Test
    public void testUnspecifiedExtractors()
        throws Exception
    {
        PluginDescriptor pluginDescriptor = createPluginDescriptor();

        scanner.populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );

        checkResult( pluginDescriptor, extractors.keySet() );
    }

    @Test
    public void testSpecifiedExtractors()
        throws Exception
    {
        Set<String> activeExtractors = new HashSet<>();
        activeExtractors.add( "one" );
        activeExtractors.add( "" );
        activeExtractors.add( null );
        activeExtractors.add( "three" );

        PluginDescriptor pluginDescriptor = createPluginDescriptor();

        scanner.setActiveExtractors( activeExtractors );
        scanner.populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );

        checkResult( pluginDescriptor, Arrays.asList( "one", "three" ) );
    }

    @Test
    public void testAllExtractorsThroughNull()
        throws Exception
    {
        PluginDescriptor pluginDescriptor = createPluginDescriptor();

        scanner.setActiveExtractors( null );
        scanner.populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );

        checkResult( pluginDescriptor, extractors.keySet() );
    }

    @Test
    public void testNoExtractorsThroughEmptySet()
        throws Exception
    {
        PluginDescriptor pluginDescriptor = createPluginDescriptor();

        scanner.setActiveExtractors( Collections.<String>emptySet() );
        try
        {
            scanner.populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
            fail( "Expected exception" );
        }
        catch (InvalidPluginDescriptorException e)
        {
            // Ok
        }

        checkResult( pluginDescriptor, Collections.<String>emptySet() );
    }

    @Test
    public void testUnknownExtractor()
        throws Exception
    {
        Set<String> activeExtractors = new HashSet<>();
        activeExtractors.add( "four" );

        PluginDescriptor pluginDescriptor = createPluginDescriptor();

        scanner.setActiveExtractors( activeExtractors );

        try
        {
            scanner.populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
            fail( "No error for unknown extractor" );
        }
        catch ( ExtractionException e )
        {
            // Ok
        }

        checkResult( pluginDescriptor, Collections.<String>emptySet() );
    }

    private PluginDescriptor createPluginDescriptor()
    {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId( "groupId" );
        pluginDescriptor.setArtifactId( "artifactId" );
        pluginDescriptor.setVersion( "version" );
        pluginDescriptor.setGoalPrefix( "testId" );
        return pluginDescriptor;
    }

    /**
     * Checks if the {@link PluginDescriptor} contains exactly the {@link MojoDescriptor}s with the
     * supplied goal names.
     *
     * @param pluginDescriptor The {@link PluginDescriptor} to check.
     * @param expectedGoals    The goal names of the {@link MojoDescriptor}s.
     */
    protected void checkResult( PluginDescriptor pluginDescriptor, Collection<String> expectedGoals )
    {
        Set<String> remainingGoals = new HashSet<>( expectedGoals );
        @SuppressWarnings( "unchecked" )
        List<MojoDescriptor> descriptors = pluginDescriptor.getMojos();

        if ( descriptors == null )
        {
            // TODO Maybe getMojos should be more user friendly and not return null
            descriptors = Collections.emptyList();
        }

        for ( MojoDescriptor desc : descriptors )
        {
            assertEquals( pluginDescriptor, desc.getPluginDescriptor() );
            assertTrue( "Unexpected goal in PluginDescriptor: " + desc.getGoal(),
                    remainingGoals.remove( desc.getGoal() ) );
        }

        assertEquals( "Expected goals missing from PluginDescriptor: " + remainingGoals, 0, remainingGoals.size() );
    }

}