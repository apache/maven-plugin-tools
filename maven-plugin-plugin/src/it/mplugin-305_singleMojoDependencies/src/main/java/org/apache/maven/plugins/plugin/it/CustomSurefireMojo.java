package org.apache.maven.plugins.plugin.it;

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
import java.util.List;

import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.suite.RunResult;

@Mojo(name="custom-surefire")
public class CustomSurefireMojo extends AbstractSurefireMojo {

    public File getBasedir()
    {
        return null;
    }

    public File getClassesDirectory()
    {
        return null;
    }

    public String getDebugForkedProcess()
    {
        return null;
    }

    public Boolean getFailIfNoSpecifiedTests()
    {
        return null;
    }

    public int getForkedProcessTimeoutInSeconds()
    {
        return 0;
    }

    public double getParallelTestsTimeoutForcedInSeconds()
    {
        return 0;
    }

    public double getParallelTestsTimeoutInSeconds()
    {
        return 0;
    }

    public String getReportFormat()
    {
        return null;
    }

    public File getReportsDirectory()
    {
        return null;
    }

    public String getShutdown()
    {
        return null;
    }

    public int getSkipAfterFailureCount()
    {
        return 0;
    }

    public String getTest()
    {
        return null;
    }

    public File getTestClassesDirectory()
    {
        return null;
    }

    public boolean isPrintSummary()
    {
        return false;
    }

    public boolean isSkip()
    {
        return false;
    }

    public boolean isSkipExec()
    {
        return false;
    }

    public boolean isSkipTests()
    {
        return false;
    }

    public boolean isUseFile()
    {
        return false;
    }

    public boolean isUseManifestOnlyJar()
    {
        return false;
    }

    public boolean isUseSystemClassLoader()
    {
        return false;
    }

    public void setBasedir( File arg0 )
    {
    }

    public void setClassesDirectory( File arg0 )
    {
    }

    public void setDebugForkedProcess( String arg0 )
    {
    }

    public void setFailIfNoSpecifiedTests( boolean arg0 )
    {
    }

    public void setForkedProcessTimeoutInSeconds( int arg0 )
    {
    }

    public void setParallelTestsTimeoutForcedInSeconds( double arg0 )
    {
    }

    public void setParallelTestsTimeoutInSeconds( double arg0 )
    {
    }

    public void setPrintSummary( boolean arg0 )
    {
    }

    public void setReportFormat( String arg0 )
    {
    }

    public void setReportsDirectory( File arg0 )
    {
    }

    public void setSkip( boolean arg0 )
    {
    }

    public void setSkipExec( boolean arg0 )
    {
    }

    public void setSkipTests( boolean arg0 )
    {
    }

    public void setTest( String arg0 )
    {
    }

    public void setTestClassesDirectory( File arg0 )
    {
    }

    public void setUseFile( boolean arg0 )
    {
    }

    public void setUseManifestOnlyJar( boolean arg0 )
    {
    }

    public void setUseSystemClassLoader( boolean arg0 )
    {
    }

    @Override
    protected String[] getDefaultIncludes()
    {
        return null;
    }

    @Override
    public File getExcludesFile()
    {
        return null;
    }

    @Override
    public List<String> getIncludes()
    {
        return null;
    }

    @Override
    public File getIncludesFile()
    {
        return null;
    }

    @Override
    protected String getPluginName()
    {
        return null;
    }

    @Override
    protected int getRerunFailingTestsCount()
    {
        return 0;
    }

    @Override
    public String getRunOrder()
    {
        return null;
    }

    @Override
    public File[] getSuiteXmlFiles()
    {
        return null;
    }

    @Override
    protected void handleSummary( RunResult arg0, Exception arg1 )
    {
    }

    @Override
    protected boolean hasSuiteXmlFiles()
    {
        return false;
    }

    @Override
    protected boolean isSkipExecution()
    {
        return false;
    }

    @Override
    public void setIncludes( List<String> arg0 )
    {
    }

    @Override
    public void setRunOrder( String arg0 )
    {
    }

    @Override
    public void setSuiteXmlFiles( File[] arg0 )
    {
    }

    @Override
    protected List<File> suiteXmlFiles()
    {
        return null;
    }
}