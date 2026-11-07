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

// help.log holds the output of the second invoker run: the goal and its undocumented parameter must be listed
def log = new File( basedir, "help.log" ).text.replace( "\r", "" )

assert log.contains( "[INFO] help 1.0-SNAPSHOT" ) : "help.log has no plugin header"
assert log =~ /(?m)^prefix:test$/ : "help.log does not describe the goal prefix:test"
assert log =~ /(?s)Available parameters:\s+empyAnnotatedParameter\s/ : "help.log does not list the undocumented parameter"

return true
