package org.apache.maven.plugin.coreit;

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
import org.apache.maven.project.MavenProjectHelper;

/**
 * Checks maximum annotations with non-default values.
 *
 * @goal maximal
 * @aggregator
 * @configurator configurator-hint
 * @execute phase="compile"
 * @executionStrategy always
 * @inheritByDefault false
 * @instantiationStrategy singleton
 * @phase package
 * @requiresDependencyResolution compile
 * @requiresDependencyCollection test
 * @requiresDirectInvocation true
 * @requiresOnline true
 * @requiresProject false
 * @requiresReports true
 * @threadSafe
 * @since since-text
 * @deprecated deprecated-text
 */
public class Maximal
    extends AbstractMojo
{
    /**
     * Parameter description.
     *
     * @parameter alias="myAlias" implementation="my.implementation" property="aProperty" default-value="${anExpression}"
     * @readonly
     * @required
     * @since since-text
     * @deprecated deprecated-text
     */
    private String param;

    /**
     * @component role="org.apache.maven.project.MavenProjectHelper" roleHint="test"
     * @since since-text
     * @deprecated deprecated-text
     */
    private Object projectHelper;

    public void execute()
    {
    }

}
