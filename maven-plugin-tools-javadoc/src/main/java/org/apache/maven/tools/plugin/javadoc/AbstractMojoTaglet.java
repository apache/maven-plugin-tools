package org.apache.maven.tools.plugin.javadoc;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * Abstract <code>Taglet</code> for <a href="http://maven.codehaus.org/"/>Maven</a> Mojo annotations.
 * <br/>
 * A Mojo annotation is defined like the following:
 * <pre>
 * &#64;annotation &lt;annotationValue&gt; &lt;parameterName="parameterValue"&gt;
 * </pre>
 *
 * @see <a href="package-summary.html#package_description">package-summary.html</a>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractMojoTaglet
    implements Taglet
{
    /** {@inheritDoc} */
    public String toString( Tag tag )
    {
        if ( tag == null )
        {
            return null;
        }

        String tagValue = getTagValue( tag );
        MutableAttributeSet tagAttributes = getTagAttributes( tag );

        StringBuffer sb = new StringBuffer();

        appendTag( sb, tag, tagAttributes, tagValue );

        return sb.toString();
    }

    /** {@inheritDoc} */
    public String toString( Tag[] tags )
    {
        if ( tags.length == 0 )
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < tags.length; i++ )
        {
            String tagValue = getTagValue( tags[i] );
            MutableAttributeSet tagAttributes = getTagAttributes( tags[i] );

            appendTag( sb, tags[i], tagAttributes, tagValue );
        }

        return sb.toString();
    }

    /**
     * @return the header, i.e. the message, to display
     */
    public abstract String getHeader();

    /**
     * @return the given annotation value, or <code>null</code> if the given Mojo annotation/tag does't allow
     * annotation value.
     * <br/>
     * <b>Note</b>: the value could be a pattern value, i.e.: <code>*</code> for every values, <code>a|b|c</code>
     * for <code>a OR b OR c</code>.
     */
    public abstract String getAllowedValue();

    /**
     * @return an array of the allowed parameter names for the given Mojo annotation/tag, or <code>null</code>
     * if the annotation/tag doesn't allow parameter.
     */
    public abstract String[] getAllowedParameterNames();

    /**
     * @return <code>true</code> if taglet has annotation value, <code>false</code> otherwise.
     * @see #getAllowedValue()
     */
    public boolean hasAnnotationValue()
    {
        return getAllowedValue() != null;
    }

    /**
     * @return <code>true</code> if taglet has parameters, <code>false</code> otherwise.
     * @see #getAllowedParameterNames()
     */
    public boolean hasAnnotationParameters()
    {
        return getAllowedParameterNames() != null;
    }

    /**
     * @param tag not null.
     * @return a not null String or <code>null</code> if no annotation value was found.
     */
    private String getTagValue( Tag tag )
    {
        if ( tag == null )
        {
            throw new IllegalArgumentException( "tag should be not null" );
        }

        String text = tag.text();
        if ( isEmpty( text ) )
        {
            // using pattern: @annotation
            return null;
        }

        String tagValue = null;
        StringTokenizer token = new StringTokenizer( text, " " );
        while ( token.hasMoreTokens() )
        {
            String nextToken = token.nextToken();

            if ( nextToken.indexOf( "=" ) == -1 )
            {
                // using pattern: @annotation <annotationValue>
                tagValue = nextToken;
            }
        }

        return tagValue;
    }

    /**
     * @param tag not null.
     * @return a not null MutableAttributeSet.
     */
    private MutableAttributeSet getTagAttributes( Tag tag )
    {
        if ( tag == null )
        {
            throw new IllegalArgumentException( "tag should be not null" );
        }

        String text = tag.text();

        StringTokenizer token = new StringTokenizer( text, " " );
        MutableAttributeSet tagAttributes = new SimpleAttributeSet();
        while ( token.hasMoreTokens() )
        {
            String nextToken = token.nextToken();

            if ( nextToken.indexOf( "=" ) == -1 )
            {
                // using pattern: @annotation <annotationValue>
                continue;
            }

            StringTokenizer token2 = new StringTokenizer( nextToken, "=" );
            if ( token2.countTokens() != 2 )
            {
                System.err.println( "The annotation '" + tag.name() + "' has no name/value pairs parameter: "
                    + tag.name() + " " + text + " (" + tag.position().file() + ":" + tag.position().line() + ":"
                    + tag.position().column() + ")" );
                tagAttributes.addAttribute( token2.nextToken(), "" );
                continue;
            }

            String name = token2.nextToken();
            String value = token2.nextToken().replaceAll( "\"", "" );

            if ( getAllowedParameterNames() != null && !Arrays.asList( getAllowedParameterNames() ).contains( name ) )
            {
                System.err.println( "The annotation '" + tag.name() + "' has wrong parameter name: " + tag.name() + " "
                    + text + " (" + tag.position().file() + ":" + tag.position().line() + ":" + tag.position().column()
                    + ")" );
            }

            tagAttributes.addAttribute( name, value );
        }

        return tagAttributes;
    }

    /**
     * Append a tag
     *
     * @param sb not null
     * @param tag not null
     * @param tagAttributes not null
     * @param tagValue not null
     */
    private void appendTag( StringBuffer sb, Tag tag, MutableAttributeSet tagAttributes, String tagValue )
    {
        if ( !hasAnnotationParameters() )
        {
            if ( tagAttributes.getAttributeCount() > 0 )
            {
                System.err.println( "The annotation '@" + getName() + "' should have no attribute ("
                    + tag.position().file() + ":" + tag.position().line() + ":" + tag.position().column() + ")" );
            }

            if ( hasAnnotationValue() )
            {
                sb.append( "<DT><B>" ).append( getHeader() ).append( ":</B></DT>" );
                if ( isEveryValues( getAllowedValue() ) )
                {
                    if ( isNotEmpty( tagValue ) )
                    {
                        sb.append( "<DD>" ).append( tagValue ).append( "</DD>" );
                    }
                    else
                    {
                        System.err.println( "The annotation '@" + getName() + "' is specified to have a value but "
                            + "no value is defined (" + tag.position().file() + ":" + tag.position().line() + ":"
                            + tag.position().column() + ")" );
                        sb.append( "<DD>" ).append( "NOT DEFINED" ).append( "</DD>" );
                    }
                }
                else
                {
                    List<String> l = getOnlyValues( getAllowedValue() );
                    if ( isNotEmpty( tagValue ) )
                    {
                        if ( l.contains( tagValue ) )
                        {
                            sb.append( "<DD>" ).append( tagValue ).append( "</DD>" );
                        }
                        else
                        {
                            System.err.println( "The annotation '@" + getName() + "' is specified to be a value of "
                                + l + " (" + tag.position().file() + ":" + tag.position().line() + ":"
                                + tag.position().column() + ")" );
                            sb.append( "<DD>" ).append( tagValue ).append( "</DD>" );
                        }
                    }
                    else
                    {
                        sb.append( "<DD>" ).append( l.get( 0 ) ).append( "</DD>" );
                    }
                }
            }
            else
            {
                if ( isNotEmpty( tagValue ) )
                {
                    System.err.println( "The annotation '@" + getName() + "' should have no value ("
                        + tag.position().file() + ":" + tag.position().line() + ":" + tag.position().column() + ")" );
                }
                sb.append( "<DT><B>" ).append( getHeader() ).append( "</B></DT>" );
                sb.append( "<DD></DD>" );
            }
        }
        else
        {
            if ( hasAnnotationValue() )
            {
                sb.append( "<DT><B>" ).append( getHeader() ).append( ":</B></DT>" );
                if ( isEveryValues( getAllowedValue() ) )
                {
                    if ( isNotEmpty( tagValue ) )
                    {
                        sb.append( "<DD>" ).append( tagValue );
                    }
                    else
                    {
                        System.err.println( "The annotation '@" + getName() + "' is specified to have a value but "
                            + "no value is defined (" + tag.position().file() + ":" + tag.position().line() + ":"
                            + tag.position().column() + ")" );
                        sb.append( "<DD>" ).append( "NOT DEFINED" );
                    }
                }
                else
                {
                    List<String> l = getOnlyValues( getAllowedValue() );
                    if ( isNotEmpty( tagValue ) )
                    {
                        if ( l.contains( tagValue ) )
                        {
                            sb.append( "<DD>" ).append( tagValue );
                        }
                        else
                        {
                            System.err.println( "The annotation '@" + getName() + "' is specified to be a value in "
                                + l + " (" + tag.position().file() + ":" + tag.position().line() + ":"
                                + tag.position().column() + ")" );
                            sb.append( "<DD>" ).append( tagValue );
                        }
                    }
                    else
                    {
                        sb.append( "<DD>" ).append( l.get( 0 ) );
                    }
                }
            }
            else
            {
                if ( isNotEmpty( tagValue ) )
                {
                    System.err.println( "The annotation '@" + getName() + "' should have no value ("
                        + tag.position().file() + ":" + tag.position().line() + ":" + tag.position().column() + ")" );
                }
                sb.append( "<DT><B>" ).append( getHeader() ).append( ":</B></DT>" );
                sb.append( "<DD>" );
            }

            appendAnnotationParameters( sb, tagAttributes );
            sb.append( "</DD>" );
        }
    }

    /**
     * Append the annotation parameters as a definition list.
     *
     * @param sb not null
     * @param att not null
     */
    private static void appendAnnotationParameters( StringBuffer sb, MutableAttributeSet att )
    {
        sb.append( "<DL>" );

        Enumeration<?> names = att.getAttributeNames();
        while ( names.hasMoreElements() )
        {
            Object key = names.nextElement();
            Object value = att.getAttribute( key );

            if ( value instanceof AttributeSet )
            {
                // ignored
            }
            else
            {
                sb.append( "<DT><B>" ).append( key ).append( ":</B></DT>" );
                sb.append( "<DD>" ).append( value ).append( "</DD>" );
            }
        }

        sb.append( "</DL>" );
    }

    /**
     * @param text not null
     * @return <code>true</code> if text contains <code>*</code>, <code>false</code> otherwise.
     */
    private static boolean isEveryValues( String text )
    {
        return text.trim().equals( "*" );
    }

    /**
     * Splits the provided text into a array, using pipe as the separator.
     *
     * @param text not null
     * @return a list of parsed Strings or <code>Collections.EMPTY_LIST</code>.
     * By convention, the default value is the first element.
     */
    private static List<String> getOnlyValues( String text )
    {
        if ( text.indexOf( "|" ) == -1 )
        {
            return Collections.emptyList();
        }

        List<String> l = new ArrayList<String>();
        StringTokenizer token = new StringTokenizer( text, "|" );
        while ( token.hasMoreTokens() )
        {
            l.add( token.nextToken() );
        }

        return l;
    }

    /**
     * <p>Checks if a String is non <code>null</code> and is
     * not empty (<code>length > 0</code>).</p>
     *
     * @param str the String to check
     * @return true if the String is non-null, and not length zero
     */
    private static boolean isNotEmpty( String str )
    {
        return ( str != null && str.length() > 0 );
    }

    /**
     * <p>Checks if a (trimmed) String is <code>null</code> or empty.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if the String is <code>null</code>, or
     *  length zero once trimmed
     */
    private static boolean isEmpty( String str )
    {
        return ( str == null || str.trim().length() == 0 );
    }
}
