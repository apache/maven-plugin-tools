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

/**
 * Abstract <code>Taglet</code> for annotations specified at the Mojo parameter level.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractMojoFieldTaglet
    extends AbstractMojoTaglet
{
    /**
     * @return <code>false</code> since this annotation can <b>NOT</b> be used in constructor documentation.
     * @see com.sun.tools.doclets.Taglet#inConstructor()
     */
    public final boolean inConstructor()
    {
        return false;
    }

    /**
     * @return <code>true</code> since this annotation can <b>NOT</b> be used in field documentation.
     * @see com.sun.tools.doclets.Taglet#inField()
     */
    public final boolean inField()
    {
        return true;
    }

    /**
     * @return <code>false</code> since this annotation can <b>NOT</b> be used in method documentation.
     * @see com.sun.tools.doclets.Taglet#inMethod()
     */
    public final boolean inMethod()
    {
        return false;
    }

    /**
     * @return <code>false</code> since this annotation can <b>NOT</b> be used in overview documentation.
     * @see com.sun.tools.doclets.Taglet#inOverview()
     */
    public final boolean inOverview()
    {
        return false;
    }

    /**
     * @return <code>false</code> since this annotation can <b>NOT</b> be used in package documentation.
     * @see com.sun.tools.doclets.Taglet#inPackage()
     */
    public final boolean inPackage()
    {
        return false;
    }

    /**
     * @return <code>false</code> since this annotation can be used in type documentation.
     * @see com.sun.tools.doclets.Taglet#inType()
     */
    public final boolean inType()
    {
        return false;
    }

    /**
     * @return <code>false</code> since this annotation can <b>NOT</b> be used in inline tag.
     * @see com.sun.tools.doclets.Taglet#isInlineTag()
     */
    public final boolean isInlineTag()
    {
        return false;
    }
}
