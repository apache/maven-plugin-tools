package org.apache.maven.plugin.plugin.report_old;

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

import java.util.Properties;

/**
 * Plugin requirements.
 *
 * @deprecated will be removed in the next major version
 */
@Deprecated
public class Requirements
{
    /**
     * The minimum version of Maven to run this plugin.
     */
    private String maven;

    /**
     * The minimum version of the JDK to run this plugin.
     */
    private String jdk;

    /**
     * The minimum memory needed to run this plugin.
     */
    private String memory;

    /**
     * The minimum diskSpace needed to run this plugin.
     */
    private String diskSpace;

    /**
     * Field others.
     */
    private java.util.Properties others;

    public String getMaven()
    {
        return maven;
    }

    public String getJdk()
    {
        return jdk;
    }

    public String getMemory()
    {
        return memory;
    }

    public String getDiskSpace()
    {
        return diskSpace;
    }

    public Properties getOthers()
    {
        return others;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Requirements{" );
        sb.append( "maven='" ).append( maven ).append( '\'' );
        sb.append( ", jdk='" ).append( jdk ).append( '\'' );
        sb.append( ", memory='" ).append( memory ).append( '\'' );
        sb.append( ", diskSpace='" ).append( diskSpace ).append( '\'' );
        sb.append( ", others=" ).append( others );
        sb.append( '}' );
        return sb.toString();
    }
}
