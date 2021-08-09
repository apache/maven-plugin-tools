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

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginDescriptorGenerator;

/**
 * <p>
 * Generate a plugin descriptor.
 * </p>
 * <p>
 * <b>Note:</b> Since 3.0, for Java plugin annotations support,
 * default <a href="http://maven.apache.org/ref/current/maven-core/lifecycles.html">phase</a>
 * defined by this goal is after the "compilation" of any scripts. This doesn't override
 * <a href="/ref/current/maven-core/default-bindings.html#Bindings_for_maven-plugin_packaging">the default binding coded
 * at generate-resources phase</a> in Maven core.
 * </p>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @since 2.0
 */
@Mojo( name = "descriptor", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
       requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true )
public class DescriptorGeneratorMojo
    extends AbstractGeneratorMojo
{
    /**
     * The directory where the generated <code>plugin.xml</code> file will be put.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}/META-INF/maven", readonly = true )
    protected File outputDirectory;

    /**
     * A flag to disable generation of the <code>plugin.xml</code> in favor of a hand authored plugin descriptor.
     *
     * @since 2.6
     */
    @Parameter( defaultValue = "false" )
    private boolean skipDescriptor;

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Generator createGenerator()
    {
        return new PluginDescriptorGenerator( getLog() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( skipDescriptor )
        {
            return;
        }

        super.execute();
    }

}
