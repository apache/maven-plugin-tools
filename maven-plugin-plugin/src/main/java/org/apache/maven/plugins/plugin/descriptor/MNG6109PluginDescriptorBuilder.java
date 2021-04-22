package org.apache.maven.plugins.plugin.descriptor;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
 * Reads the plugin descriptor and adds the fix for MNG-6109 when using Maven-3.3.9 and before.
 * Class can be removed once Maven 3.5.0 is the prerequisite for this plugin.
 * 
 * @author Robert Scholte
 * @since 3.5.1
 */
public class MNG6109PluginDescriptorBuilder extends PluginDescriptorBuilder
{

    @Override
    public MojoDescriptor buildComponentDescriptor( PlexusConfiguration c, PluginDescriptor pluginDescriptor )
        throws PlexusConfigurationException
    {
        MojoDescriptor mojoDescriptor = super.buildComponentDescriptor( c, pluginDescriptor );
        
        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        PlexusConfiguration[] parameterConfigurations = c.getChild( "parameters" ).getChildren( "parameter" );

        for ( PlexusConfiguration d : parameterConfigurations )
        {
            String parameterName = d.getChild( "name" ).getValue();
            Parameter pd = mojoDescriptor.getParameterMap().get( parameterName );
            
            String parameterSince = d.getChild( "since" ).getValue();
            pd.setSince( parameterSince );
        }
        
        return mojoDescriptor;
    }
}
