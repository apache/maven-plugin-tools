package org.apache.maven.tools.plugin.extractor.annotations.scanner;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoAnnotationsScannerRequest
{
    private List<File> classesDirectories = new ArrayList<>();

    private Set<Artifact> dependencies = new HashSet<>();

    private List<String> includePatterns = Arrays.asList( "**/*.class" );

    private List<File> sourceDirectories = new ArrayList<>();

    private MavenProject project;

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

    public Set<Artifact> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( Set<Artifact> dependencies )
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

    public List<File> getSourceDirectories()
    {
        return sourceDirectories;
    }

    public void setSourceDirectories( List<File> sourceDirectories )
    {
        this.sourceDirectories = sourceDirectories;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }
}
