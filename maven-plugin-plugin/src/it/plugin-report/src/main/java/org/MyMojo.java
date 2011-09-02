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

/**
 * Does nothing.
 * 
 * @goal noop
 * @phase process-sources
 * @requiresDependencyResolution test
 * @requiresDirectInvocation true
 * @requiresOnline
 * @inheritByDefault false
 * @execute phase="compile"
 * @aggregator
 * @since 1.0
 * 
 * @deprecated You don't use test goals, do you?
 */
public class MyMojo
    extends AbstractMojo
{

    /**
     * This is a test.
     * 
     * @parameter
     * @required
     */
    @SuppressWarnings( "unused" )
    private String required;

    /**
     * This is a test.
     * 
     * @parameter expression="${string}" default-value="${project.version}/</markup-must-be-escaped>"
     * @deprecated Just testing.
     * @since 1.1
     */
    @SuppressWarnings( "unused" )
    private String string;

    public void execute()
    {
        // intentional do nothing
    }

}
