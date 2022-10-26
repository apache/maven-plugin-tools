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

import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

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
     * The goal prefix that will appear before the ":".
     */
    @Parameter
    protected String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.skip" )
    private boolean skip;

    /**
     * Maven plugin packaging types. Default is single "maven-plugin".
     * 
     * @since 3.3
     */
    @Parameter
    private List<String> packagingTypes = Collections.singletonList( "maven-plugin" );

    /**
     * System/OS line separator: used to format console messages.
     */
    protected static final String LS = System.lineSeparator();

    protected abstract void generate() throws MojoExecutionException;

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

        generate();
    }

    static String getDefaultGoalPrefix( MavenProject project )
    {
        String defaultGoalPrefix;
        if ( "maven-plugin-report-plugin".equalsIgnoreCase( project.getArtifactId() ) )
        {
            defaultGoalPrefix = "plugin-report";
        }
        else if ( "maven-plugin".equalsIgnoreCase( project.getArtifactId() ) )
        {
            defaultGoalPrefix = project.getGroupId().substring( project.getGroupId().lastIndexOf( '.' ) + 1 );
        }
        else
        {
            defaultGoalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        }
        return defaultGoalPrefix;
    }
}
