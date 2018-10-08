package org.apache.maven.tools.plugin.util.stubs;

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

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Dummy Mojo.
 *
 * @goal dummy
 */
public class MojoStub
    extends AbstractMojo
{
    /** {@inheritDoc} */
    public Log getLog()
    {
        return super.getLog();
    }

    /** {@inheritDoc} */
    public Map getPluginContext()
    {
        return super.getPluginContext();
    }

    /** {@inheritDoc} */
    public void setLog( Log log )
    {
        super.setLog( log );
    }

    /** {@inheritDoc} */
    public void setPluginContext( Map pluginContext )
    {
        super.setPluginContext( pluginContext );
    }

    /** {@inheritDoc} */
    protected Object clone()
        throws CloneNotSupportedException
    {
        return super.clone();
    }

    /** {@inheritDoc} */
    public boolean equals( Object obj )
    {
        return super.equals( obj );
    }

    /** {@inheritDoc} */
    protected void finalize()
        throws Throwable
    {
        super.finalize();
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
        return super.hashCode();
    }

    /** {@inheritDoc} */
    public String toString()
    {
        return super.toString();
    }

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

    }
}
