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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;

import java.io.File;

/**
 * Touches a test file.
 *
 * @since 1.2
 * @deprecated Don't use!
 */
@Mojo( name = "first", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.INTEGRATION_TEST )
@Execute( phase = LifecyclePhase.GENERATE_SOURCES, lifecycle = "cobertura" )
public class FirstMojo
    extends AbstractMojo
{

    /**
     * Project directory.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true )
    private File basedir;

    @Parameter( property = "first.touchFile", defaultValue = "${project.build.directory}/touch.txt",
                required = true )
    private File touchFile;

    /**
     * @since 0.1
     * @deprecated As of 0.2
     */
    @Parameter( name = "namedParam", alias = "alias" )
    private String aliasedParam;

    @Component( role = MavenProjectHelper.class, hint = "test" )
    private Object projectHelper;

    @Component
    private MavenSession session;

    @Component
    private MavenProject project;

    @Component
    private MojoExecution mojo;

    @Component
    private PluginDescriptor plugin;

    @Component
    private Settings settings;

    public void execute()
        throws MojoExecutionException
    {
    }

}
