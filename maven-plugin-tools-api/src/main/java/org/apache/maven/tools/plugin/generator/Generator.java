package org.apache.maven.tools.plugin.generator;

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

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;

import java.io.File;
import java.io.IOException;

/**
 * Generate something, for instance a plugin report, from a plugin descriptor.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public interface Generator
{
    /**
     * Execute the generation for a given plugin descriptor.
     *
     * @param destinationDirectory required
     * @param pluginDescriptor required
     * @throws IOException if any
     * 
     * @deprecated Use {@link Generator#execute(File, PluginToolsRequest)} instead.
     */
    void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException;
    
    /**
     * Execute the generation for a given plugin descriptor.
     *
     * @param destinationDirectory required
     * @param pluginDescriptor required
     * @throws IOException if any
     * 
     * @since 2.5
     */
    void execute( File destinationDirectory, PluginToolsRequest request )
        throws IOException;
}