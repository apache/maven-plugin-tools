package org.apache.maven.tools.plugin.extractor.annotations

import org.apache.maven.project.MavenProject
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScannerRequest
import org.assertj.core.api.Assertions.assertThat
import org.codehaus.plexus.testing.PlexusExtension
import org.codehaus.plexus.testing.PlexusTest
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import javax.inject.Inject

@PlexusTest
class KotlinAnnotationReaderTest
{

    @Inject
    lateinit var mojoAnnotationsScanner: MojoAnnotationsScanner

    @Test
    fun testReadKotlinMojo()
    {
        val request = MojoAnnotationsScannerRequest()
        request.classesDirectories = listOf(File(PlexusExtension.getBasedir(), "target/test-classes"))
        request.includePatterns = Arrays.asList("**/KotlinTestMojo.class")
        request.project = MavenProject()

        val mojoAnnotatedClasses = mojoAnnotationsScanner.scan(request)

        assertThat(mojoAnnotatedClasses)
            .hasSize(1)

        val mojoAnnotatedClass = mojoAnnotatedClasses.values.iterator().next()

        assertThat( mojoAnnotatedClass )
            .extracting( { it.className }, { it.mojo.name }, { it.mojo.description }, { it.mojo.since }, { it.parameters.size } )
            .contains( KotlinTestMojo::class.java.name, "kotlin", "KotlinTestMojo description", "3.7.0", 1 )

        // test parameter description
        val parameter = mojoAnnotatedClass.parameters.iterator().next()
        assertThat( parameter )
            .extracting( { it.value.className }, { it.value.description }, { it.value.since } )
            .contains( String::class.java.name,  "the cool bar to go", "3.7.0" )

    }

}
