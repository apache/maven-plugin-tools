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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.w3c.tidy.Tidy;

/**
 * Convenience methods to play with Maven plugins.
 *
 * @author jdcasey
 */
public final class GeneratorUtils
{
    private GeneratorUtils()
    {
        // nop
    }

    /**
     * @param w not null writer
     * @param pluginDescriptor not null
     */
    public static void writeDependencies( XMLWriter w, PluginDescriptor pluginDescriptor )
    {
        w.startElement( "dependencies" );

        @SuppressWarnings( "unchecked" )
        List<ComponentDependency> deps = pluginDescriptor.getDependencies();
        for ( ComponentDependency dep : deps )
        {
            w.startElement( "dependency" );

            element( w, "groupId", dep.getGroupId() );

            element( w, "artifactId", dep.getArtifactId() );

            element( w, "type", dep.getType() );

            element( w, "version", dep.getVersion() );

            w.endElement();
        }

        w.endElement();
    }

    /**
     * @param w not null writer
     * @param name  not null
     * @param value could be null
     */
    public static void element( XMLWriter w, String name, String value )
    {
        w.startElement( name );

        if ( value == null )
        {
            value = "";
        }

        w.writeText( value );

        w.endElement();
    }

    public static void element( XMLWriter w, String name, String value, boolean asText )
    {
        element( w, name, asText ? GeneratorUtils.toText( value ) : value );
    }
    
    /**
     * @param dependencies not null list of <code>Dependency</code>
     * @return list of component dependencies
     */
    public static List<ComponentDependency> toComponentDependencies( List<Dependency> dependencies )
    {
        List<ComponentDependency> componentDeps = new LinkedList<>();

        for ( Dependency dependency : dependencies )
        {
            ComponentDependency cd = new ComponentDependency();

            cd.setArtifactId( dependency.getArtifactId() );
            cd.setGroupId( dependency.getGroupId() );
            cd.setVersion( dependency.getVersion() );
            cd.setType( dependency.getType() );

            componentDeps.add( cd );
        }

        return componentDeps;
    }

    /**
     * Returns a literal replacement <code>String</code> for the specified <code>String</code>. This method
     * produces a <code>String</code> that will work as a literal replacement <code>s</code> in the
     * <code>appendReplacement</code> method of the {@link Matcher} class. The <code>String</code> produced will
     * match the sequence of characters in <code>s</code> treated as a literal sequence. Slashes ('\') and dollar
     * signs ('$') will be given no special meaning. TODO: copied from Matcher class of Java 1.5, remove once target
     * platform can be upgraded
     *
     * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Matcher.html">java.util.regex.Matcher</a>
     * @param s The string to be literalized
     * @return A literal string replacement
     */
    private static String quoteReplacement( String s )
    {
        if ( ( s.indexOf( '\\' ) == -1 ) && ( s.indexOf( '$' ) == -1 ) )
        {
            return s;
        }

        StringBuilder sb = new StringBuilder();
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
     * Decodes javadoc inline tags into equivalent HTML tags. For instance, the inline tag "{@code <A&B>}" should be
     * rendered as "<code>&lt;A&amp;B&gt;</code>".
     *
     * @param description The javadoc description to decode, may be <code>null</code>.
     * @return The decoded description, never <code>null</code>.
     */
    static String decodeJavadocTags( String description )
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
                final int label = 7;
                final int clazz = 3;
                final int member = 5;
                final int args = 6;
                Matcher link = Pattern.compile( pattern ).matcher( text );
                if ( link.matches() )
                {
                    text = link.group( label );
                    if ( StringUtils.isEmpty( text ) )
                    {
                        text = link.group( clazz );
                        if ( StringUtils.isEmpty( text ) )
                        {
                            text = "";
                        }
                        if ( StringUtils.isNotEmpty( link.group( member ) ) )
                        {
                            if ( StringUtils.isNotEmpty( text ) )
                            {
                                text += '.';
                            }
                            text += link.group( member );
                            if ( StringUtils.isNotEmpty( link.group( args ) ) )
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
     * Fixes some javadoc comment to become a valid XHTML snippet.
     *
     * @param description Javadoc description with HTML tags, may be <code>null</code>.
     * @return The description with valid XHTML tags, never <code>null</code>.
     */
    public static String makeHtmlValid( String description )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return "";
        }

        String commentCleaned = decodeJavadocTags( description );

        // Using jTidy to clean comment
        Tidy tidy = new Tidy();
        tidy.setDocType( "loose" );
        tidy.setXHTML( true );
        tidy.setXmlOut( true );
        tidy.setInputEncoding( "UTF-8" );
        tidy.setOutputEncoding( "UTF-8" );
        tidy.setMakeClean( true );
        tidy.setNumEntities( true );
        tidy.setQuoteNbsp( false );
        tidy.setQuiet( true );
        tidy.setShowWarnings( false );
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream( commentCleaned.length() + 256 );
            tidy.parse( new ByteArrayInputStream( commentCleaned.getBytes( UTF_8 ) ), out );
            commentCleaned = out.toString( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e )
        {
            // cannot happen as every JVM must support UTF-8, see also class javadoc for java.nio.charset.Charset
        }

        if ( StringUtils.isEmpty( commentCleaned ) )
        {
            return "";
        }

        // strip the header/body stuff
        String ls = System.getProperty( "line.separator" );
        int startPos = commentCleaned.indexOf( "<body>" + ls ) + 6 + ls.length();
        int endPos = commentCleaned.indexOf( ls + "</body>" );
        commentCleaned = commentCleaned.substring( startPos, endPos );

        return commentCleaned;
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
     * @since 2.4.3
     */
    public static String toText( String html )
    {
        if ( StringUtils.isEmpty( html ) )
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();

        HTMLEditorKit.Parser parser = new ParserDelegator();
        HTMLEditorKit.ParserCallback htmlCallback = new MojoParserCallback( sb );

        try
        {
            parser.parse( new StringReader( makeHtmlValid( html ) ), htmlCallback, true );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        return sb.toString().replace( '\"', '\'' ); // for CDATA
    }

    /**
     * ParserCallback implementation.
     */
    private static class MojoParserCallback
        extends HTMLEditorKit.ParserCallback
    {
        /**
         * Holds the index of the current item in a numbered list.
         */
        class Counter
        {
            int value;
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
        private Stack<Counter> numbering = new Stack<>();

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

        /**
         * The current buffer.
         */
        private final StringBuilder sb;

        /**
         * @param sb not null
         */
        MojoParserCallback( StringBuilder sb )
        {
            this.sb = sb;
        }

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
                Counter counter = numbering.peek();
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
                    pendingNewline = false;
                    newline( pendingNewline );
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
                text = data;
            }
            else
            {
                text = data.replace( '\n', ' ' );
            }
            sb.append( text );
        }
    }

    /**
     * Find the best package name, based on the number of hits of actual Mojo classes.
     *
     * @param pluginDescriptor not null
     * @return the best name of the package for the generated mojo
     */
    public static String discoverPackageName( PluginDescriptor pluginDescriptor )
    {
        Map<String, Integer> packageNames = new HashMap<>();

        List<MojoDescriptor> mojoDescriptors = pluginDescriptor.getMojos();
        if ( mojoDescriptors == null )
        {
            return "";
        }
        for ( MojoDescriptor descriptor : mojoDescriptors )
        {

            String impl = descriptor.getImplementation();
            if ( StringUtils.equals( descriptor.getGoal(), "help" ) && StringUtils.equals( "HelpMojo", impl ) )
            {
                continue;
            }
            if ( impl.lastIndexOf( '.' ) != -1 )
            {
                String name = impl.substring( 0, impl.lastIndexOf( '.' ) );
                if ( packageNames.get( name ) != null )
                {
                    int next = ( packageNames.get( name ) ).intValue() + 1;
                    packageNames.put( name,  Integer.valueOf( next ) );
                }
                else
                {
                    packageNames.put( name, Integer.valueOf( 1 ) );
                }
            }
            else
            {
                packageNames.put( "", Integer.valueOf( 1 ) );
            }
        }

        String packageName = "";
        int max = 0;
        for ( Map.Entry<String, Integer> entry : packageNames.entrySet() )
        {
            int value = entry.getValue().intValue();
            if ( value > max )
            {
                max = value;
                packageName = entry.getKey();
            }
        }

        return packageName;
    }

    /**
     * @param impl a Mojo implementation, not null
     * @param project a MavenProject instance, could be null
     * @return <code>true</code> is the Mojo implementation implements <code>MavenReport</code>,
     * <code>false</code> otherwise.
     * @throws IllegalArgumentException if any
     */
    @SuppressWarnings( "unchecked" )
    public static boolean isMavenReport( String impl, MavenProject project )
        throws IllegalArgumentException
    {
        if ( impl == null )
        {
            throw new IllegalArgumentException( "mojo implementation should be declared" );
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if ( project != null )
        {
            List<String> classPathStrings;
            try
            {
                classPathStrings = project.getCompileClasspathElements();
                if ( project.getExecutionProject() != null )
                {
                    classPathStrings.addAll( project.getExecutionProject().getCompileClasspathElements() );
                }
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new IllegalArgumentException( e );
            }

            List<URL> urls = new ArrayList<>( classPathStrings.size() );
            for ( String classPathString : classPathStrings )
            {
                try
                {
                    urls.add( new File( classPathString ).toURL() );
                }
                catch ( MalformedURLException e )
                {
                    throw new IllegalArgumentException( e );
                }
            }

            classLoader = new URLClassLoader( urls.toArray( new URL[urls.size()] ), classLoader );
        }

        try
        {
            Class<?> clazz = Class.forName( impl, false, classLoader );

            return MavenReport.class.isAssignableFrom( clazz );
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }

}
