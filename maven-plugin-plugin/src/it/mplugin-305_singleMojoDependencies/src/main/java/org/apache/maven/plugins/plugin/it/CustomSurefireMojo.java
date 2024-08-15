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
package org.apache.maven.api.plugins.plugin.it;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ForkNodeFactory;

@Mojo(name = "custom-surefire")
public class CustomSurefireMojo extends AbstractSurefireMojo {

    public File getBasedir() {
        return null;
    }

    public File getClassesDirectory() {
        return null;
    }

    public String getDebugForkedProcess() {
        return null;
    }

    public String getEnableProcessChecker() {
        return null;
    }

    public String getEncoding() {
        return null;
    }

    public boolean getFailIfNoSpecifiedTests() {
        return false;
    }

    public ForkNodeFactory getForkNode() {
        return null;
    }

    public int getForkedProcessTimeoutInSeconds() {
        return 0;
    }

    public File getMainBuildPath() {
        return null;
    }

    public double getParallelTestsTimeoutForcedInSeconds() {
        return 0;
    }

    public double getParallelTestsTimeoutInSeconds() {
        return 0;
    }

    public String getReportFormat() {
        return null;
    }

    public String getReportSchemaLocation() {
        return null;
    }

    public File getReportsDirectory() {
        return null;
    }

    public String getShutdown() {
        return null;
    }

    public int getSkipAfterFailureCount() {
        return 0;
    }

    public String getTest() {
        return null;
    }

    public File getTestClassesDirectory() {
        return null;
    }

    public boolean isPrintSummary() {
        return false;
    }

    public boolean isSkip() {
        return false;
    }

    public boolean isSkipExec() {
        return false;
    }

    public boolean isSkipTests() {
        return false;
    }

    public boolean isUseFile() {
        return false;
    }

    public boolean isUseManifestOnlyJar() {
        return false;
    }

    public boolean isUseSystemClassLoader() {
        return false;
    }

    public boolean useModulePath() {
        return false;
    }

    public void setBasedir(File arg0) {}

    public void setClassesDirectory(File arg0) {}

    public void setDebugForkedProcess(String arg0) {}

    public void setEncoding(String arg0) {}

    public void setExcludes(List<String> arg0) {}

    public void setExcludeJUnit5Engines(String[] arg0) {}

    public void setFailIfNoSpecifiedTests(boolean arg0) {}

    public void setForkedProcessExitTimeoutInSeconds(int arg0) {}

    public void setForkedProcessTimeoutInSeconds(int arg0) {}

    public void setIncludeJUnit5Engines(String[] arg0) {}

    public void setMainBuildPath(File arg0) {}

    public void setParallelTestsTimeoutForcedInSeconds(double arg0) {}

    public void setParallelTestsTimeoutInSeconds(double arg0) {}

    public void setPrintSummary(boolean arg0) {}

    public void setReportFormat(String arg0) {}

    public void setReportsDirectory(File arg0) {}

    public void setRunOrder(String arg0) {}

    public void setRunOrderRandomSeed(Long arg0) {}

    public void setSkip(boolean arg0) {}

    public void setSkipExec(boolean arg0) {}

    public void setSkipTests(boolean arg0) {}

    public void setSuiteXmlFiles(File[] arg0) {}

    public void setSystemPropertiesFile(File arg0) {}

    public void setTest(String arg0) {}

    public void setTestClassesDirectory(File arg0) {}

    public void setUseFile(boolean arg0) {}

    public void setUseManifestOnlyJar(boolean arg0) {}

    public void setUseModulePath(boolean arg0) {}

    public void setUseSystemClassLoader(boolean arg0) {}

    public void setIncludes(List<String> arg0) {}

    @Override
    protected String[] getDefaultIncludes() {
        return null;
    }

    @Override
    public String[] getExcludeJUnit5Engines() {
        return null;
    }

    @Override
    protected String[] getExcludedEnvironmentVariables() {
        return null;
    }

    @Override
    public List<String> getExcludes() {
        return null;
    }

    @Override
    public File getExcludesFile() {
        return null;
    }

    @Override
    public int getForkedProcessExitTimeoutInSeconds() {
        return 0;
    }

    @Override
    public String[] getIncludeJUnit5Engines() {
        return null;
    }

    @Override
    public List<String> getIncludes() {
        return null;
    }

    @Override
    public File getIncludesFile() {
        return null;
    }

    @Override
    protected String getPluginName() {
        return null;
    }

    @Override
    protected int getRerunFailingTestsCount() {
        return 0;
    }

    @Override
    public String getRunOrder() {
        return null;
    }

    @Override
    public Long getRunOrderRandomSeed() {
        return null;
    }

    @Override
    public File[] getSuiteXmlFiles() {
        return null;
    }

    @Override
    public File getSystemPropertiesFile() {
        return null;
    }

    @Override
    protected void handleSummary(RunResult arg0, Exception arg1) {}

    @Override
    protected boolean hasSuiteXmlFiles() {
        return false;
    }

    @Override
    protected boolean isSkipExecution() {
        return false;
    }

    @Override
    protected List<File> suiteXmlFiles() {
        return null;
    }
}
