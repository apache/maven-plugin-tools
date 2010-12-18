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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
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
    /** Line separator */
    private static final String LS = System.getProperty( "line.separator" );

    /** Default generated class name */
    private static final String HELP_MOJO_CLASS_NAME = "HelpMojo";

    /** Default goal */
    private static final String HELP_GOAL = "help";

    private String helpPackageName;

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
        execute( destinationDirectory, new DefaultPluginToolsRequest( null, pluginDescriptor ) );
    }
    
    /** {@inheritDoc} */
    public void execute( File destinationDirectory, PluginToolsRequest request )
        throws IOException
    {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();
        
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
            writer = new OutputStreamWriter( new FileOutputStream( helpClass ), request.getEncoding() );
            writeClass( writer, pluginDescriptor, helpDescriptor );
            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public PluginHelpGenerator setHelpPackageName( String helpPackageName )
    {
        this.helpPackageName = helpPackageName;
        return this;
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
    private MojoDescriptor makeHelpDescriptor( PluginDescriptor pluginDescriptor )
    {
        MojoDescriptor descriptor = new MojoDescriptor();

        descriptor.setPluginDescriptor( pluginDescriptor );

        descriptor.setLanguage( "java" );

        descriptor.setGoal( HELP_GOAL );

        String packageName = helpPackageName;
        if ( StringUtils.isEmpty( packageName ) )
        {
            packageName = discoverPackageName( pluginDescriptor );
        }
        if ( StringUtils.isNotEmpty( packageName ) )
        {
            descriptor.setImplementation( packageName + '.' + HELP_MOJO_CLASS_NAME );
        }
        else
        {
            descriptor.setImplementation( HELP_MOJO_CLASS_NAME );
        }

        descriptor.setDescription( "Display help information on " + pluginDescriptor.getArtifactId()
            + ".<br/> Call <pre>  mvn " + descriptor.getFullGoalName()
            + " -Ddetail=true -Dgoal=&lt;goal-name&gt;</pre> to display parameter details." );

        try
        {
            Parameter param = new Parameter();
            param.setName( "detail" );
            param.setType( "boolean" );
            param.setDescription( "If <code>true</code>, display all settable properties for each goal." );
            param.setDefaultValue( "false" );
            param.setExpression( "${detail}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "goal" );
            param.setType( "java.lang.String" );
            param.setDescription( "The name of the goal for which to show help."
                + " If unspecified, all goals will be displayed." );
            param.setExpression( "${goal}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "lineLength" );
            param.setType( "int" );
            param.setDescription( "The maximum length of a display line, should be positive." );
            param.setDefaultValue( "80" );
            param.setExpression( "${lineLength}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "indentSize" );
            param.setType( "int" );
            param.setDescription( "The number of spaces per indentation level, should be positive." );
            param.setDefaultValue( "2" );
            param.setExpression( "${indentSize}" );
            descriptor.addParameter( param );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to setup parameters for help goal", e );
        }

        return descriptor;
    }

    /**
     * Find the best package name, based on the number of hits of actual Mojo classes.
     *
     * @param pluginDescriptor not null
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
                    packageNames.put( name, new Integer( next ) );
                }
                else
                {
                    packageNames.put( name, new Integer( 1 ) );
                }
            }
            else
            {
                packageNames.put( "", new Integer( 1 ) );
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
     * @param writer not null
     * @param pluginDescriptor not null
     * @param helpDescriptor not null
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

        writeVariables( writer, helpDescriptor );

        writer.write( LS );

        writeExecute( writer, pluginDescriptor, helpDescriptor );

        writer.write( LS );
        writeUtilities( writer );
        writer.write( "}" + LS );
    }

    /**
     * @param writer not null
     * @throws IOException if any
     */
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

    /**
     * @param writer not null
     * @param pluginDescriptor not null
     * @param helpDescriptor not null
     * @throws IOException if any
     */
    private static void writeMojoJavadoc( Writer writer, PluginDescriptor pluginDescriptor,
                                          MojoDescriptor helpDescriptor )
        throws IOException
    {
        StringBuffer author = new StringBuffer();
        author.append( PluginHelpGenerator.class.getName() );

        String resource = "META-INF/maven/org.apache.maven.plugin-tools/maven-plugin-tools-api/pom.properties";
        InputStream resourceAsStream = PluginHelpGenerator.class.getClassLoader().getResourceAsStream( resource );

        if ( resourceAsStream != null )
        {
            try
            {
                Properties properties = new Properties();
                properties.load( resourceAsStream );

                author.append( " (version " ).append( properties.getProperty( "version", "unknown" ) ).append( ")" );
            }
            catch ( IOException e )
            {
                // nope
            }
        }

        writer.write( "/**" + LS );
        writer.write( " * " + helpDescriptor.getDescription() + LS );
        writer.write( " *" + LS );
        writer.write( " * @version generated on " + new Date() + LS );
        writer.write( " * @author " + author.toString() + LS );
        writer.write( " * @goal " + helpDescriptor.getGoal() + LS );
        writer.write( " * @requiresProject false" + LS );
        writer.write( " * @threadSafe" + LS );
        writer.write( " */" + LS );
    }

    /**
     * @param writer not null
     * @param helpDescriptor not null
     * @throws IOException if any
     */
    private static void writeVariables( Writer writer, MojoDescriptor helpDescriptor )
        throws IOException
    {
        for ( Iterator it = helpDescriptor.getParameters().iterator(); it.hasNext(); )
        {
            Parameter param = (Parameter) it.next();
            writer.write( "    /**" + LS );
            writer.write( "     * " + StringUtils.escape( param.getDescription() ) + LS );
            writer.write( "     * " + LS );
            writer.write( "     * @parameter" );
            if ( StringUtils.isNotEmpty( param.getExpression() ) )
            {
                writer.write( " expression=\"" );
                writer.write( StringUtils.escape( param.getExpression() ) );
                writer.write( "\"" );
            }
            if ( StringUtils.isNotEmpty( param.getDefaultValue() ) )
            {
                writer.write( " default-value=\"" );
                writer.write( StringUtils.escape( param.getDefaultValue() ) );
                writer.write( "\"" );
            }
            writer.write( LS );
            writer.write( "     */" + LS );
            writer.write( "    private " + param.getType() + " " + param.getName() + ";" + LS );
            writer.write( LS );
        }
    }

    /**
     * @param writer not null
     * @param pluginDescriptor not null
     * @param helpDescriptor not null
     * @throws IOException if any
     */
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

        PluginUtils.sortMojos( mojoDescriptors );

        writer.write( "    /** {@inheritDoc} */" + LS );
        writer.write( "    public void execute()" + LS );
        writer.write( "        throws MojoExecutionException" + LS );
        writer.write( "    {" + LS );

        writer.write( "        if ( lineLength <= 0 )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            getLog().warn( \"The parameter 'lineLength' should be positive, using '80' as "
            + "default.\" );" + LS );
        writer.write( "            lineLength = 80;" + LS );
        writer.write( "        }" + LS );
        writer.write( "        if ( indentSize <= 0 )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            getLog().warn( \"The parameter 'indentSize' should be positive, using '2' as "
            + "default.\" );" + LS );
        writer.write( "            indentSize = 2;" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );

        writer.write( "        StringBuffer sb = new StringBuffer();" + LS );
        writer.write( LS );

        writer.write( "        append( sb, \"" + StringUtils.escape( pluginDescriptor.getId() ) + "\", 0 );" + LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );
        writer.write( LS );

        if ( StringUtils.isNotEmpty( pluginDescriptor.getName() )
            && ( pluginDescriptor.getName().indexOf( pluginDescriptor.getId() ) != -1 ) )
        {
            writer.write( "        append( sb, \""
                + StringUtils.escape( pluginDescriptor.getName() + " " + pluginDescriptor.getVersion() )
                + "\", 0 );" + LS );
        }
        else
        {
            if ( StringUtils.isNotEmpty( pluginDescriptor.getName() ) )
            {
                writer.write( "        append( sb, \"" + StringUtils.escape( pluginDescriptor.getName() )
                    + "\", 0 );" + LS );
            }
            else
            {
                writer.write( "        append( sb, \"" + StringUtils.escape( pluginDescriptor.getId() )
                    + "\", 0 );" + LS );
            }
        }
        writer.write( "        append( sb, \"" + toDescription( pluginDescriptor.getDescription() ) + "\", 1 );"
            + LS );
        writer.write( "        append( sb, \"\", 0 );" + LS );
        writer.write( LS );

        writer.write( "        if ( goal == null || goal.length() <= 0 )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            append( sb, \"This plugin has " + mojoDescriptors.size() + " "
            + ( mojoDescriptors.size() > 1 ? "goals" : "goal" ) + ":\", 0 );" + LS );
        writer.write( "            append( sb, \"\", 0 );" + LS );
        writer.write( "        }" + LS );

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

    /**
     * @param writer not null
     * @param descriptor not null
     * @throws IOException if any
     */
    private static void writeGoal( Writer writer, MojoDescriptor descriptor )
        throws IOException
    {
        String goalDescription = toDescription( descriptor.getDescription() );

        writer.write( "        if ( goal == null || goal.length() <= 0 || \""
            + StringUtils.escape( descriptor.getGoal() ) + "\".equals( goal ) )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            append( sb, \"" + StringUtils.escape( descriptor.getFullGoalName() ) + "\", 0 );"
            + LS );
        if ( StringUtils.isNotEmpty( descriptor.getDeprecated() ) )
        {
            writer.write( "            append( sb, \"Deprecated. " + toDescription( descriptor.getDeprecated() )
                + "\", 1 );" + LS );
            writer.write( "            if ( detail )" + LS );
            writer.write( "            {" + LS );
            writer.write( "                append( sb, \"\", 0 );" + LS );
            writer.write( "                append( sb, \"" + goalDescription + "\", 1 );" + LS );
            writer.write( "            }" + LS );
        }
        else
        {
            writer.write( "            append( sb, \"" + goalDescription + "\", 1 );" + LS );
        }
        writer.write( "            append( sb, \"\", 0 );" + LS );

        if ( descriptor.getParameters() != null && descriptor.getParameters().size() > 0 )
        {
            List params = descriptor.getParameters();

            PluginUtils.sortMojoParameters( params );

            writer.write( "            if ( detail )" + LS );
            writer.write( "            {" + LS );

            writer.write( "                append( sb, \"Available parameters:\", 1 );" + LS );
            writer.write( "                append( sb, \"\", 0 );" + LS );

            for ( Iterator it = params.iterator(); it.hasNext(); )
            {
                Parameter parameter = (Parameter) it.next();

                if ( parameter.isEditable() )
                {
                    writer.write( LS );
                    writeParameter( writer, parameter );
                }
            }

            writer.write( "            }" + LS );
        }

        writer.write( "        }" + LS );
        writer.write( LS );
    }

    /**
     * @param writer not null
     * @param parameter not null
     * @throws IOException if any
     */
    private static void writeParameter( Writer writer, Parameter parameter )
        throws IOException
    {
        String expression = parameter.getExpression();

        if ( expression == null || !expression.startsWith( "${component." ) )
        {
            String parameterName = StringUtils.escape( parameter.getName() );
            String parameterDescription = toDescription( parameter.getDescription() );
            String parameterDefaultValue = "";
            if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) )
            {
                parameterDefaultValue = " (Default: " + StringUtils.escape( parameter.getDefaultValue() ) + ")";
            }
            writer.write( "                append( sb, \"" + parameterName + parameterDefaultValue + "\", 2 );" + LS );
            if ( StringUtils.isNotEmpty( parameter.getDeprecated() ) )
            {
                writer.write( "                append( sb, \"Deprecated. " + toDescription( parameter.getDeprecated() )
                    + "\", 3 );" + LS );
                writer.write( "                append( sb, \"\", 0 );" + LS );
            }
            writer.write( "                append( sb, \"" + parameterDescription + "\", 3 );" + LS );
            if ( parameter.isRequired() )
            {
                writer.write( "                append( sb, \"Required: Yes\", 3 );" + LS );
            }
            if ( StringUtils.isNotEmpty( parameter.getExpression() ) )
            {
                writer.write( "                append( sb, \"Expression: "
                    + StringUtils.escape( parameter.getExpression() ) + "\", 3 );" + LS );
            }
            writer.write( "                append( sb, \"\", 0 );" + LS );
        }
    }

    /**
     * @param writer not null
     * @throws IOException if any
     */
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
        writer.write( "    /** " + LS );
        writer.write( "     * Append a description to the buffer by respecting the indentSize and lineLength "
            + "parameters." + LS );
        writer.write( "     * <b>Note</b>: The last character is always a new line." + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @param sb The buffer to append the description, not <code>null</code>." + LS );
        writer.write( "     * @param description The description, not <code>null</code>." + LS );
        writer.write( "     * @param indent The base indentation level of each line, must not be negative." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private void append( StringBuffer sb, String description, int indent )" + LS );
        writer.write( "    {" + LS );
        writer.write( "        for ( Iterator it = toLines( description, indent, indentSize, lineLength )"
            + ".iterator(); it.hasNext(); )" + LS );
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
        writer.write( "     * @param indentSize The size of each indentation, must not be negative." + LS );
        writer.write( "     * @param lineLength The length of the line, must not be negative." + LS );
        writer.write( "     * @return The sequence of display lines, never <code>null</code>." + LS );
        writer.write( "     * @throws NegativeArraySizeException if <code>indent < 0</code>" + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static List toLines( String text, int indent, int indentSize, int lineLength )"
            + LS );
        writer.write( "    {" + LS );
        writer.write( "        List lines = new ArrayList();" + LS );
        writer.write( LS );
        writer.write( "        String ind = repeat( \"\\t\", indent );" + LS );
        writer.write( "        String[] plainLines = text.split( \"(\\r\\n)|(\\r)|(\\n)\" );" + LS );
        writer.write( "        for ( int i = 0; i < plainLines.length; i++ )" + LS );
        writer.write( "        {" + LS );
        writer.write( "            toLines( lines, ind + plainLines[i], indentSize, lineLength );" + LS );
        writer.write( "        }" + LS );
        writer.write( LS );
        writer.write( "        return lines;" + LS );
        writer.write( "    }" + LS );

        writer.write( LS );
        writer.write( "    /** " + LS );
        writer.write( "     * Adds the specified line to the output sequence, performing line wrapping if necessary."
            + LS );
        writer.write( "     * " + LS );
        writer.write( "     * @param lines The sequence of display lines, must not be <code>null</code>." + LS );
        writer.write( "     * @param line The line to add, must not be <code>null</code>." + LS );
        writer.write( "     * @param indentSize The size of each indentation, must not be negative." + LS );
        writer.write( "     * @param lineLength The length of the line, must not be negative." + LS );
        writer.write( "     */" + LS );
        writer.write( "    private static void toLines( List lines, String line, int indentSize, int lineLength )"
            + LS );
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
        writer.write( "                    buf.append( repeat( \" \", indentSize - buf.length() % indentSize ) );"
            + LS );
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
        writer.write( "     * @param line The line whose indentation level should be retrieved, must not be "
            + "<code>null</code>." + LS );
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
            return StringUtils.escape( PluginUtils.toText( description ) );
        }

        return "(no description available)";
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
     * @deprecated since 2.4.3, using {@link PluginUtils#toText(String)} instead of.
     */
    protected static String toText( String html )
    {
        return PluginUtils.toText( html );
    }
}
