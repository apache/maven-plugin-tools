package org.apache.maven.plugin.tools.model;

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
        Set descriptors = new PluginMetadataParser().parseMojoDescriptors( metadataFile );
        
        assertEquals( 1, descriptors.size() );
        
        MojoDescriptor desc = (MojoDescriptor) descriptors.iterator().next();
        assertTrue( desc.getImplementation().indexOf( ":" ) < 0 );
        assertEquals( "test", desc.getGoal() );
    }
    
    public void testBasicDeclarationWithCall()
        throws PluginMetadataParseException
    {
        File metadataFile = getMetadataFile( "test2.mojos.xml" );
        Set descriptors = new PluginMetadataParser().parseMojoDescriptors( metadataFile );
        
        assertEquals( 1, descriptors.size() );
        
        MojoDescriptor desc = (MojoDescriptor) descriptors.iterator().next();
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
