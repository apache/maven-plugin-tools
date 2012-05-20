package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.AbstractFirstMojo;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Touches a test file.
 *
 * @since 1.2
 * @deprecated Don't use!
 */
@Mojo( name = "first", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.INTEGRATION_TEST )
@Execute( phase = LifecyclePhase.GENERATE_SOURCES, lifecycle = "cobertura" )
public class FirstMojo
    extends AbstractFirstMojo
{

    /**
     * @since 0.1
     * @deprecated As of 0.2
     */
    @Parameter( alias = "alias" )
    private String aliasedParam;

    @Component( role = MavenProjectHelper.class )
    private Object projectHelper;

    public void execute()
        throws MojoExecutionException
    {
        if ( basedir == null )
        {
            throw new MojoExecutionException( "basedir == null" );
        }
        if ( touchFile == null )
        {
            throw new MojoExecutionException( "touchFile == null" );
        }
        if ( projectHelper == null )
        {
            throw new MojoExecutionException( "projectHelper == null" );
        }
        if ( compilerManager == null )
        {
            throw new MojoExecutionException( "compilerManager == null" );
        }
        if (! ( projectHelper instanceof MavenProjectHelper ))
        {
            throw new MojoExecutionException( "! projectHelper instanceof MavenProjectHelper" );
        }

    }

}
