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

import groovy.xml.XmlParser

File descriptorFile = new File( basedir, "target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

def pluginDescriptor = new XmlParser().parse( descriptorFile );

assert pluginDescriptor.requiredJavaVersion.text() == '17'
assert pluginDescriptor.requiredMavenVersion.text() == '4.0.0-rc-3'

def mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "first" }[0]

assert mojo.goal.text() == 'first'
assert mojo.implementation.text() == 'org.apache.maven.its.v4api.FirstMojo'
assert mojo.language.text() == 'java'
assert mojo.description.text().startsWith('Test mojo for the v4 api plugin descriptor generation.')
assert mojo.projectRequired.text() == 'true'
assert mojo.onlineRequired.text() == 'false'
assert mojo.aggregator.text() == 'false'
assert mojo.phase.text() == 'integration-test'

assert mojo.parameters.parameter.size() == 3

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "basedir" }[0]
assert parameter.name.text() == 'basedir'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.nio.file.Path'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == 'Project directory.'
assert parameter.defaultValue.text() == '${basedir}'
assert parameter.expression.isEmpty()

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "touchFile" }[0]
assert parameter.name.text() == 'touchFile'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.nio.file.Path'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''
assert parameter.defaultValue.text() == '${project.build.directory}/touch.txt'
assert parameter.expression.text() == '${first.touchFile}'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "namedParam" }[0]
assert parameter.name.text() == 'namedParam'
assert parameter.alias.text() == 'alias'
assert parameter.type.text() == 'java.lang.String'
assert parameter.since.text() == '0.1'
assert parameter.deprecated.text() == 'As of 0.2'
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''
assert parameter.defaultValue.isEmpty()
assert parameter.expression.isEmpty()

// check help mojo source and class
assert new File( basedir, "target/classes/org/apache/maven/its/v4api/HelpMojo.class" ).isFile()
assert new File( basedir, "target/generated-sources/plugin/org/apache/maven/its/v4api/HelpMojo.java" ).isFile()

return true;
