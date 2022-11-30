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

def pluginDescriptor = new XmlParser().parse( descriptorFile );

assert pluginDescriptor.requiredJavaVersion.text() == '[1.8,)'
assert pluginDescriptor.requiredMavenVersion.text() == '4.0.0-alpha-2'

def mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "first" }[0]

assert mojo.goal.text() == 'first'
assert mojo.implementation.text() == 'org.apache.maven.its.v4api.FirstMojo'
assert mojo.language.text() == 'java'
assert mojo.description.text().startsWith('Test mojo for the v4 api plugin descriptor generation.')
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
assert mojo.v4Api.text() == 'true'

assert mojo.configuration.basedir[0].text() == ''
assert mojo.configuration.basedir[0].'@implementation' == 'java.nio.file.Path'
assert mojo.configuration.basedir[0].'@default-value' == '${basedir}'

assert mojo.configuration.touchFile[0].text() == '${first.touchFile}'
assert mojo.configuration.touchFile[0].'@implementation' == 'java.nio.file.Path'
assert mojo.configuration.touchFile[0].'@default-value' == '${project.build.directory}/touch.txt'

assert mojo.configuration.session[0].text() == ''
assert mojo.configuration.session[0].'@implementation' == 'org.apache.maven.api.Session'
assert mojo.configuration.session[0].'@default-value' == '${session}'

assert mojo.configuration.project[0].text() == ''
assert mojo.configuration.project[0].'@implementation' == 'org.apache.maven.api.Project'
assert mojo.configuration.project[0].'@default-value' == '${project}'

assert mojo.configuration.mojo[0].text() == ''
assert mojo.configuration.mojo[0].'@implementation' == 'org.apache.maven.api.MojoExecution'
assert mojo.configuration.mojo[0].'@default-value' == '${mojoExecution}'

assert mojo.configuration.settings[0].text() == ''
assert mojo.configuration.settings[0].'@implementation' == 'org.apache.maven.api.settings.Settings'
assert mojo.configuration.settings[0].'@default-value' == '${settings}'

assert mojo.requirements.requirement.size() == 2

assert mojo.requirements.requirement[0].role.text() == 'org.apache.maven.api.services.ArtifactInstaller'
assert mojo.requirements.requirement[0].'role-hint'.text() == 'test'
assert mojo.requirements.requirement[0].'field-name'.text() == 'custom'

assert mojo.requirements.requirement[1].role.text() == 'org.apache.maven.api.plugin.Log'
assert mojo.requirements.requirement[1].'role-hint'.isEmpty()
assert mojo.requirements.requirement[1].'field-name'.text() == 'log'

assert mojo.parameters.parameter.size() == 7

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "basedir" }[0]
assert parameter.name.text() == 'basedir'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.nio.file.Path'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == 'Project directory.'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "touchFile" }[0]
assert parameter.name.text() == 'touchFile'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.nio.file.Path'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "mojo" }[0]
assert parameter.name.text() == 'mojo'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.api.MojoExecution'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "namedParam" }[0]
assert parameter.name.text() == 'namedParam'
assert parameter.alias.text() == 'alias'
assert parameter.type.text() == 'java.lang.String'
assert parameter.since.text() == '0.1'
assert parameter.deprecated.text() == 'As of 0.2'
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "session" }[0]
assert parameter.name.text() == 'session'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.api.Session'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "project" }[0]
assert parameter.name.text() == 'project'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.api.Project'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "mojo" }[0]
assert parameter.name.text() == 'mojo'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.api.MojoExecution'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "settings" }[0]
assert parameter.name.text() == 'settings'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.api.settings.Settings'
assert parameter.since.isEmpty()
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

// check help mojo source and class
assert new File( basedir, "target/classes/org/apache/maven/its/v4api/HelpMojo.class" ).isFile()
assert new File( basedir, "target/generated-sources/plugin/org/apache/maven/its/v4api/HelpMojo.java" ).isFile()

return true;
