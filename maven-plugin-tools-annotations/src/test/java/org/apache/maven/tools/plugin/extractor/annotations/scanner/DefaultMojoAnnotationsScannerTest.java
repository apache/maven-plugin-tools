package org.apache.maven.tools.plugin.extractor.annotations.scanner;

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

import static org.mockito.Mockito.mock;

import java.io.File;

import org.codehaus.plexus.logging.Logger;
import org.junit.Test;

public class DefaultMojoAnnotationsScannerTest
{
    private DefaultMojoAnnotationsScanner scanner = new DefaultMojoAnnotationsScanner();

    @Test
    public void testSkipModuleInfoClassInArchive() throws Exception
    {
        scanner.scanArchive( new File( "target/test-classes/java9-module.jar"), null, false );
    }
    
    @Test
    public void testJava8Annotations() throws Exception
    {
        scanner.enableLogging( mock( Logger.class ) );
        scanner.scanArchive( new File( "target/test-classes/java8-annotations.jar"), null, false );
    }

}
