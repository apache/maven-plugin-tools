package org.apache.maven.plugin.testing;

import java.io.IOException;

import junit.framework.TestCase;

public class ArtifactStubFactoryTest
    extends TestCase
{
    public void testVersionChecks() throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        assertTrue(factory.getReleaseArtifact().isRelease());
        assertFalse(factory.getReleaseArtifact().isSnapshot());
        assertTrue(factory.getSnapshotArtifact().isSnapshot());
        assertFalse(factory.getSnapshotArtifact().isRelease());
        
    }
    
    public void testCreateFiles() throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        assertFalse(factory.isCreateFiles());
    }
}
