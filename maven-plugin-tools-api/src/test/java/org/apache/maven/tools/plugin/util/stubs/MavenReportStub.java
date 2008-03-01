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

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

/**
 * Dummy report.
 *
 * @goal dummyReport
 * @version $Id$
 */
public class MavenReportStub
    extends AbstractMavenReport
{
    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        return super.canGenerateReport();
    }

    /** {@inheritDoc} */
    protected void closeReport()
    {
        super.closeReport();
    }

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        super.execute();
    }

    /** {@inheritDoc} */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {

    }

    /** {@inheritDoc} */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        super.generate( sink, locale );
    }

    /** {@inheritDoc} */
    public String getCategoryName()
    {
        return super.getCategoryName();
    }

    /** {@inheritDoc} */
    protected String getOutputDirectory()
    {
        return null;
    }

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return null;
    }

    /** {@inheritDoc} */
    public File getReportOutputDirectory()
    {
        return super.getReportOutputDirectory();
    }

    /** {@inheritDoc} */
    public org.apache.maven.doxia.sink.Sink getSink()
    {
        return super.getSink();
    }

    /** {@inheritDoc} */
    protected Renderer getSiteRenderer()
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isExternalReport()
    {
        return super.isExternalReport();
    }

    /** {@inheritDoc} */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        super.setReportOutputDirectory( reportOutputDirectory );
    }

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
    public String getDescription( Locale locale )
    {
        return null;
    }

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return null;
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return null;
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
}
