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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * Touches a test file.
 * 
 * @goal it0013
 */
public class CoreIt0013Mojo
    extends AbstractMojo
{
    
    /**
     * @parameter property="project.build.directory"
     * @required
     */
    private String outputDirectory;

    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "outputDirectory = " + outputDirectory );

        File f = new File( outputDirectory );
        
        if ( !f.exists() )
        {
            f.mkdirs();
        }
        
        File touch = new File( f, "touch.txt" );
        
        try
        {
            touch.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing verification file.", e );
        }                
    }

}
