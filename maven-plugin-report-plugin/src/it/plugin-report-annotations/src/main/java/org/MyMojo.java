package org;

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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Does nothing.
 *
 * @since 1.0
 * @deprecated You don't use test goals, do you?
 */
@Mojo( name = "noop", defaultPhase = LifecyclePhase.PROCESS_SOURCES,
       requiresDependencyResolution = ResolutionScope.TEST,
       requiresDirectInvocation = true, requiresOnline = true, inheritByDefault = false, aggregator = true )
@Execute( phase = LifecyclePhase.COMPILE )
public class MyMojo
    extends AbstractMojo
{

    /**
     * This is a test.
     */
    @SuppressWarnings( "unused" )
    @Parameter( required = true )
    private String required;

    /**
     * This is a test.
     *
     * @since 1.1
     * @deprecated Just testing.
     */
    @SuppressWarnings( "unused" )
    @Parameter( property = "string", defaultValue = "${project.version}/</markup-must-be-escaped>" )
    private String string;

    public void execute()
    {
        // intentional do nothing
    }

}
