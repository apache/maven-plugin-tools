package org.apache.maven.tools.plugin.generator;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates an <code>HelpMojo</code> class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.4
 */
public class PluginHelpGenerator
    extends AbstractLogEnabled
    implements Generator
{
    private static final String LS = System.getProperty( "line.separator" );

    private static final String HELP_MOJO_CLASS_NAME = "HelpMojo";

    private static final String HELP_GOAL = "help";

    /**
     * Default constructor
     */
    public PluginHelpGenerator()
    {
        this.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "PluginHelpGenerator" ) );
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        if ( pluginDescriptor.getMojos() == null || pluginDescriptor.getMojos().size() < 1 )
        {
            return;
        }

        String packageName = discoverPackageName( pluginDescriptor );

        File helpClass = new File( destinationDirectory, packageName.replace( '.', File.separatorChar )
            + File.separator + HELP_MOJO_CLASS_NAME + ".java" );

        // Verify that no help goal already exists
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            if ( descriptor.getGoal().equals( HELP_GOAL )
                && !descriptor.getImplementation().equals( packageName + "." + HELP_MOJO_CLASS_NAME ) )
            {
                if ( getLogger().isWarnEnabled() )
                {
                    getLogger().warn( "\n\nAn help goal (" + descriptor.getImplementation()
                        + ") already exists in this plugin. SKIPPED THE " + HELP_MOJO_CLASS_NAME + " GENERATION.\n" );
                }

                return;
            }
        }

        helpClass.getParentFile().mkdirs();

        Writer writer = null;
        try
        {
            writer = new FileWriter( helpClass );
            writeClass( writer, packageName, pluginDescriptor );
            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @return the help goal for the generated mojo
     */
    private static String getHelpGoalName()
    {
        return HELP_GOAL;
    }

    /**
     * @return the full help goal name for the generated mojo
     */
    private static String getFullHelpGoalName( PluginDescriptor pluginDescriptor )
    {
        return pluginDescriptor.getGoalPrefix() + ":" + getHelpGoalName();
    }

    /**
     * @param pluginDescriptor
     * @return the help description for the generated mojo
     */
    private static String getHelpDescription( PluginDescriptor pluginDescriptor )
    {
        return "Display help information on '" + pluginDescriptor.getPluginLookupKey() + "' plugin. Call 'mvn "
            + getFullHelpGoalName( pluginDescriptor ) + " -Ddetail=true' to display all details.";
    }

    /**
     * Find the best package name, based on the number of hits of actual Mojo classes.
     *
     * @param pluginDescriptor
     * @return the best name of the package for the generated mojo
     */
    private static String discoverPackageName( PluginDescriptor pluginDescriptor )
    {
        Map packageNames = new HashMap();
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            String impl = descriptor.getImplementation();
            if ( impl.lastIndexOf( '.' ) != -1 )
            {
                String name = impl.substring( 0, impl.lastIndexOf( '.' ) );
                if ( packageNames.get( name ) != null )
                {
                    int next = ( (Integer) packageNames.get( name ) ).intValue() + 1;
                    packageNames.put( name, Integer.valueOf( "" + next ) );
                }
                else
                {
                    packageNames.put( name, Integer.valueOf( "" + 1 ) );
                }
            }
            else
            {
                packageNames.put( "", Integer.valueOf( "" + 1 ) );
            }
        }

        String packageName = "";
        int max = 0;
        for ( Iterator it = packageNames.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            int value = ( (Integer) packageNames.get( key ) ).intValue();
            if ( value > max )
            {
                max = value;
                packageName = key;
            }
        }

        return packageName;
    }

    /**
     * Generated the <code>HelpMojo</code> class.
     *
     * @param writer
     * @param packageName
     * @param pluginDescriptor
     * @throws IOException if any
     */
    private static void writeClass( Writer writer, String packageName, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        if ( packageName.length() > 0 )
        {
            writer.write( "package " + packageName + ";" + LS );
            writer.write( LS );
        }

        writeImports( writer );
        writer.write( LS );

        writeMojoJavadoc( writer, pluginDescriptor );

        writer.write( "public class HelpMojo" + LS );
        writer.write( "    extends AbstractMojo" + LS );
        writer.write( "{" + LS );

        writeVariables( writer );

        writer.write( LS );

        writeExecute( writer, pluginDescriptor );

        writer.write( LS );
        writeUtilities( writer );
        writer.write( "}" + LS );
    }

    private static void writeImports( Writer writer )
        throws IOException
    {
        writer.write( "import java.util.ArrayList;" + LS );
        writer.write( "import java.util.Iterator;" + LS );
        writer.write( "import java.util.List;" + LS );
        writer.write( "import java.util.StringTokenizer;" + LS );
        writer.write( LS );
        writer.write( "import org.apache.maven.plugin.AbstractMojo;" + LS );
        writer.write( "import org.apache.maven.plugin.MojoExecutionException;" + LS );
    }

    private static void writeMojoJavadoc( Writer writer, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        writer.write( "/**" + LS );
        writer.write( " * " + getHelpDescription( pluginDescriptor ) + LS );
        writer.write( " *" + LS );
        writer.write( " * @version generated on " + new Date() + LS );
        writer.write( " * @goal " + getHelpGoalName() + LS );
        writer.write( " */" + LS );
    }

    private static void writeVariables( Writer writer )
        throws IOException
    {
        writer.write( "    /** 80-character display buffer */" + LS );
        writer.write( "    private static final int DEFAULT_WIDTH = 80;" + LS );
        writer.write( LS );
        writer.write( "    /** 4 indent spaces */" + LS );
        writer.write( "    private static final String DEFAULT_INDENT = repeat( \" \", 4 );" + LS );
        writer.write( LS );
        writer.write( "    /**" + LS );
        writer.write( "     * If true, display all settable properies for each goal." + LS );
        writer.write( "     *" + LS );
        writer.write( "     * @parameter expression=\"${detail}\" default-value=\"false\"" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private boolean detail;" + LS );
    }

    private static void writeExecute( Writer writer, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        writer.write( "    /** {@inheritDoc} */" + LS );
        writer.write( "    public void execute()" + LS );
        writer.write( "        throws MojoExecutionException" + LS );
        writer.write( "    {" + LS );

        writer.write( "        StringBuffer sb = new StringBuffer();" + LS );
        writer.write( LS );
        writer.write( "        sb.append( \"The '" + pluginDescriptor.getPluginLookupKey() + "' plugin has "
            + ( pluginDescriptor.getMojos().size() + 1 ) + " "
            + ( ( pluginDescriptor.getMojos().size() + 1 ) > 1 ? "goals" : "goal" ) + ":\" ).append( \"\\n\" );" + LS );
        writer.write( "        sb.append( \"\\n\" );" + LS );

        writer.write( LS );

        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            String goal = descriptor.getFullGoalName();
            String description = StringUtils.isNotEmpty( descriptor.getDescription() ) ? toText( descriptor
                .getDescription() ) : "No description available.";

            writer.write( "        sb.append( \"" + goal + "\" ).append( \"\\n\" );" + LS );
            writer.write( "        for ( Iterator it = toLines( \"" + description + "\" ).iterator(); it.hasNext(); )"
                + LS );
            writer.write( "        {" + LS );
            writer.write( "            sb.append( it.next().toString() ).append( \"\\n\" );" + LS );
            writer.write( "        }" + LS );

            if ( descriptor.getParameters() != null && descriptor.getParameters().size() > 0 )
            {
                writer.write( "        if ( detail )" + LS );
                writer.write( "        {" + LS );

                writer.write( "            sb.append( \"\\n\" );" + LS );
                writer.write( LS );

                writer.write( "            sb.append( repeat( \" \", 4 ) );" + LS );
                writer.write( "            sb.append( \"Available parameters:\" ).append( \"\\n\" );" + LS );
                writer.write( LS );
                writer.write( "            sb.append( \"\\n\" );" + LS );
                writer.write( LS );

                for ( Iterator it2 = descriptor.getParameters().iterator(); it2.hasNext(); )
                {
                    Parameter parameter = (Parameter) it2.next();

                    if ( parameter.isEditable() )
                    {
                        String expression = parameter.getExpression();

                        if ( expression == null || !expression.startsWith( "${component." ) )
                        {
                            String parameterName = parameter.getName();
                            String parameterDescription = StringUtils.isNotEmpty( parameter.getDescription() )
                                                                                                              ? toText( parameter
                                                                                                                  .getDescription() )
                                                                                                              : "No description available.";
                            String parameterDefaultValue = parameterName
                                + ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) ? " (Default: '"
                                    + parameter.getDefaultValue() + "')" : "" );

                            writer.write( "        for ( Iterator it = toLines( \"" + parameterDefaultValue
                                + "\", repeat( \" \", 4 ), DEFAULT_WIDTH ).iterator(); it.hasNext(); )" + LS );
                            writer.write( "        {" + LS );
                            writer.write( "            sb.append( it.next().toString() ).append( \"\\n\" );" + LS );
                            writer.write( "        }" + LS );

                            writer.write( "            for ( Iterator it = toLines( \"" + parameterDescription
                                + "\", repeat( \" \", 8 ), DEFAULT_WIDTH ).iterator(); it.hasNext(); )" + LS );
                            writer.write( "            {" + LS );
                            writer.write( "                sb.append( it.next().toString() ).append( \"\\n\" );" + LS );
                            writer.write( "            }" + LS );
                        }
                    }
                }

                writer.write( "        }" + LS );
            }

            writer.write( LS );
            writer.write( "        sb.append( \"\\n\" );" + LS );
            writer.write( LS );
        }

        // TODO Should be discovered
        writer.write( "        sb.append( \"" + getFullHelpGoalName( pluginDescriptor ) + "\" ).append( \"\\n\" );"
            + LS );
        writer.write( "        for ( Iterator it = toLines( \"" + getHelpDescription( pluginDescriptor )
            + "\" ).iterator(); it.hasNext(); )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            sb.append( it.next().toString() ).append( \"\\n\" );" + LS );
        writer.write( "        }" + LS );

        writer.write( LS );

        writer.write( "        if ( getLog().isInfoEnabled() )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            getLog().info( sb.toString() );" + LS );
        writer.write( "        }" + LS );
        writer.write( "    }" + LS );
    }

    private static void writeUtilities( Writer writer )
        throws IOException
    {
        writer.write( "    /**" + LS );
        writer.write( "     * <p>Repeat a String <code>n</code> times to form a new string.</p>" + LS );
        writer.write( "     *" + LS );
        writer.write( "     * @param str String to repeat" + LS );
        writer.write( "     * @param repeat number of times to repeat str" + LS );
        writer.write( "     * @return String with repeated String" + LS );
        writer.write( "     * @throws NegativeArraySizeException if <code>repeat < 0</code>" + LS );
        writer.write( "     * @throws NullPointerException if str is <code>null</code>" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static String repeat( String str, int repeat )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        StringBuffer buffer = new StringBuffer( repeat * str.length() );" + LS );
        writer.write( LS );
        writer.write( "        for ( int i = 0; i < repeat; i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            buffer.append( str );" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        return buffer.toString();" + LS );
        writer.write( "    }" + LS );
        writer.write( LS );
        writer.write( "    /**" + LS );
        writer.write( "     * <p>Give a list of lines for the <code>str</code>. " + "Each line is indented by 4 spaces"
            + LS );
        writer.write( "     * and has a maximum of <code>80</code> characters.</p>" + LS );
        writer.write( "     *" + LS );
        writer.write( "     * @param str String to split in lines" + LS );
        writer.write( "     * @return List of lines" + LS );
        writer.write( "     * @throws NullPointerException if str is <code>null</code>" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static List toLines( String str )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        return toLines( str, DEFAULT_INDENT, DEFAULT_WIDTH );" + LS );
        writer.write( "    }" + LS );
        writer.write( LS );
        writer.write( "    /**" + LS );
        writer
            .write( "     * <p>Give a list of lines for the <code>str</code>. Each line is indented by <code>indent</code>"
                + LS );
        writer.write( "     * and has a maximum of <code>size</code> characters.</p>" + LS );
        writer.write( "     *" + LS );
        writer.write( "     * @param str String to split in lines" + LS );
        writer.write( "     * @param indent the string to precede each line" + LS );
        writer.write( "     * @param size the size of the character display buffer" + LS );
        writer.write( "     * @return List of lines" + LS );
        writer.write( "     * @throws IllegalArgumentException if <code>size < 0</code>" + LS );
        writer.write( "     * @throws NullPointerException if str is <code>null</code>" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static List toLines( String str, String indent, int size )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        List sentences = new ArrayList();" + LS );
        writer.write( LS );
        writer.write( "        if ( indent == null )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            indent = \"\";" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        if ( size < 0 )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            throw new IllegalArgumentException( \"size should be positive\" );" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        StringBuffer tmp = new StringBuffer( indent );" + LS );
        writer.write( "        StringTokenizer tokenizer = new StringTokenizer( str, \" \" );" + LS );
        writer.write( "        while ( tokenizer.hasMoreTokens() )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            String word = tokenizer.nextToken();" + LS );
        writer.write( LS );
        writer.write( "            if ( tmp.length() + word.length() + 1 < size )" + LS );
        writer.write( "            {" + LS );
        writer.write( "                tmp.append( word ).append( \" \" );" + LS );
        writer.write( "            }" + LS );
        writer.write( "            else" + LS );
        writer.write( "            {" + LS );
        writer.write( "                sentences.add( tmp.toString() );" + LS );
        writer.write( "                tmp = new StringBuffer( indent );" + LS );
        writer.write( "                tmp.append( word ).append( \" \" );" + LS );
        writer.write( "            }" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        if ( tmp.toString().length() > 0 )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            sentences.add( tmp.toString() );" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        return sentences;" + LS );
        writer.write( "    }" + LS );
    }

    /**
     * Remove HTML tags from a string
     *
     * @param str
     * @return a String with HTML tags into pure text
     * @throws IOException if any
     */
    protected static String toText( String str )
        throws IOException
    {
        if ( StringUtils.isEmpty( str ) )
        {
            return "";
        }

        final StringBuffer sb = new StringBuffer();

        HTMLEditorKit.Parser parser = new ParserDelegator();
        HTMLEditorKit.ParserCallback htmlCallback = new HTMLEditorKit.ParserCallback()
        {
            /** {@inheritDoc} */
            public void handleText( char[] data, int pos )
            {
                // the parser parses things like <br /> as "\n>"
                if ( data[0] == '>' )
                {
                    for ( int i = 1; i < data.length; i++ )
                    {
                        if ( data[i] == '\n' )
                        {
                            sb.append( ' ' );
                        }
                        else
                        {
                            sb.append( data[i] );
                        }
                    }
                }
                else
                {
                    for ( int i = 0; i < data.length; i++ )
                    {
                        if ( data[i] == '\n' )
                        {
                            sb.append( ' ' );
                        }
                        else
                        {
                            sb.append( data[i] );
                        }
                    }
                }
            }
        };

        parser.parse( new StringReader( str ), htmlCallback, true );

        return StringUtils.replace( sb.toString(), "\"", "'" ); // for CDATA
    }
}
