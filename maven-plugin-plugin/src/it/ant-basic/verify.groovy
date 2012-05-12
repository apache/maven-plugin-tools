File touchFile = new File( basedir, "target/touch.txt" )
assert touchFile.isFile()

File descriptorFile = new File( basedir, "target/classes/META-INF/maven/plugin.xml" );
assert descriptorFile.isFile()

File oldHelpClass = new File( basedir, "target/classes/HelpMojo.class" );
assert !oldHelpClass.exists()


return true;
