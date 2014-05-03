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

File touchFile = new File( basedir, "target/touch.txt" )
assert touchFile.isFile()

File descriptorFile = new File( basedir, "target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

def pluginDescriptor = new XmlParser().parse( descriptorFile );

def mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "first" }[0]

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

assert mojo.configuration.session[0].text() == ''
assert mojo.configuration.session[0].'@implementation' == 'org.apache.maven.execution.MavenSession'
assert mojo.configuration.session[0].'@default-value' == '${session}'

assert mojo.configuration.project[0].text() == ''
assert mojo.configuration.project[0].'@implementation' == 'org.apache.maven.project.MavenProject'
assert mojo.configuration.project[0].'@default-value' == '${project}'

assert mojo.configuration.mojo[0].text() == ''
assert mojo.configuration.mojo[0].'@implementation' == 'org.apache.maven.plugin.MojoExecution'
assert mojo.configuration.mojo[0].'@default-value' == '${mojoExecution}'

assert mojo.configuration.plugin[0].text() == ''
assert mojo.configuration.plugin[0].'@implementation' == 'org.apache.maven.plugin.descriptor.PluginDescriptor'
assert mojo.configuration.plugin[0].'@default-value' == '${plugin}'

assert mojo.configuration.settings[0].text() == ''
assert mojo.configuration.settings[0].'@implementation' == 'org.apache.maven.settings.Settings'
assert mojo.configuration.settings[0].'@default-value' == '${settings}'

assert mojo.requirements.requirement.size() == 1

assert mojo.requirements.requirement[0].role.text() == 'org.apache.maven.project.MavenProjectHelper'
assert mojo.requirements.requirement[0].'role-hint'.text() == 'test'
assert mojo.requirements.requirement[0].'field-name'.text() == 'projectHelper'

assert mojo.parameters.parameter.size() == 8

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "namedParam" }[0]
assert parameter.name.text() == 'namedParam'
assert parameter.alias.text() == 'alias'
assert parameter.type.text() == 'java.lang.String'
assert parameter.deprecated.text() == 'As of 0.2'
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "basedir" }[0]
assert parameter.name.text() == 'basedir'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.io.File'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == 'Project directory.'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "mojo" }[0]
assert parameter.name.text() == 'mojo'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.plugin.MojoExecution'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "plugin" }[0]
assert parameter.name.text() == 'plugin'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.plugin.descriptor.PluginDescriptor'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "project" }[0]
assert parameter.name.text() == 'project'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.project.MavenProject'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "session" }[0]
assert parameter.name.text() == 'session'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.execution.MavenSession'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "settings" }[0]
assert parameter.name.text() == 'settings'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'org.apache.maven.settings.Settings'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == ''

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "touchFile" }[0]
assert parameter.name.text() == 'touchFile'
assert parameter.alias.isEmpty()
assert parameter.type.text() == 'java.io.File'
assert parameter.deprecated.isEmpty()
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

// check default values
mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "minimal"}[0]

assert mojo.goal.text() == 'minimal'
assert mojo.implementation.text() == 'org.apache.maven.plugin.coreit.Minimal'
assert mojo.language.text() == 'java'
assert mojo.description.text() == ''
assert mojo.deprecated.text() == ''
assert mojo.requiresDependencyResolution.text() == ''
assert mojo.requiresDependencyCollection.text() == ''
assert mojo.requiresProject.text() == 'true'
assert mojo.requiresOnline.text() == 'false'
assert mojo.requiresDirectInvocation.text() == 'false'
assert mojo.requiresReports.text() == 'false'
assert mojo.aggregator.text() == 'false'
assert mojo.threadSafe.text() == 'false'
assert mojo.phase.text() == ''
assert mojo.executePhase.text() == ''
assert mojo.executeLifecycle.text() == ''
assert mojo.executionStrategy.text() == 'once-per-session'
assert mojo.inheritedByDefault.text() == 'true'
assert mojo.instantiationStrategy.text() == 'per-lookup'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "param" }[0]
assert parameter.name.text() == 'param'
assert parameter.alias.text() == ''
assert parameter.type.text() == 'java.lang.String'
assert parameter.deprecated.text() == ''
assert parameter.required.text() == 'false'
assert parameter.editable.text() == 'true'
assert parameter.description.text() == ''

def requirement = mojo.requirements.requirement.findAll{ it.'field-name'.text() == "projectHelper" }[0]
assert requirement.role.text() == 'org.apache.maven.project.MavenProjectHelper'

// check values set by every annotation
mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "maximal"}[0]

assert mojo.goal.text() == 'maximal'
assert mojo.implementation.text() == 'org.apache.maven.plugin.coreit.Maximal'
assert mojo.language.text() == 'java'
assert mojo.description.text() == 'Checks maximum annotations with non-default values.'
assert mojo.deprecated.text() == 'deprecated-text'
assert mojo.requiresDependencyResolution.text() == 'compile'
assert mojo.requiresDependencyCollection.text() == 'test'
assert mojo.requiresProject.text() == 'false'
assert mojo.requiresOnline.text() == 'true'
assert mojo.requiresDirectInvocation.text() == 'true'
assert mojo.requiresReports.text() == 'true'
assert mojo.aggregator.text() == 'true'
assert mojo.configurator.text() == 'configurator-hint'
assert mojo.threadSafe.text() == 'true'
assert mojo.phase.text() == 'package'
assert mojo.executePhase.text() == 'compile'
assert mojo.executeLifecycle.text() == ''
assert mojo.executionStrategy.text() == 'always'
assert mojo.inheritedByDefault.text() == 'false'
assert mojo.instantiationStrategy.text() == 'singleton'

parameter = mojo.parameters.parameter.findAll{ it.name.text() == "param" }[0]
assert parameter.name.text() == 'param'
assert parameter.alias.text() == 'myAlias'
assert parameter.type.text() == 'java.lang.String'
assert parameter.since.text() == 'since-text'
assert parameter.deprecated.text() == 'deprecated-text'
assert parameter.required.text() == 'true'
assert parameter.editable.text() == 'false'
assert parameter.description.text() == 'Parameter description.'

requirement = mojo.requirements.requirement.findAll{ it.'field-name'.text() == "projectHelper" }[0]
assert requirement.role.text() == 'org.apache.maven.project.MavenProjectHelper'

// check help mojo source and class
assert new File( basedir, "target/classes/org/apache/maven/plugin/coreit/HelpMojo.class" ).isFile()
assert new File( basedir, "target/generated-sources/plugin/org/apache/maven/plugin/coreit/HelpMojo.java" ).isFile()
assert !new File( basedir, "target/generated-sources/plugin/HelpMojo.java" ).isFile()

return true;
