File touchFile = new File( basedir, "target/touch.txt" )
assert touchFile.isFile()

File descriptorFile = new File( basedir, "target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

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
assert mojo.executePhase.text() == 'generate-sources'
assert mojo.executeLifecycle.text() == 'cobertura'

assert mojo.configuration.basedir[0].text() == ''
assert mojo.configuration.basedir[0].'@implementation' == 'java.io.File'
assert mojo.configuration.basedir[0].'@default-value' == '${basedir}'

assert mojo.configuration.touchFile[0].text() == '${first.touchFile}'
assert mojo.configuration.touchFile[0].'@implementation' == 'java.io.File'
assert mojo.configuration.touchFile[0].'@default-value' == '${project.build.directory}/touch.txt'

assert mojo.requirements.requirement.size() == 1

assert mojo.requirements.requirement[0].role.text() == 'org.apache.maven.project.MavenProjectHelper'
assert mojo.requirements.requirement[0].'role-hint'.text() == 'test'
assert mojo.requirements.requirement[0].'field-name'.text() == 'projectHelper'

assert mojo.parameters.parameter.size() == 3

assert mojo.parameters.parameter[0].name.text() == 'aliasedParam'
assert mojo.parameters.parameter[0].alias.text() == 'alias'
assert mojo.parameters.parameter[0].type.text() == 'java.lang.String'
assert mojo.parameters.parameter[0].deprecated.text() == 'As of 0.2'
assert mojo.parameters.parameter[0].required.text() == 'false'
assert mojo.parameters.parameter[0].editable.text() == 'true'
assert mojo.parameters.parameter[0].description.text() == ''

assert mojo.parameters.parameter[1].name.text() == 'basedir'
assert mojo.parameters.parameter[1].alias.isEmpty()
assert mojo.parameters.parameter[1].type.text() == 'java.io.File'
assert mojo.parameters.parameter[1].deprecated.isEmpty()
assert mojo.parameters.parameter[1].required.text() == 'false'
assert mojo.parameters.parameter[1].editable.text() == 'false'
assert mojo.parameters.parameter[1].description.text() == 'Project directory.'

assert mojo.parameters.parameter[2].name.text() == 'touchFile'
assert mojo.parameters.parameter[2].alias.isEmpty()
assert mojo.parameters.parameter[2].type.text() == 'java.io.File'
assert mojo.parameters.parameter[2].deprecated.isEmpty()
assert mojo.parameters.parameter[2].required.text() == 'true'
assert mojo.parameters.parameter[2].editable.text() == 'true'
assert mojo.parameters.parameter[2].description.text() == ''

mojo = pluginDescriptor.mojos.mojo.findAll{ it.goal.text() == "second"}[0]

assert mojo.requiresDependencyCollection.text() == 'compile'
assert mojo.threadSafe.text() == 'true'

return true;
