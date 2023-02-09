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
package org.apache.maven.plugin.coreit;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Touches a test file.
 *
 * @goal first
 * @requiresDependencyResolution test
 * @phase integration-test
 * @execute phase="generate-sources" lifecycle="cobertura"
 * @deprecated Don't use!
 * @since 1.2
 */
public class FirstMojo extends AbstractMojo {

    /**
     * Project directory.
     * @parameter default-value="${basedir}"
     * @readonly
     */
    private File basedir;

    /**
     * @parameter expression="first.touchFile" default-value="${project.build.directory}/touch.txt"
     * @required
     */
    private File touchFile;

    /**
     * @parameter name="namedParam" alias="alias"
     * @deprecated As of 0.2
     * @since 0.1
     */
    private String aliasedParam;

    /**
     * @component role="org.apache.maven.project.MavenProjectHelper" roleHint="test"
     */
    private Object projectHelper;

    /**
     * @component
     */
    private MavenSession session;

    /**
     * @component
     */
    private MavenProject project;

    /**
     * @component
     */
    private MojoExecution mojo;

    /**
     * @component
     */
    private PluginDescriptor plugin;

    /**
     * @component
     */
    private Settings settings;

    public void execute() throws MojoExecutionException {}
}
