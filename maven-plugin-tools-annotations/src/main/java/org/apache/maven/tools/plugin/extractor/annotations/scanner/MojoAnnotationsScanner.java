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

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.tools.plugin.extractor.ExtractionException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public interface MojoAnnotationsScanner
{
    String ROLE = MojoAnnotationsScanner.class.getName();

    List<String> CLASS_LEVEL_ANNOTATIONS = Arrays.asList( Mojo.class.getName(), Execute.class.getName() );

    List<String> FIELD_LEVEL_ANNOTATIONS = Arrays.asList( Parameter.class.getName(), Component.class.getName() );

    /**
     * Scan classes for mojo annotations.
     * 
     * @param request
     * @return map of mojo-annotated classes keyed by full class name
     * @throws ExtractionException
     */
    Map<String, MojoAnnotatedClass> scan( MojoAnnotationsScannerRequest request )
        throws ExtractionException;
}
