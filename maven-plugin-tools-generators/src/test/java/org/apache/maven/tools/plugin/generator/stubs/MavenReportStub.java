package org.apache.maven.tools.plugin.generator.stubs;

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

import java.util.Locale;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * Dummy report.
 *
 * @goal dummyReport
 */
public class MavenReportStub
    extends AbstractMavenReport
{
    /** {@inheritDoc} */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {

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
    protected Renderer getSiteRenderer()
    {
        return null;
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
}
