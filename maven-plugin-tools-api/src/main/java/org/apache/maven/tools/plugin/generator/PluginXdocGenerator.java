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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.w3c.tidy.Tidy;

/**
 * @todo add example usage tag that can be shown in the doco
 * @version $Id$
 */
public class PluginXdocGenerator
    implements Generator
{
    private final Locale locale;

    private final MavenProject project;

    /**
     * Default constructor using <code>Locale.ENGLISH</code> as locale.
     * Used only in test cases.
     */
    public PluginXdocGenerator()
    {
        this.project = null;
        this.locale = Locale.ENGLISH;
    }

    /**
     * Constructor using <code>Locale.ENGLISH</code> as locale.
     *
     * @param project not null Maven project.
     */
    public PluginXdocGenerator( MavenProject project )
    {
        this.project = project;
        this.locale = Locale.ENGLISH;
    }

    /**
     * @param locale not null wanted locale.
     */
    public PluginXdocGenerator( MavenProject project, Locale locale )
    {
        this.project = project;
        if ( locale == null )
        {
            this.locale = Locale.ENGLISH;
        }
        else
        {
            this.locale = locale;
        }
    }

    /** {@inheritDoc} */
    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();
            processMojoDescriptor( descriptor, destinationDirectory );
        }
    }

    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, File destinationDirectory )
        throws IOException
    {
        File outputFile = new File( destinationDirectory, getMojoFilename( mojoDescriptor, "xml" ) );
        OutputStreamWriter writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( outputFile ), "UTF-8" );

            writeBody( writer, mojoDescriptor );

            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private String getMojoFilename( MojoDescriptor mojo, String ext )
    {
        return mojo.getGoal() + "-mojo." + ext;
    }

    private void writeBody( OutputStreamWriter writer, MojoDescriptor mojoDescriptor )
    {
        XMLWriter w = new PrettyPrintXMLWriter( new PrintWriter( writer ), writer.getEncoding(), null );

        w.startElement( "document" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "properties" );

        w.startElement( "title" );

        // TODO: need a friendly name for a plugin
        w.writeText( mojoDescriptor.getPluginDescriptor().getArtifactId() + " - " + mojoDescriptor.getFullGoalName() );

        w.endElement(); // title

        w.endElement(); // properties

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "body" );

        w.startElement( "section" );

        w.addAttribute( "name", mojoDescriptor.getFullGoalName() );

        writeReportNotice( mojoDescriptor, w );

        w.startElement( "p" );
        w.writeMarkup( "<strong>"+ getBundle( locale ).getString( "pluginxdoc.mojodescriptor.fullname" ) + "</strong>:" );
        w.endElement(); //p
        w.startElement( "p" );
        w.writeMarkup( mojoDescriptor.getPluginDescriptor().getGroupId() + ":"
            + mojoDescriptor.getPluginDescriptor().getArtifactId() + ":"
            + mojoDescriptor.getPluginDescriptor().getVersion() + ":" + mojoDescriptor.getGoal() );
        w.endElement(); //p

        w.startElement( "p" );
        w.writeMarkup( "<strong>"+ getBundle( locale ).getString( "pluginxdoc.description" ) + "</strong>:" );
        w.endElement(); //p

        w.startElement( "p" );
        if ( mojoDescriptor.getDescription() != null )
        {
            w.writeMarkup( makeHtmlValid( mojoDescriptor.getDescription() ) );
        }
        else
        {
            w.writeText( getBundle( locale ).getString( "pluginxdoc.nodescription" ) );
        }

        w.endElement(); // p

        writeGoalAttributes( mojoDescriptor, w );

        writeGoalParameterTable( mojoDescriptor, w );

        w.endElement(); // section

        w.endElement(); // body

        w.endElement(); // document
    }

    private void writeReportNotice( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        if ( PluginUtils.isMavenReport( mojoDescriptor.getImplementation(), project ) )
        {
            w.startElement( "p" );
            w.writeMarkup( "<strong>" + getBundle( locale ).getString( "pluginxdoc.mojodescriptor.notice.note" )
                + "</strong>: " );
            w.writeText( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.notice.isMavenReport" ) );
            w.endElement(); //p
        }
    }

    private void writeGoalAttributes( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        w.startElement( "p" );
        w.writeMarkup( "<strong>"+ getBundle( locale ).getString( "pluginxdoc.mojodescriptor.attributes" ) + "</strong>:" );
        w.endElement(); //p

        w.startElement( "ul" );

        String value = mojoDescriptor.getDeprecated();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.deprecated" ) + ": " + value + "." );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isProjectRequired() )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.projectRequired" ) );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isAggregator() )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.aggregator" ) );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isDirectInvocationOnly() )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.directInvocationOnly" ) );
            w.endElement(); //li
        }

        value = mojoDescriptor.isDependencyResolutionRequired();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.dependencyResolutionRequired" ) + ": <code>" + value + "</code>." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getSince();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.since" ) + ": <code>" + value + "</code>." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getPhase();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.phase" ) + ": <code>" + value + "</code>." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecutePhase();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.executePhase1" ) + " <code>" + value
                + "</code> " + getBundle( locale ).getString( "pluginxdoc.mojodescriptor.executePhase2" ) + "." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecuteGoal();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.executeGoal1" ) + " <code>" + value
                + "</code> " + getBundle( locale ).getString( "pluginxdoc.mojodescriptor.executeGoal2" ) + "." );
            w.endElement(); //li
        }

        value = mojoDescriptor.getExecuteLifecycle();
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.executeLifecycle" ) + ": <code>" + value + "</code>." );
            w.endElement(); //li
        }

        if ( mojoDescriptor.isOnlineRequired() )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.onlineRequired" ) );
            w.endElement(); //li
        }

        if ( !mojoDescriptor.isInheritedByDefault() )
        {
            w.startElement( "li" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.inheritedByDefault" ) );
            w.endElement(); //li
        }

        w.endElement();//ul
    }

    private void writeGoalParameterTable( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        List parameterList = mojoDescriptor.getParameters();

        //remove components and read-only parameters
        List list = filterParameters( parameterList );

        if ( list != null && list.size() > 0 )
        {
            writeParameterSummary( mojoDescriptor, list, w );

            writeParameterDetails( mojoDescriptor, list, w );
        }
        else
        {
            w.startElement( "subsection" );
            w.addAttribute( "name", getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameters" ) );

            w.startElement( "p" );
            w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.noParameter" ) );
            w.endElement(); //p

            w.endElement();
        }
    }

    private List filterParameters( List parameterList )
    {
        List filtered = new ArrayList();

        if ( parameterList != null )
        {
            for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
            {
                Parameter parameter = (Parameter) parameters.next();

                if ( parameter.isEditable() )
                {
                    String expression = parameter.getExpression();

                    if ( expression == null || !expression.startsWith( "${component." ) )
                    {
                        filtered.add( parameter );
                    }
                }
            }
        }

        return filtered;
    }

    private void writeParameterDetails( MojoDescriptor mojoDescriptor, List parameterList, XMLWriter w )
    {
        w.startElement( "subsection" );
        w.addAttribute( "name", getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.details" ) );

        for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            w.startElement( "p" );
            w.writeMarkup( "<strong><a name=\"" + parameter.getName() + "\">" + parameter.getName() + "</a>:</strong>" );
            w.endElement(); //p

            String description = parameter.getDescription();
            if ( StringUtils.isEmpty( description ) )
            {
                description = getBundle( locale ).getString( "pluginxdoc.nodescription" );
            }
            else
            {
                description = makeHtmlValid( description );
            }
            w.startElement( "p" );
            w.writeMarkup( description );
            w.endElement(); //p

            w.startElement( "ul" );

            writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.deprecated" ), parameter.getDeprecated(), w );

            writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.type" ), parameter.getType(), w );

            if ( StringUtils.isNotEmpty( parameter.getSince() ) )
            {
                writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.since" ), parameter.getSince(), w );
            }
            else
            {
                if ( StringUtils.isNotEmpty( mojoDescriptor.getSince() ) )
                {
                    writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.since" ), mojoDescriptor.getSince(), w );
                }
            }

            if ( parameter.isRequired() )
            {
                writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.required" ), getBundle( locale ).getString( "pluginxdoc.yes" ), w );
            }
            else
            {
                writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.required" ), getBundle( locale ).getString( "pluginxdoc.no" ), w );
            }

            writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.expression" ), parameter.getExpression(), w );

            writeDetail( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.default" ), parameter.getDefaultValue(), w );

            w.endElement();//ul

            if ( parameters.hasNext() )
            {
                w.writeMarkup( "<hr/>" );
            }
        }

        w.endElement();
    }

    private void writeDetail( String param, String value, XMLWriter w )
    {
        if ( StringUtils.isNotEmpty( value ) )
        {
            w.startElement( "li" );
            w.writeMarkup( "<strong>" + param + "</strong>: <code>" );
            w.writeText( value );
            w.writeMarkup( "</code>" );
            w.endElement(); //li
        }
    }

    private void writeParameterSummary( MojoDescriptor mojoDescriptor, List parameterList, XMLWriter w )
    {
        List requiredParams = getParametersByRequired( true, parameterList );
        if ( requiredParams.size() > 0 )
        {
            writeParameterList( mojoDescriptor, getBundle( locale ).getString( "pluginxdoc.mojodescriptor.requiredParameters" ), requiredParams, w );
        }

        List optionalParams = getParametersByRequired( false, parameterList );
        if ( optionalParams.size() > 0 )
        {
            writeParameterList( mojoDescriptor, getBundle( locale ).getString( "pluginxdoc.mojodescriptor.optionalParameters" ), optionalParams, w );
        }
    }

    private void writeParameterList( MojoDescriptor mojoDescriptor, String title, List parameterList, XMLWriter w )
    {
        w.startElement( "subsection" );
        w.addAttribute( "name", title );

        w.startElement( "table" );
        w.addAttribute( "border", "0" );
        w.addAttribute( "align", "left" );

        w.startElement( "tr" );
        w.startElement( "th" );
        w.writeText( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.name" ) );
        w.endElement();//th
        w.startElement( "th" );
        w.writeText( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.type" ) );
        w.endElement();//th
        w.startElement( "th" );
        w.writeText( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.since" ) );
        w.endElement();//th
        w.startElement( "th" );
        w.writeText( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.description" ) );
        w.endElement();//th
        w.endElement();//tr

        for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            w.startElement( "tr" );
            w.startElement( "td" );
            w.writeMarkup( "<strong><a href=\"#" + parameter.getName() + "\">" + parameter.getName() + "</a></strong>" );
            w.endElement();//td
            w.startElement( "td" );
            int index = parameter.getType().lastIndexOf( "." );
            w.writeMarkup( "<code>" + parameter.getType().substring( index + 1 ) + "</code>" );
            w.endElement();//td
            w.startElement( "td" );
            if ( StringUtils.isNotEmpty( parameter.getSince() ) )
            {
                w.writeMarkup( "<code>" + parameter.getSince() + "</code>" );
            }
            else
            {
                if ( StringUtils.isNotEmpty( mojoDescriptor.getSince() ) )
                {
                    w.writeMarkup( "<code>" + mojoDescriptor.getSince() + "</code>" );
                }
                else
                {
                    w.writeMarkup( "<code>-</code>" );
                }
            }
            w.endElement();//td
            w.startElement( "td" );
            String description = parameter.getDescription();
            if ( StringUtils.isEmpty( description ) )
            {
                description = getBundle( locale ).getString( "pluginxdoc.nodescription" );
            }
            else
            {
                description = makeHtmlValid( description );
            }
            if ( StringUtils.isNotEmpty( parameter.getDeprecated() ) )
            {
                description = "<strong>" + getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.deprecated" ) + "</strong>. " + description;
            }
            w.writeMarkup( description + " " );

            if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) )
            {
                w.writeMarkup( getBundle( locale ).getString( "pluginxdoc.mojodescriptor.parameter.defaultValue" ) + ": <code>" );
                w.writeText( parameter.getDefaultValue() );
                w.writeMarkup( "</code>." );
            }
            w.endElement();//td
            w.endElement(); //tr
        }

        w.endElement();//table
        w.endElement();//section
    }

    private List getParametersByRequired( boolean required, List parameterList )
    {
        List list = new ArrayList();

        for ( Iterator parameters = parameterList.iterator(); parameters.hasNext(); )
        {
            Parameter parameter = (Parameter) parameters.next();

            if ( parameter.isRequired() == required )
            {
                list.add( parameter );
            }
        }

        return list;
    }

    /**
     * @param description Javadoc description with HTML tags
     * @return the description with valid HTML tags
     */
    protected static String makeHtmlValid( String description )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return "";
        }

        StringOutputStream out = new StringOutputStream();

        // Using jTidy to clean comment
        Tidy tidy = new Tidy();
        tidy.setDocType( "loose" );
        tidy.setXHTML( true );
        tidy.setXmlOut( true );
        tidy.setMakeClean( true );
        tidy.setQuiet( true );
        tidy.setShowWarnings( false );
        tidy.parse( new StringInputStream( decodeJavadocTags( description ) ), out );

        // strip the header/body stuff
        String LS = System.getProperty( "line.separator" );
        String commentCleaned = out.toString();
        if ( StringUtils.isEmpty( commentCleaned ) )
        {
            return "";
        }
        int startPos = commentCleaned.indexOf( "<body>" + LS ) + 6 + LS.length();
        int endPos = commentCleaned.indexOf( LS + "</body>" );

        return commentCleaned.substring( startPos, endPos );
    }

    /**
     * Decodes javadoc inline tags into equivalent HTML tags. For instance, the inline tag "{@code <A&B>}" should be
     * rendered as "<code>&lt;A&amp;B&gt;</code>".
     *
     * @param description The javadoc description to decode, may be <code>null</code>.
     * @return The decoded description, never <code>null</code>.
     */
    protected static String decodeJavadocTags( String description )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return "";
        }

        StringBuffer decoded = new StringBuffer( description.length() + 1024 );

        Matcher matcher = Pattern.compile( "\\{@(\\w+)\\s*([^\\}]*)\\}" ).matcher( description );
        while ( matcher.find() )
        {
            String tag = matcher.group( 1 );
            String text = matcher.group( 2 );
            text = StringUtils.replace( text, "&", "&amp;" );
            text = StringUtils.replace( text, "<", "&lt;" );
            text = StringUtils.replace( text, ">", "&gt;" );
            if ( "code".equals( tag ) )
            {
                text = "<code>" + text + "</code>";
            }
            else if ( "link".equals( tag ) || "linkplain".equals( tag ) || "value".equals( tag ) )
            {
                String pattern = "(([^#\\.\\s]+\\.)*([^#\\.\\s]+))?" + "(#([^\\(\\s]*)(\\([^\\)]*\\))?\\s*(\\S.*)?)?";
                final int LABEL = 7;
                final int CLASS = 3;
                final int MEMBER = 5;
                final int ARGS = 6;
                Matcher link = Pattern.compile( pattern ).matcher( text );
                if ( link.matches() )
                {
                    text = link.group( LABEL );
                    if ( StringUtils.isEmpty( text ) )
                    {
                        text = link.group( CLASS );
                        if ( StringUtils.isEmpty( text ) )
                        {
                            text = "";
                        }
                        if ( StringUtils.isNotEmpty( link.group( MEMBER ) ) )
                        {
                            if ( StringUtils.isNotEmpty( text ) )
                            {
                                text += '.';
                            }
                            text += link.group( MEMBER );
                            if ( StringUtils.isNotEmpty( link.group( ARGS ) ) )
                            {
                                text += "()";
                            }
                        }
                    }
                }
                if ( !"linkplain".equals( tag ) )
                {
                    text = "<code>" + text + "</code>";
                }
            }
            matcher.appendReplacement( decoded, ( text != null ) ? quoteReplacement( text ) : "" );
        }
        matcher.appendTail( decoded );

        return decoded.toString();
    }

    /**
     * Returns a literal replacement <code>String</code> for the specified <code>String</code>. This method
     * produces a <code>String</code> that will work as a literal replacement <code>s</code> in the
     * <code>appendReplacement</code> method of the {@link Matcher} class. The <code>String</code> produced will
     * match the sequence of characters in <code>s</code> treated as a literal sequence. Slashes ('\') and dollar
     * signs ('$') will be given no special meaning.
     *
     * TODO: copied from Matcher class of Java 1.5, remove once target platform can be upgraded
     * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Matcher.html">
     * http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Matcher.html</a>
     *
     * @param s The string to be literalized
     * @return A literal string replacement
     */
    private static String quoteReplacement( String s )
    {
        if ( ( s.indexOf( '\\' ) == -1 ) && ( s.indexOf( '$' ) == -1 ) )
            return s;
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < s.length(); i++ )
        {
            char c = s.charAt( i );
            if ( c == '\\' )
            {
                sb.append( '\\' );
                sb.append( '\\' );
            }
            else if ( c == '$' )
            {
                sb.append( '\\' );
                sb.append( '$' );
            }
            else
            {
                sb.append( c );
            }
        }
        return sb.toString();
    }

    /**
     * Gets the resource bundle for the specified locale.
     *
     * @param locale The locale of the currently generated report.
     * @return The resource bundle for the requested locale.
     */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "pluginxdoc", locale, getClass().getClassLoader() );
    }
}
