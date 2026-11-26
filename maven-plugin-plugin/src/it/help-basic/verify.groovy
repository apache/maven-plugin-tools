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

// each module documents the same mojo; they differ in how the deprecations are declared
for ( String module in [ "help", "help-deprecated-annotation-only" ] )
{
    def expected = new File( basedir, "${module}/expected-help.txt" ).text.trim().replace( "\r", "" )

    def log = new File( basedir, "${module}.log" ).text.replace( "\r", "" )
    log = log.substring( log.indexOf( "[INFO] ${module} 1.0-SNAPSHOT" ) )
    log = log.substring( 0, log.indexOf( "[INFO]", 5 ) ).trim()

    assert log == expected : "unexpected help output of ${module}"
}

return true
