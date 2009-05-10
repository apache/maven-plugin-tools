package test;

/**
 * This is the source class to be scanned for annotations. While scanning, QDox must not try to resolve references to
 * other types like the super class from the plugin class realm. The plugin class realm has no relation at all to
 * the project class path. In particular, the plugin class realm could (by incident) contain different versions of those
 * types or could be incomplete (due to exclusions). The later case leads to NoClassDefFoundErrors, crashing the scan.
 *
 * @goal test
 */
public class SomeMojo
    extends ClassB
{

}
