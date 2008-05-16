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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.tools.plugin.util.PluginUtils;
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

        MojoDescriptor helpDescriptor = makeHelpDescriptor( pluginDescriptor );

        // Verify that no help goal already exists
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            if ( descriptor.getGoal().equals( helpDescriptor.getGoal() )
                && !descriptor.getImplementation().equals( helpDescriptor.getImplementation() ) )
            {
                if ( getLogger().isWarnEnabled() )
                {
                    getLogger().warn(
                                      "\n\nA help goal (" + descriptor.getImplementation()
                                          + ") already exists in this plugin. SKIPPED THE "
                                          + helpDescriptor.getImplementation() + " GENERATION.\n" );
                }

                return;
            }
        }

        String sourcePath = helpDescriptor.getImplementation().replace( '.', File.separatorChar ) + ".java";
        File helpClass = new File( destinationDirectory, sourcePath );
        helpClass.getParentFile().mkdirs();

        Writer writer = null;
        try
        {
            writer = new FileWriter( helpClass );
            writeClass( writer, pluginDescriptor, helpDescriptor );
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
     * Creates a minimalistic mojo descriptor for the generated help goal.
     * 
     * @param pluginDescriptor The descriptor of the plugin for which to generate a help goal, must not be
     *            <code>null</code>.
     * @return The mojo descriptor for the generated help goal, never <code>null</code>.
     */
    private static MojoDescriptor makeHelpDescriptor( PluginDescriptor pluginDescriptor )
    {
        MojoDescriptor descriptor = new MojoDescriptor();

        descriptor.setPluginDescriptor( pluginDescriptor );

        descriptor.setLanguage( "java" );

        descriptor.setGoal( HELP_GOAL );

        String packageName = discoverPackageName( pluginDescriptor );
        if ( StringUtils.isNotEmpty( packageName ) )
        {
            descriptor.setImplementation( packageName + '.' + HELP_MOJO_CLASS_NAME );
        }
        else
        {
            descriptor.setImplementation( HELP_MOJO_CLASS_NAME );
        }

        descriptor.setDescription( "Display help information on " + pluginDescriptor.getArtifactId()
            + ". Call 'mvn " + descriptor.getFullGoalName() + " -Ddetail=true' to display parameter details." );

        return descriptor;
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
     * Generates the <code>HelpMojo</code> class.
     * 
     * @param writer
     * @param pluginDescriptor
     * @param helpDescriptor
     * @throws IOException if any
     */
    private static void writeClass( Writer writer, PluginDescriptor pluginDescriptor, MojoDescriptor helpDescriptor )
        throws IOException
    {
        String packageName = "";
        String simpleName = helpDescriptor.getImplementation();
        int dot = simpleName.lastIndexOf( '.' );
        if ( dot >= 0 )
        {
            packageName = simpleName.substring( 0, dot );
            simpleName = simpleName.substring( dot + 1 );
        }

        if ( packageName.length() > 0 )
        {
            writer.write( "package " + packageName + ";" + LS );
            writer.write( LS );
        }

        writeImports( writer );
        writer.write( LS );

        writeMojoJavadoc( writer, pluginDescriptor, helpDescriptor );

        writer.write( "public class " + simpleName + LS );
        writer.write( "    extends AbstractMojo" + LS );
        writer.write( "{" + LS );

        writeVariables( writer );

        writer.write( LS );

        writeExecute( writer, pluginDescriptor, helpDescriptor );

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
        writer.write( LS );
        writer.write( "import org.apache.maven.plugin.AbstractMojo;" + LS );
        writer.write( "import org.apache.maven.plugin.MojoExecutionException;" + LS );
    }

    private static void writeMojoJavadoc( Writer writer, PluginDescriptor pluginDescriptor,
                                          MojoDescriptor helpDescriptor )
        throws IOException
    {
        writer.write( "/**" + LS );
        writer.write( " * " + helpDescriptor.getDescription() + LS );
        writer.write( " *" + LS );
        writer.write( " * @version generated on " + new Date() + LS );
        writer.write( " * @goal " + helpDescriptor.getGoal() + LS );
        writer.write( " * @requiresProject false" + LS );
        writer.write( " */" + LS );
    }

    private static void writeVariables( Writer writer )
        throws IOException
    {
        writer.write( "    /**" + LS );
        writer.write( "     * The maximum length of a display line." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private int lineLength = 80;" + LS );
        writer.write( LS );
        writer.write( "    /**" + LS );
        writer.write( "     * The number of spaces per indentation level." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private int indentSize = 2;" + LS );
        writer.write( LS );
        writer.write( "    /**" + LS );
        writer.write( "     * If <code>true</code>, display all settable properties for each goal." + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @parameter expression=\"${detail}\" default-value=\"false\"" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private boolean detail;" + LS );
    }

    private static void writeExecute( Writer writer, PluginDescriptor pluginDescriptor, MojoDescriptor helpDescriptor )
        throws IOException
    {
        List mojoDescriptors = new ArrayList();

        mojoDescriptors.add( helpDescriptor );
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) it.next();

            if ( !helpDescriptor.getGoal().equals( mojoDescriptor.getGoal() ) )
            {
                mojoDescriptors.add( mojoDescriptor );
            }
        }

        Collections.sort( mojoDescriptors, new Comparator()
        {

            public int compare( Object arg0, Object arg1 )
            {
                MojoDescriptor mojo0 = (MojoDescriptor) arg0;
                MojoDescriptor mojo1 = (MojoDescriptor) arg1;
                return mojo0.getGoal().compareToIgnoreCase( mojo1.getGoal() );
            }

        } );

        writer.write( "    /** {@inheritDoc} */" + LS );
        writer.write( "    public void execute()" + LS );
        writer.write( "        throws MojoExecutionException" + LS );
        writer.write( "    {" + LS );

        writer.write( "        StringBuffer sb = new StringBuffer();" + LS );
        writer.write( LS );

        writer.write( "        append( sb, \"" + pluginDescriptor.getId() + "\", 0 );" + LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );
        writer.write( LS );

        writer.write( "        append( sb, \""
            + StringUtils.escape( pluginDescriptor.getName() + " " + pluginDescriptor.getVersion() )
            + "\", 0 );" + LS );
        writer.write( "        append( sb, \"" + toDescription( pluginDescriptor.getDescription() ) + "\", 1 );" + LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );
        writer.write( LS );

        writer.write( "        append( sb, \"This plugin has " + mojoDescriptors.size() + " "
            + ( mojoDescriptors.size() > 1 ? "goals" : "goal" ) + ":\", 0 );" + LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );

        writer.write( LS );

        for ( Iterator it = mojoDescriptors.iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            writeGoal( writer, descriptor );
        }

        writer.write( "        if ( getLog().isInfoEnabled() )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            getLog().info( sb.toString() );" + LS );
        writer.write( "        }" + LS );
        writer.write( "    }" + LS );
    }

    private static void writeGoal( Writer writer, MojoDescriptor descriptor )
        throws IOException
    {
        writer.write( "        append( sb, \"" + descriptor.getFullGoalName() + "\", 0 );" + LS );
        writer.write( "        append( sb, \"" + toDescription( descriptor.getDescription() ) + "\", 1 );" + LS );

        if ( descriptor.getParameters() != null && descriptor.getParameters().size() > 0 )
        {
            writer.write( "        if ( detail )" + LS );
            writer.write( "        {" + LS );

            writer.write( "            append( sb, \"\", 0 );" + LS );
            writer.write( "            append( sb, \"Available parameters:\", 1 );" + LS );
            writer.write( "            append( sb, \"\", 0 );" + LS );

            for ( Iterator it = descriptor.getParameters().iterator(); it.hasNext(); )
            {
                Parameter parameter = (Parameter) it.next();

                if ( parameter.isEditable() )
                {
                    writer.write( LS );
                    writeParameter( writer, parameter );
                }
            }

            writer.write( "        }" + LS );
        }

        writer.write( LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );
        writer.write( LS );
    }

    private static void writeParameter( Writer writer, Parameter parameter )
        throws IOException
    {
        String expression = parameter.getExpression();

        if ( expression == null || !expression.startsWith( "${component." ) )
        {
            String parameterName = parameter.getName();
            String parameterDescription = toDescription( parameter.getDescription() );
            String parameterDefaultValue = parameterName
                + ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) ? " (Default: '"
                    + parameter.getDefaultValue() + "')" : "" );

            writer.write( "            append( sb, \"" + parameterDefaultValue + "\", 2 );" + LS );
            writer.write( "            append( sb, \"" + parameterDescription + "\", 3 );" + LS );
        }
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
        writer.write( "    private void append( StringBuffer sb, String description, int indent )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        for ( Iterator it = toLines( description, indent ).iterator(); it.hasNext(); )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            sb.append( it.next().toString() ).append( '\\n' );" + LS );
        writer.write( "        }" + LS );
        writer.write( "    }" + LS );

        writer.write( LS );
        writer.write( "    /** " + LS );
        writer.write( "     * Splits the specified text into lines of convenient display length." + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @param text The text to split into lines, must not be <code>null</code>." + LS );
        writer.write( "     * @param indent The base indentation level of each line, must not be negative." + LS );
        writer.write( "     * @return The sequence of display lines, never <code>null</code>." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private List toLines( String text, int indent )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        List lines = new ArrayList();" + LS );
        writer.write( LS );
        writer.write( "        String ind = repeat( \"\\t\", indent );" + LS );
        writer.write( "        String[] plainLines = text.split( \"(\\r\\n)|(\\r)|(\\n)\" );" + LS );
        writer.write( "        for ( int i = 0; i < plainLines.length; i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            toLines( lines, ind + plainLines[i] );" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        return lines;" + LS );
        writer.write( "    }" + LS );

        writer.write( LS );
        writer.write( "    /** " + LS );
        writer.write( "     * Adds the specified line to the output sequence, performing line wrapping if necessary." + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @param lines The sequence of display lines, must not be <code>null</code>." + LS );
        writer.write( "     * @param line The line to add, must not be <code>null</code>." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private void toLines( List lines, String line )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        int lineIndent = getIndentLevel( line );" + LS );
        writer.write( "        StringBuffer buf = new StringBuffer( 256 );" + LS );
        writer.write( "        String[] tokens = line.split( \" +\" );" + LS );
        writer.write( "        for ( int i = 0; i < tokens.length; i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            String token = tokens[i];" + LS );
        writer.write( "            if ( i > 0 )" + LS );
        writer.write( "            {" + LS );
        writer.write( "                if ( buf.length() + token.length() >= lineLength )" + LS );
        writer.write( "                {" + LS );
        writer.write( "                    lines.add( buf.toString() );" + LS );
        writer.write( "                    buf.setLength( 0 );" + LS );
        writer.write( "                    buf.append( repeat( \" \", lineIndent * indentSize ) );" + LS );
        writer.write( "                }" + LS );
        writer.write( "                else" + LS );
        writer.write( "                {" + LS );
        writer.write( "                    buf.append( ' ' );" + LS );
        writer.write( "                }" + LS );
        writer.write( "            }" + LS );
        writer.write( "            for ( int j = 0; j < token.length(); j++ )" + LS );
        writer.write( "            {" + LS );
        writer.write( "                char c = token.charAt( j );" + LS );
        writer.write( "                if ( c == '\\t' )" + LS );
        writer.write( "                {" + LS );
        writer.write( "                    buf.append( repeat( \" \", indentSize - buf.length() % indentSize ) );" + LS );
        writer.write( "                }" + LS );
        writer.write( "                else if ( c == '\\u00A0' )" + LS );
        writer.write( "                {" + LS );
        writer.write( "                    buf.append( ' ' );" + LS );
        writer.write( "                }" + LS );
        writer.write( "                else" + LS );
        writer.write( "                {" + LS );
        writer.write( "                    buf.append( c );" + LS );
        writer.write( "                }" + LS );
        writer.write( "            }" + LS );
        writer.write( "        }" + LS );
        writer.write( "        lines.add( buf.toString() );" + LS );
        writer.write( "    }" + LS );

        writer.write( LS );
        writer.write( "    /** " + LS );
        writer.write( "     * Gets the indentation level of the specified line." + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @param line The line whose indentation level should be retrieved, must not be <code>null</code>." + LS );
        writer.write( "     * @return The indentation level of the line." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static int getIndentLevel( String line )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        int level = 0;" + LS );
        writer.write( "        for ( int i = 0; i < line.length() && line.charAt( i ) == '\\t'; i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            level++;" + LS );
        writer.write( "        }" + LS );
        writer.write( "        for ( int i = level + 1; i <= level + 4 && i < line.length(); i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            if ( line.charAt( i ) == '\\t' )" + LS );
        writer.write( "            {" + LS );
        writer.write( "                level++;" + LS );
        writer.write( "                break;" + LS );
        writer.write( "            }" + LS );
        writer.write( "        }" + LS );
        writer.write( "        return level;" + LS );
        writer.write( "    }" + LS );
    }

    /**
     * Gets the effective string to use for the plugin/mojo/parameter description.
     * 
     * @param description The description of the element, may be <code>null</code>.
     * @return The effective description string, never <code>null</code>.
     */
    private static String toDescription( String description )
    {
        if ( StringUtils.isNotEmpty( description ) )
        {
            return StringUtils.escape( toText( description ) );
        }
        else
        {
            return "(no description available)";
        }
    }

    /**
     * Converts a HTML fragment as extracted from a javadoc comment to a plain text string. This method tries to retain
     * as much of the text formatting as possible by means of the following transformations:
     * <ul>
     * <li>List items are converted to leading tabs (U+0009), followed by the item number/bullet, another tab and
     * finally the item contents. Each tab denotes an increase of indentation.</li>
     * <li>Flow breaking elements as well as literal line terminators in preformatted text are converted to a newline
     * (U+000A) to denote a mandatory line break.</li>
     * <li>Consecutive spaces and line terminators from character data outside of preformatted text will be normalized
     * to a single space. The resulting space denotes a possible point for line wrapping.</li>
     * <li>Each space in preformatted text will be converted to a non-breaking space (U+00A0).</li>
     * </ul>
     * 
     * @param html The HTML fragment to convert to plain text, may be <code>null</code>.
     * @return A string with HTML tags converted into pure text, never <code>null</code>.
     */
    protected static String toText( String html )
    {
        if ( StringUtils.isEmpty( html ) )
        {
            return "";
        }

        final StringBuffer sb = new StringBuffer();

        HTMLEditorKit.Parser parser = new ParserDelegator();
        HTMLEditorKit.ParserCallback htmlCallback = new HTMLEditorKit.ParserCallback()
        {
            /**
             * Holds the index of the current item in a numbered list.
             */
            class Counter
            {
                public int value;
            }

            /**
             * A flag whether the parser is currently in the body element.
             */
            private boolean body;

            /**
             * A flag whether the parser is currently processing preformatted text, actually a counter to track nesting.
             */
            private int preformatted;

            /**
             * The current indentation depth for the output.
             */
            private int depth;

            /**
             * A stack of {@link Counter} objects corresponding to the nesting of (un-)ordered lists. A
             * <code>null</code> element denotes an unordered list.
             */
            private Stack numbering = new Stack();

            /**
             * A flag whether an implicit line break is pending in the output buffer. This flag is used to postpone the
             * output of implicit line breaks until we are sure that are not to be merged with other implicit line
             * breaks.
             */
            private boolean pendingNewline;

            /**
             * A flag whether we have just parsed a simple tag.
             */
            private boolean simpleTag;

            /** {@inheritDoc} */
            public void handleSimpleTag( HTML.Tag t, MutableAttributeSet a, int pos )
            {
                simpleTag = true;
                if ( body && HTML.Tag.BR.equals( t ) )
                {
                    newline( false );
                }
            }

            /** {@inheritDoc} */
            public void handleStartTag( HTML.Tag t, MutableAttributeSet a, int pos )
            {
                simpleTag = false;
                if ( body && ( t.breaksFlow() || t.isBlock() ) )
                {
                    newline( true );
                }
                if ( HTML.Tag.OL.equals( t ) )
                {
                    numbering.push( new Counter() );
                }
                else if ( HTML.Tag.UL.equals( t ) )
                {
                    numbering.push( null );
                }
                else if ( HTML.Tag.LI.equals( t ) )
                {
                    Counter counter = (Counter) numbering.peek();
                    if ( counter == null )
                    {
                        text( "-\t" );
                    }
                    else
                    {
                        text( ++counter.value + ".\t" );
                    }
                    depth++;
                }
                else if ( HTML.Tag.DD.equals( t ) )
                {
                    depth++;
                }
                else if ( t.isPreformatted() )
                {
                    preformatted++;
                }
                else if ( HTML.Tag.BODY.equals( t ) )
                {
                    body = true;
                }
            }

            /** {@inheritDoc} */
            public void handleEndTag( HTML.Tag t, int pos )
            {
                if ( HTML.Tag.OL.equals( t ) || HTML.Tag.UL.equals( t ) )
                {
                    numbering.pop();
                }
                else if ( HTML.Tag.LI.equals( t ) || HTML.Tag.DD.equals( t ) )
                {
                    depth--;
                }
                else if ( t.isPreformatted() )
                {
                    preformatted--;
                }
                else if ( HTML.Tag.BODY.equals( t ) )
                {
                    body = false;
                }
                if ( body && ( t.breaksFlow() || t.isBlock() ) && !HTML.Tag.LI.equals( t ) )
                {
                    if ( ( HTML.Tag.P.equals( t ) || HTML.Tag.PRE.equals( t ) || HTML.Tag.OL.equals( t )
                        || HTML.Tag.UL.equals( t ) || HTML.Tag.DL.equals( t ) )
                        && numbering.isEmpty() )
                    {
                        newline( pendingNewline = false );
                    }
                    else
                    {
                        newline( true );
                    }
                }
            }

            /** {@inheritDoc} */
            public void handleText( char[] data, int pos )
            {
                /*
                 * NOTE: Parsers before JRE 1.6 will parse XML-conform simple tags like <br/> as "<br>" followed by
                 * the text event ">..." so we need to watch out for the closing angle bracket.
                 */
                int offset = 0;
                if ( simpleTag && data[0] == '>' )
                {
                    simpleTag = false;
                    for ( ++offset; offset < data.length && data[offset] <= ' '; )
                    {
                        offset++;
                    }
                }
                if ( offset < data.length )
                {
                    String text = new String( data, offset, data.length - offset );
                    text( text );
                }
            }

            /** {@inheritDoc} */
            public void flush()
            {
                flushPendingNewline();
            }

            /**
             * Writes a line break to the plain text output.
             * 
             * @param implicit A flag whether this is an explicit or implicit line break. Explicit line breaks are
             *            always written to the output whereas consecutive implicit line breaks are merged into a single
             *            line break.
             */
            private void newline( boolean implicit )
            {
                if ( implicit )
                {
                    pendingNewline = true;
                }
                else
                {
                    flushPendingNewline();
                    sb.append( '\n' );
                }
            }

            /**
             * Flushes a pending newline (if any).
             */
            private void flushPendingNewline()
            {
                if ( pendingNewline )
                {
                    pendingNewline = false;
                    if ( sb.length() > 0 )
                    {
                        sb.append( '\n' );
                    }
                }
            }

            /**
             * Writes the specified character data to the plain text output. If the last output was a line break, the
             * character data will automatically be prefixed with the current indent.
             * 
             * @param data The character data, must not be <code>null</code>.
             */
            private void text( String data )
            {
                flushPendingNewline();
                if ( sb.length() <= 0 || sb.charAt( sb.length() - 1 ) == '\n' )
                {
                    for ( int i = 0; i < depth; i++ )
                    {
                        sb.append( '\t' );
                    }
                }
                String text;
                if ( preformatted > 0 )
                {
                    text = data.replace( ' ', '\u00A0' );
                }
                else
                {
                    text = data.replace( '\n', ' ' );
                }
                sb.append( text );
            }
        };

        try
        {
            parser.parse( new StringReader( PluginUtils.makeHtmlValid( html ) ), htmlCallback, true );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        return sb.toString().replace( '\"', '\'' ); // for CDATA
    }

}
