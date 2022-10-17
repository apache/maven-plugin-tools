package org.apache.maven.tools.plugin.extractor.annotations

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Description
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

/**
 * This Javadoc description should not be used.
 */
@Mojo( name = "kotlin" )
@Description( value = "KotlinTestMojo description", since = "3.7.0" )
class KotlinTestMojo : AbstractMojo()
{
    /**
     * the cool bar to go
     * @since 1.0
     */
    @Parameter( property = "thebar", required = true, defaultValue = "coolbar" )
    @Description( value = "the cool bar to go", since = "3.7.0" )
    protected var bar: String? = null

    @Throws( MojoExecutionException::class, MojoFailureException::class )
    override fun execute()
    {
        throw UnsupportedOperationException( "invocation not supported." )
    }

}
