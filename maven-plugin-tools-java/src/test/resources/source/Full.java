package source;

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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.tools.plugin.scanner.MojoScanner;

/**
 * Test defining every javadoc annotation.
 *
 * @goal full-annotations
 * @aggregator
 * @configurator "role-hint"
 * @execute phase="site-deploy" lifecycle="site"
 * @executionStrategy always
 * @inheritByDefault false
 * @instantiationStrategy singleton
 * @phase install
 * @requiresDependencyResolution compile+runtime
 * @requiresDependencyCollection test
 * @requiresDirectInvocation true
 * @requiresOnline true
 * @requiresProject false
 * @requiresReports true
 * @threadSafe
 * @since now
 * @deprecated deprecation text test
 */
public class Full
    extends AbstractMojo
{
    /**
     * A parameter.
     *
     * @parameter
     */
    protected String[] parameter;

    /**
     * @parameter alias="myAlias" property="aSystemProperty" default-value="${anExpression}"
     * @readonly
     * @required
     * @since tomorrow
     * @deprecated after tomorrow
     */
    private File file;

    /**
     * @parameter property="aSystemProperty"
     */
    private String property;

    /**
     * A component.
     * 
     * @component role="role" roleHint="hint"
     * @required
     */
    private MojoScanner component;

    public Full()
    {
    }

    public void execute()
    {
    }
}
