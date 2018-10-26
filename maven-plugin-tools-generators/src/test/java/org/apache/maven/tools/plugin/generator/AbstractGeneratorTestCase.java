package org.apache.maven.tools.plugin.generator;

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
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 */
public abstract class AbstractGeneratorTestCase
    extends PlexusTestCase
{
    protected Generator generator;

    protected String basedir;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        basedir = System.getProperty( "basedir" );
    }

    public void testGenerator()
        throws Exception
    {
        setupGenerator();

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "testGoal" );
        mojoDescriptor.setImplementation( "org.apache.maven.tools.plugin.generator.TestMojo" );
        mojoDescriptor.setDependencyResolutionRequired( "compile" );

        List<Parameter> params = new ArrayList<Parameter>();

        Parameter param = new Parameter();
        param.setExpression( "${project.build.directory}" );
        param.setDefaultValue( "</markup-must-be-escaped>" );
        param.setName( "dir" );
        param.setRequired( true );
        param.setType( "java.lang.String" );
        param.setDescription( "Test parameter description" );
        param.setAlias( "some.alias" );

        params.add( param );

        mojoDescriptor.setParameters( params );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        pluginDescriptor.addMojo( mojoDescriptor );

        pluginDescriptor.setArtifactId( "maven-unitTesting-plugin" );
        pluginDescriptor.setGoalPrefix( "test" );

        ComponentDependency dependency = new ComponentDependency();
        dependency.setGroupId( "testGroup" );
        dependency.setArtifactId( "testArtifact" );
        dependency.setVersion( "0.0.0" );

        pluginDescriptor.setDependencies( Collections.singletonList( dependency ) );

        File destinationDirectory = Files.createTempDirectory( "testGenerator-outDir" ).toFile();
        destinationDirectory.mkdir();

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId( "foo" );
        mavenProject.setArtifactId( "bar" );
        mavenProject.setBuild( new Build()
        {
            @Override
            public String getDirectory()
            {
                return basedir + "/target";
            }

            @Override
            public String getOutputDirectory()
            {
                return basedir + "/target";
            }
        } );

        generator.execute( destinationDirectory, new DefaultPluginToolsRequest( mavenProject, pluginDescriptor ) );

        validate( destinationDirectory );

        FileUtils.deleteDirectory( destinationDirectory );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void setupGenerator()
        throws Exception
    {
        String generatorClassName = getClass().getName();

        generatorClassName = generatorClassName.substring( 0, generatorClassName.length() - 4 );

        try
        {
            Class<?> generatorClass = Thread.currentThread().getContextClassLoader().loadClass( generatorClassName );

            Log log = new SystemStreamLog();
            try
            {
                Constructor<?> constructor = generatorClass.getConstructor( Log.class );
                generator = (Generator) constructor.newInstance( log );
            }
            catch ( NoSuchMethodException ignore )
            {
                generator = (Generator) generatorClass.newInstance();

            }
        }
        catch ( Exception e )
        {
            throw new Exception( "Cannot find " + generatorClassName +
                                     "! Make sure your test case is named in the form ${generatorClassName}Test " +
                                     "or override the setupPlugin() method to instantiate the mojo yourself." );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void validate( File destinationDirectory )
        throws Exception
    {
        // empty
    }
}
