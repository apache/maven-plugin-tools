package org.apache.maven.tools.plugin.extractor;

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
 * Wrap errors when extraction exception occurred.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class ExtractionException
    extends Exception
{
    /** serialVersionUID */
    static final long serialVersionUID = 9074953540861573535L;

    /**
     * @param message given message
     * @param cause given cause
     */
    public ExtractionException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * @param message a given message
     */
    public ExtractionException( String message )
    {
        super( message );
    }
}
