package org.apache.maven.plugin.tools.model;

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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.Set;

import junit.framework.TestCase;

public class PluginMetadataParserTest
    extends TestCase
{
    
    public void testBasicDeclarationWithoutCall()
        throws PluginMetadataParseException
    {
        File metadataFile = getMetadataFile( "test.mojos.xml" );
        Set<MojoDescriptor> descriptors = new PluginMetadataParser().parseMojoDescriptors( metadataFile );
        
        assertEquals( 1, descriptors.size() );
        
        MojoDescriptor desc = descriptors.iterator().next();
        assertTrue( desc.getImplementation().indexOf( ":" ) < 0 );
        assertEquals( "test", desc.getGoal() );
    }
    
    public void testBasicDeclarationWithCall()
        throws PluginMetadataParseException
    {
        File metadataFile = getMetadataFile( "test2.mojos.xml" );
        Set<MojoDescriptor> descriptors = new PluginMetadataParser().parseMojoDescriptors( metadataFile );
        
        assertEquals( 1, descriptors.size() );
        
        MojoDescriptor desc = descriptors.iterator().next();
        assertTrue( desc.getImplementation().endsWith( ":test2" ) );
        assertEquals( "test2", desc.getGoal() );
    }
    
    private File getMetadataFile( String name )
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( name );
        if ( resource == null )
        {
            fail( "Cannot find classpath resource: '" + name + "'." );
        }
        
        return new File( StringUtils.replace( resource.getPath(), "%20", " " ) );
    }

}
