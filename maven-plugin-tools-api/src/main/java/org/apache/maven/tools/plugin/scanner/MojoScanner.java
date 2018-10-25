package org.apache.maven.tools.plugin.scanner;

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

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;

import java.util.Set;

/**
 * @author jdcasey
 *
 */
public interface MojoScanner
{
    /** Plexus role for lookup */
    String ROLE = MojoScanner.class.getName();

    /**
     * @param request not null
     * @throws ExtractionException if any
     * @throws InvalidPluginDescriptorException if any
     * @since 2.5
     */
    void populatePluginDescriptor( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException;

    /**
     * <p>
     * Sets the active extractors.
     * </p>
     * <p>
     * Only the specified extractors will be used, all others will be skipped.
     * </p>
     * @param extractors The names of the active extractors. If this parameter is <code>null</code>,
     * all the scanner's extractors are considered active. Set entries that are <code>null</code> or
     * empty ("") will be ignored.
     */
    void setActiveExtractors( Set<String> extractors );

}
