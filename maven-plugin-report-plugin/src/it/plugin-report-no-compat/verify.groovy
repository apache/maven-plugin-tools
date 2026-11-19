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

def goalReport = new File( basedir, 'target/site/noop-goal.html' )
assert goalReport.isFile()
assert goalReport.text.contains( 'Does nothing.' )

assert !new File( basedir, 'target/site/noop-mojo.html' ).exists()

def pluginInfo = new File( basedir, 'target/site/plugin-info.html' )
assert pluginInfo.isFile()
assert pluginInfo.text.contains( 'noop-goal.html' )
assert !pluginInfo.text.contains( 'noop-mojo.html' )
