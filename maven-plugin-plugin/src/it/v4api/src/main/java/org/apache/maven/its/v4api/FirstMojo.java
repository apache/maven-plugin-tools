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
package org.apache.maven.its.v4api;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.plugin.annotations.Resolution;

/**
 * Test mojo for the v4 api plugin descriptor generation.
 * This mojo is not actually runnable because:
 *  - it's using a custom lifecycle which is not defined
 *  - it has a @Inject dependency on ArtifactInstaller (hint=test) which does not exist
 *
 * @since 1.2
 */
@Mojo(name = "first", defaultPhase = "integration-test")
public class FirstMojo implements org.apache.maven.api.plugin.Mojo {

    /**
     * Project directory.
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private Path basedir;

    @Parameter(property = "first.touchFile", defaultValue = "${project.build.directory}/touch.txt", required = true)
    private Path touchFile;

    /**
     * @since 0.1
     * @deprecated As of 0.2
     */
    @Deprecated
    @Parameter(name = "namedParam", alias = "alias")
    private String aliasedParam;

    @Resolution(pathScope = "main-runtime")
    private List<Path> classPath;

    @Inject
    private Session session;

    @Inject
    private Project project;

    @Inject
    private MojoExecution mojo;

    @Inject
    private Log log;

    public void execute() throws MojoException {
        log.info("Executing first");
        for (Path path : classPath) {
            log.info(path.toString());
        }
    }
}
