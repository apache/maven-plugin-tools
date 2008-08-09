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

import java.util.Map;

import org.apache.maven.tools.plugin.extractor.java.JavaMojoAnnotation;

import com.sun.tools.doclets.Taglet;

/**
 * The <tt>@parameter</tt> tag is used to define a Mojo parameter and has annotation parameter.
 * <br/>
 * The following is a sample declaration:
 * <pre>
 * public class MyMojo extends AbstractMojo
 * {
 *   &#x2f;&#x2a;&#x2a;
 *   &#x20;&#x2a; Dummy parameter.
 *   &#x20;&#x2a;
 *   &#x20;&#x2a; &#64;parameter &lt;alias="..."&gt; &lt;default-value="..."&gt; &lt;expression="..."&gt;
 *   &#x20;&#x2a; &lt;implementation="..."&gt; &lt;property="..."&gt;
 *   &#x20;&#x2a; ...
 *   &#x20;&#x2a;&#x2f;
 *   private Object parameterName;
 * }
 * </pre>
 * To use it, calling the <code>Javadoc</code> tool with the following:
 * <pre>
 * javadoc ... -taglet 'org.apache.maven.tools.plugin.javadoc.MojoParameterFieldTaglet'
 * </pre>
 *
 * @see <a href="package-summary.html#package_description">package-summary.html</a>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class MojoParameterFieldTaglet
    extends AbstractMojoFieldTaglet
{
    /** The Javadoc annotation */
    private static final String NAME = JavaMojoAnnotation.PARAMETER;

    private static final String[] PARAMETERS_NAME = {
        JavaMojoAnnotation.PARAMETER_ALIAS,
        JavaMojoAnnotation.PARAMETER_DEFAULT_VALUE,
        JavaMojoAnnotation.PARAMETER_EXPRESSION,
        JavaMojoAnnotation.PARAMETER_IMPLEMENTATION,
        JavaMojoAnnotation.PARAMETER_PROPERTY };

    /** The Javadoc text which will be added to the generated page. */
    protected static final String HEADER = "Is defined by";

    /**
     * @return By default, return the string defined in {@linkplain #HEADER}.
     * @see org.apache.maven.tools.plugin.javadoc.AbstractMojoTaglet#getHeader()
     * @see #HEADER
     */
    public String getHeader()
    {
        return HEADER;
    }

    /**
     * @return <code>null</code> since <code>@parameter</code> has no value.
     * @see org.apache.maven.tools.plugin.javadoc.AbstractMojoTaglet#getAllowedValue()
     */
    public String getAllowedValue()
    {
        return null;
    }

    /**
     * @return <code>MojoParameterFieldTaglet#PARAMETERS_NAME</code> since <code>@parameter</code> has parameters.
     * @see org.apache.maven.tools.plugin.javadoc.AbstractMojoTaglet#getAllowedParameterNames()
     */
    public String[] getAllowedParameterNames()
    {
        return PARAMETERS_NAME;
    }

    /**
     * @return By default, return the name of this taglet.
     * @see com.sun.tools.doclets.Taglet#getName()
     * @see MojoParameterFieldTaglet#NAME
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * Register this Taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register( Map tagletMap )
    {
        MojoParameterFieldTaglet tag = new MojoParameterFieldTaglet();
        Taglet t = (Taglet) tagletMap.get( tag.getName() );
        if ( t != null )
        {
            tagletMap.remove( tag.getName() );
        }
        tagletMap.put( tag.getName(), tag );
    }
}
