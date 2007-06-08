package org.apache.maven.tools.plugin.util;

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

import junit.framework.TestCase;

import java.net.URL;

/**
 * @author jdcasey
 */
public class TestUtils
    extends TestCase
{

    public void testDirnameFunction_METATEST()
    {
        String classname = getClass().getName().replace( '.', '/' ) + ".class";
        String basedir = TestUtils.dirname( classname );

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource( classname );

        assertEquals( resource.getPath(), basedir + classname );
    }

    public static String dirname( String file )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL fileResource = cl.getResource( file );

        String fullPath = fileResource.getPath();

        return fullPath.substring( 0, fullPath.length() - file.length() );
    }

}