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

import java.net.URISyntaxException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavadocReferenceTest
{

    @Test
    void testParse()
        throws URISyntaxException
    {
        assertEquals( new JavadocReference( Optional.empty(), Optional.empty(), Optional.of( "member" ), Optional.empty() ),
                      JavadocReference.parse( "#member" ) );
        assertEquals( new JavadocReference( Optional.empty(),Optional.of( "Class" ), Optional.of( "member" ), Optional.empty() ),
                      JavadocReference.parse( "Class#member" ) );
        assertEquals( new JavadocReference( Optional.empty(), Optional.of( "package.Class" ), Optional.of( "member" ), Optional.empty() ),
                      JavadocReference.parse( "package.Class#member" ) );
        assertEquals( new JavadocReference( Optional.empty(), Optional.of( "package" ), Optional.empty(), Optional.empty() ),
                      JavadocReference.parse( "package" ) );
        assertEquals( new JavadocReference( Optional.empty(), Optional.of( "package.Class" ), Optional.of( "member(ArgType1,ArgType2)" ), Optional.of("label") ),
                      JavadocReference.parse( "package.Class#member(ArgType1,ArgType2) label" ) );
        assertEquals( new JavadocReference( Optional.of("my.module"), Optional.of( "package.Class" ), Optional.of( "member(ArgType1,ArgType2)" ), Optional.of("label") ),
                      JavadocReference.parse( "my.module/package.Class#member(ArgType1,ArgType2) label" ) );
    }

}
