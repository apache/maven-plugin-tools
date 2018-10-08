package org.apache.maven.tools.plugin.util;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Convenience methods to play with Maven plugins.
 *
 * @author jdcasey
 *
 */
public final class PluginUtils
{
    private PluginUtils()
    {
        // nop
    }

    /**
     * Expression associated with class types to recognize Maven objects (injected in fact as parameters by <a
     * href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html">
     * maven-core's PluginParameterExpressionEvaluator</a>) like components ("real" components are injected by Plexus).
     * 
     * @deprecated wrong approach (fake components), documented parameter default values instead to learn people how to
     *             discover them
     */
    public static final Map<String, String> MAVEN_COMPONENTS;
    static
    {
        Map<String, String> mavenComponents = new HashMap<>();

        mavenComponents.put( "org.apache.maven.execution.MavenSession", "${session}" );
        mavenComponents.put( "org.apache.maven.project.MavenProject", "${project}" );
        mavenComponents.put( "org.apache.maven.plugin.MojoExecution", "${mojoExecution}" );
        mavenComponents.put( "org.apache.maven.plugin.descriptor.PluginDescriptor", "${plugin}" );
        mavenComponents.put( "org.apache.maven.settings.Settings", "${settings}" );
        
        MAVEN_COMPONENTS = Collections.unmodifiableMap( mavenComponents );
    }

    /**
     * @param basedir not null
     * @param include not null
     * @return list of included files with default SCM excluded files
     */
    public static String[] findSources( String basedir, String include )
    {
        return PluginUtils.findSources( basedir, include, null );
    }

    /**
     * @param basedir not null
     * @param include not null
     * @param exclude could be null
     * @return list of included files
     */
    public static String[] findSources( String basedir, String include, String exclude )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( basedir );
        scanner.setIncludes( new String[] { include } );
        if ( !StringUtils.isEmpty( exclude ) )
        {
            scanner.setExcludes( new String[] { exclude, StringUtils.join( FileUtils.getDefaultExcludes(), "," ) } );
        }
        else
        {
            scanner.setExcludes( FileUtils.getDefaultExcludes() );
        }

        scanner.scan();

        return scanner.getIncludedFiles();
    }

    /**
     * Sorts the specified mojo descriptors by goal name.
     *
     * @param mojoDescriptors The mojo descriptors to sort, may be <code>null</code>.
     * @see MojoDescriptor#getGoal()
     */
    public static void sortMojos( List<MojoDescriptor> mojoDescriptors )
    {
        if ( mojoDescriptors != null )
        {
            Collections.sort( mojoDescriptors, new Comparator<MojoDescriptor>()
            {
                /** {@inheritDoc} */
                public int compare( MojoDescriptor mojo0, MojoDescriptor mojo1 )
                {
                    return mojo0.getGoal().compareToIgnoreCase( mojo1.getGoal() );
                }
            } );
        }
    }

    /**
     * Sorts the specified mojo parameters by name.
     *
     * @param parameters The mojo parameters to sort, may be <code>null</code>.
     * @see Parameter#getName()
     * @since 2.4.4
     */
    public static void sortMojoParameters( List<Parameter> parameters )
    {
        if ( parameters != null )
        {
            Collections.sort( parameters, new Comparator<Parameter>()
            {
                /** {@inheritDoc} */
                public int compare( Parameter parameter1, Parameter parameter2 )
                {
                    return parameter1.getName().compareToIgnoreCase( parameter2.getName() );
                }
            } );
        }
    }
}
