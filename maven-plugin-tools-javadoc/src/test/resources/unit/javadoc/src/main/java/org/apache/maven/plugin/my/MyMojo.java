package org.apache.maven.plugin.my;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;

/**
 * Goal which touches a timestamp file.
 *
 * @aggregator
 * @configurator roleHint
 * @execute phase="validate" lifecycle="default"
 * @executionStrategy always
 * @goal touch
 * @inheritByDefault true
 * @instantiationStrategy per-lookup
 * @phase phaseName
 * @requiresDependencyResolution compile
 * @requiresDirectInvocation
 * @requiresOnline
 * @requiresProject
 * @requiresReports
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Dummy parameter.
     *
     * @parameter expression="${project.build.directory}" default-value="value" alias="myAlias"
     * @required
     * @readonly
     */
    private String dummy;

    /**
     * Dummy component.
     *
     * @component role="org.apacha.maven.MyComponent" roleHint="default"
     * @required
     * @readonly
     */
    private String component;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        File f = outputDirectory;

        if ( !f.exists() )
        {
            f.mkdirs();
        }

        File touch = new File( f, "touch.txt" );

        Writer w = null;
        try
        {
            w = new OutputStreamWriter( new FileOutputStream( touch ), "UTF-8" );

            w.write( "touch.txt" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file " + touch, e );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }
}
