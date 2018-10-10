package org.apache.maven.tools.plugin.extractor.annotations;

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

import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.manager.CompilerManager;

/**
 * @author Olivier Lamy
 */
@Mojo( name = "foo", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true )
@Execute( goal = "compiler", lifecycle = "my-lifecycle", phase = LifecyclePhase.PACKAGE )
public class FooMojo
    extends AbstractFooMojo
{
    /**
     * the cool bar to go
     * @since 1.0
     */
    @Parameter( property = "thebar", required = true, defaultValue = "coolbar" )
    protected String bar;

    /**
     * beer for non french folks
     * @deprecated wine is better
     */
    @Parameter( property = "thebeer", defaultValue = "coolbeer" )
    protected String beer;

    /**
     * Plexus compiler manager.
     */
    @Component
    protected CompilerManager compilerManager;

    /**
     *
     */
    @Component( role = ArtifactMetadataSource.class, hint = "maven" )
    protected ArtifactMetadataSource artifactMetadataSource;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // nothing
    }
}
