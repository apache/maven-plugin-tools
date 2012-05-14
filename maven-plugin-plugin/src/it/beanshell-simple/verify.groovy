File touchFile = new File( basedir, "target/touch.txt" )
assert touchFile.exists()
assert touchFile.isFile()
content = touchFile.text
assert content.contains('This is a Beanshell test');


return true;
