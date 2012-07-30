File touchFile = new File( basedir, "antsample-maven-plugin/target/site/sample-mojo.html" )
assert touchFile.exists()
assert touchFile.isFile()
content = touchFile.text
assert content.contains('Since');


return true;
