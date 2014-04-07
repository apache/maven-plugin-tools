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

File touchFile = new File( basedir, "module-mojo/target/touch.txt" )
assert touchFile.isFile()

File descriptorFile = new File( basedir, "module-mojo/target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

def pluginDescriptor = new XmlParser().parse( descriptorFile );

assert pluginDescriptor.mojos.mojo.size() == 3

def mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "first"}[0]

assert mojo.goal.text() == 'first'
assert mojo.implementation.text() == 'org.apache.maven.plugin.coreit.FirstMojo'
assert mojo.language.text() == 'java'
assert mojo.description.text() == 'Touches a test file.'
assert mojo.deprecated.text() == "Don't use!"
assert mojo.requiresDependencyResolution.text() == 'test'
assert mojo.requiresDependencyCollection.text() == ''
assert mojo.requiresProject.text() == 'true'
assert mojo.requiresOnline.text() == 'false'
assert mojo.requiresDirectInvocation.text() == 'false'
assert mojo.aggregator.text() == 'false'
assert mojo.threadSafe.text() == 'false'
assert mojo.phase.text() == 'integration-test'
assert mojo.executePhase.text() == 'generate-sources'
assert mojo.executeLifecycle.text() == 'cobertura'

assert mojo.configuration.basedir[0].text() == ''
assert mojo.configuration.basedir[0].'@implementation' == 'java.io.File'
assert mojo.configuration.basedir[0].'@default-value' == '${basedir}'

assert mojo.configuration.touchFile[0].text() == '${first.touchFile}'
assert mojo.configuration.touchFile[0].'@implementation' == 'java.io.File'
assert mojo.configuration.touchFile[0].'@default-value' == '${project.build.directory}/touch.txt'

assert mojo.requirements.requirement.size() == 3

assert mojo.requirements.requirement[2].role.text() == 'org.apache.maven.project.MavenProjectHelper'
//assert mojo.requirements.requirement[2].'role-hint'.text() == 'default'
assert mojo.requirements.requirement[2].'field-name'.text() == 'projectHelper'

assert mojo.parameters.parameter.size() == 3

def parameter = mojo.parameters.parameter.findAll{ it.name.text() == "aliasedParam"}[0]

assert parameter.name.text() == 'aliasedParam'
assert parameter.alias.text() == 'alias'
assert parameter.type.text() == 'java.lang.String'
assert parameter.deprecated.text() == 'As of 0.2'
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "touchFile"}[0]

assert parameter.name.text() == 'touchFile'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.io.File'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "basedir"}[0]

assert parameter.name.text() == 'basedir'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.io.File'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == 'Project directory.'

mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "second"}[0]

assert mojo.requiresDependencyCollection.text() == 'compile'
assert mojo.threadSafe.text() == 'true'

return true;
