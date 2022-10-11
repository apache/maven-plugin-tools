package test;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo-Description with some non-ASCII characters: €àáâãäåæòóôõöø
 * <p>
 * This file is encoded with UTF-8 and Maven is configured with UTF-8 for source
 * files and UTF-8 as output encoding for the site.  Therefore we expect the
 * generated site to contain all characters from above, even if the system
 * encoding is not UTF-8.
 */
@Mojo( name= "test" )
public class MyMojo
    extends AbstractMojo
{

    /**
     * Parameter-Description with some non-ASCII characters: ÈÉÊË€
     */
    @Parameter
    private String testParam;


    public void execute()
    {
    }

}
