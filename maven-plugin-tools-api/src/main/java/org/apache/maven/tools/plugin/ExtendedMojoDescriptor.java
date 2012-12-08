package org.apache.maven.tools.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * Extensions to MojoDescriptor added to Maven 3, then are not available when run under Maven2.
 * @author Kristian Rosenvold
 */
public class ExtendedMojoDescriptor
    extends MojoDescriptor
{
    private boolean threadSafe = false;

    private String requiresDependencyCollection = null;

    public boolean isThreadSafe()
    {
        return threadSafe;
    }

    public void setThreadSafe( boolean threadSafe )
    {
        this.threadSafe = threadSafe;
    }

    public String getDependencyCollectionRequired()
    {
        return requiresDependencyCollection;
    }

    public void setDependencyCollectionRequired( String requiresDependencyCollection )
    {
        this.requiresDependencyCollection = requiresDependencyCollection;
    }
}
