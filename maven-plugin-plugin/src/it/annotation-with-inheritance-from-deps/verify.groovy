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

File descriptorFile = new File( basedir, "target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

File oldHelpClass = new File( basedir, "target/classes/HelpMojo.class" );
assert !oldHelpClass.exists()

File newHelpClass = new File( basedir, "target/classes/org/apache/maven/plugin/coreit/HelpMojo.class" );
assert newHelpClass.exists()

def pluginDescriptor = new XmlParser().parse( descriptorFile );

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
assert mojo.executePhase.text() == 'package'
assert mojo.executeLifecycle.text() == 'my-lifecycle'

assert mojo.configuration.bar[0].text() == '${thebar}'
assert mojo.configuration.bar[0].'@implementation' == 'java.lang.String'
assert mojo.configuration.bar[0].'@default-value' == 'coolbar'

assert mojo.configuration.beer[0].text() == '${thebeer}'
assert mojo.configuration.beer[0].'@implementation' == 'java.lang.String'
assert mojo.configuration.beer[0].'@default-value' == 'coolbeer'

assert mojo.requirements.requirement.size() == 3

assert mojo.requirements.requirement[1].role.text() == 'org.codehaus.plexus.compiler.manager.CompilerManager'
assert mojo.requirements.requirement[1].'role-hint'.text() == ''
assert mojo.requirements.requirement[1].'field-name'.text() == 'compilerManager'

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

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "beer"}[0]

assert parameter.name.text() == 'beer'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.lang.String'
assert parameter.deprecated.text() == "wine is better"
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == 'beer for non french folks'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "bar"}[0]

assert parameter.name.text() == 'bar'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.lang.String'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == 'the cool bar to go'

return true;
