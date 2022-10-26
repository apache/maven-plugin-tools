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

assert new File( basedir, 'target/site/noop-mojo.html' ).isFile()
assert new File( basedir, 'target/site/report-mojo.html' ).isFile()

def pluginInfo = new File( basedir, 'target/site/plugin-info.html' )
assert pluginInfo.isFile()

assert !pluginInfo.text.contains('Memory')
assert !pluginInfo.text.contains('Disk Space')
// missing prerequisites in pom
assert pluginInfo.text.contains('No minimum requirement.')
