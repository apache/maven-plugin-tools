package test;

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
 * MOJO-DESCRIPTION. Some "quotation" marks and backslashes '\\', some <strong>important</strong> javadoc<br> and an
 * inline link to {@link test.AnotherMojo}.
 * 
 * @goal test
 * @deprecated As of 1.0, use the "quoted" goal instead.
 * @since 2.1
 */
public class MyMojo
    extends AbstractMojo
{

    /**
     * This parameter uses "quotation" marks and backslashes '\\' in its description. Those characters <em>must</em> be
     * escaped in Java string literals.
     * 
     * @parameter default-value="escape\\backslash"
     * @since 2.0
     */
    private String defaultParam;

    /**
     * This parameter is deprecated.
     * 
     * @parameter
     * @deprecated As of version 1.0, us the {@link #defaultParam} instead.
     */
    private String deprecatedParam;

    /**
     * @parameter expression="${test.undocumented}"
     * @required
     */
    private String undocumentedParam;

    /**
     * Readonly parameter: should not be proposed for configuration.
     *
     * @parameter default-value="not for configuration"
     * @readonly
     */
    private String readonly;

    public void execute()
    {
    }

}
