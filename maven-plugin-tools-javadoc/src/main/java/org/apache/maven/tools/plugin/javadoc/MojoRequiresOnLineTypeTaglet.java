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
 * The <tt>@requiresOnline</tt> tag is used to specify that this Mojo should be online
 * and has parameter.
 * <br/>
 * The following is a sample declaration:
 * <pre>
 * &#x2f;&#x2a;&#x2a;
 * &#x20;&#x2a; Dummy Mojo.
 * &#x20;&#x2a;
 * &#x20;&#x2a; &#64;requiresOnline &lt;true|false&gt;
 * &#x20;&#x2a; ...
 * &#x20;&#x2a;&#x2f;
 * public class MyMojo extends AbstractMojo{}
 * </pre>
 * To use it, calling the <code>Javadoc</code> tool with the following:
 * <pre>
 * javadoc ... -taglet 'org.apache.maven.tools.plugin.javadoc.MojoRequiresOnLineTypeTaglet'
 * </pre>
 * <b>Note</b>: This taglet is similar to call the <code>Javadoc</code> tool with the following:
 * <pre>
 * javadoc ... -tag 'requiresOnline:t:Requires to be online to run'
 * </pre>
 *
 * @see <a href="package-summary.html#package_description">package-summary.html</a>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class MojoRequiresOnLineTypeTaglet
    extends AbstractMojoTypeTaglet
{
    /** The Javadoc annotation */
    private static final String NAME = JavaMojoAnnotation.REQUIRES_ONLINE;

    /** The Javadoc text which will be added to the generated page. */
    protected static final String HEADER = "Requires to be online to run";

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
     * @return <code>true|false</code> since <code>@requiresOnline</code> has value.
     * @see org.apache.maven.tools.plugin.javadoc.AbstractMojoTaglet#getAllowedValue()
     */
    public String getAllowedValue()
    {
        return "true|false";
    }

    /**
     * @return <code>null</code> since <code>@requiresOnline</code> has no parameter.
     * @see org.apache.maven.tools.plugin.javadoc.AbstractMojoTaglet#getAllowedParameterNames()
     */
    public String[] getAllowedParameterNames()
    {
        return null;
    }

    /**
     * @return By default, return the name of this taglet.
     * @see com.sun.tools.doclets.Taglet#getName()
     * @see MojoRequiresOnLineTypeTaglet#NAME
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
        MojoRequiresOnLineTypeTaglet tag = new MojoRequiresOnLineTypeTaglet();
        Taglet t = (Taglet) tagletMap.get( tag.getName() );
        if ( t != null )
        {
            tagletMap.remove( tag.getName() );
        }
        tagletMap.put( tag.getName(), tag );
    }
}
