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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Objects;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.tools.plugin.EnhancedParameterWrapper;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnhancedPluginDescriptorBuilderTest
{

    @Test
    void testTypeJavaDocUrlElement() throws PlexusConfigurationException, IOException
    {
        EnhancedPluginDescriptorBuilder builder = new EnhancedPluginDescriptorBuilder( false );
        try ( InputStream input = Objects.requireNonNull( this.getClass().getResourceAsStream( "/plugin-enhanced.xml" ) );
              Reader reader = new XmlStreamReader( input ) )
        {
            PluginDescriptor descriptor = builder.build( reader );
            MojoDescriptor mojoDescriptor = descriptor.getMojo( "format-xml" );
            Assertions.assertNotNull( mojoDescriptor );
            EnhancedParameterWrapper enhancedParameter = assertEnhancedParameter( mojoDescriptor, "excludes" );
            Assertions.assertEquals( URI.create( "apidocs/java/util/Set.html" ), enhancedParameter.getTypeJavadocUrl() );
            assertParameter( mojoDescriptor, "enableForIncrementalBuild" );// primitive types don't have javadoc
        }
    }

    EnhancedParameterWrapper assertEnhancedParameter( MojoDescriptor mojoDescriptor, String parameterName )
    {
        return (EnhancedParameterWrapper) assertParameter( mojoDescriptor, parameterName, true );
    }

    Parameter assertParameter( MojoDescriptor mojoDescriptor, String parameterName )
    {
        return assertParameter( mojoDescriptor, parameterName, false );
    }

    Parameter assertParameter( MojoDescriptor mojoDescriptor, String parameterName, boolean isEnhancedParameter )
    {
        // test both getParameters() and getParameterMap() as both use independent objects
        Parameter parameter = mojoDescriptor.getParameters().stream().filter( p -> p.getName().equals( parameterName ) ).findFirst().orElse( null );
        assertParameter( parameter, isEnhancedParameter );
        Parameter parameterFromMap = mojoDescriptor.getParameterMap().get( parameterName );
        assertParameter( parameterFromMap, isEnhancedParameter );
        // check that both are equal
        Assertions.assertEquals( parameter, parameterFromMap );
        return parameter;
    }

    void assertParameter( Parameter parameter, boolean isEnhhancedParameter )
    {
        Assertions.assertNotNull( parameter );
        Assertions.assertEquals( isEnhhancedParameter, parameter instanceof EnhancedParameterWrapper );
    }
}
