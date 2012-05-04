package org.apache.maven.tools.plugin.annotations.scanner;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoAnnotationsScannerRequest
{
    private List<File> classesDirectories = new ArrayList<File>();

    private List<File> dependencies = new ArrayList<File>();

    private List<String> includePatterns = Arrays.asList( "**/*.class" );

    public MojoAnnotationsScannerRequest()
    {
        // no o
    }

    public List<File> getClassesDirectories()
    {
        return classesDirectories;
    }

    public void setClassesDirectories( List<File> classesDirectories )
    {
        this.classesDirectories = classesDirectories;
    }

    public List<File> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( List<File> dependencies )
    {
        this.dependencies = dependencies;
    }

    public List<String> getIncludePatterns()
    {
        return includePatterns;
    }

    public void setIncludePatterns( List<String> includePatterns )
    {
        this.includePatterns = includePatterns;
    }
}
