package test;

import org.apache.maven.plugin.AbstractMojo;

/**
 * MOJO-DESCRIPTION. Some "quotation" marks and backslashes '\\', some <strong>important</strong> javadoc<br> and an
 * inline link to {@link test.AnotherMojo}.
 * 
 * @goal test
 * @deprecated As of 1.0, use the "quoted" goal instead.
 * @since 2.1
 */
public class MyMojo
    extends AbstractMojo
{

    /**
     * This parameter uses "quotation" marks and backslashes '\\' in its description. Those characters <em>must</em> be
     * escaped in Java string literals.
     * 
     * @parameter default-value="escape\\backslash"
     * @since 2.0
     */
    private String defaultParam;

    /**
     * This parameter is deprecated.
     * 
     * @parameter
     * @deprecated As of version 1.0, us the {@link #defaultParam} instead.
     */
    private String deprecatedParam;

    /**
     * @parameter expression="${test.undocumented}"
     */
    private String undocumentedParam;

    public void execute()
    {
    }

}
