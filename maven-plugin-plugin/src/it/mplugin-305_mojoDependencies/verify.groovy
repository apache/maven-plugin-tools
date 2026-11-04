/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.xml.XmlParser

def parameterCount = { String variant ->
    File descriptorFile = new File( basedir, "target/${variant}/classes/META-INF/maven/plugin.xml" )
    assert descriptorFile.isFile() : "no plugin descriptor for ${variant}"
    def pluginDescriptor = new XmlParser().parse( descriptorFile )
    assert pluginDescriptor.mojos.mojo.size() == 1
    return pluginDescriptor.mojos.mojo.parameters.parameter.size()
}

// no <mojoDependencies>: every dependency is scanned for inherited parameters
assert parameterCount( "default" ) == 48
// <mojoDependencies/>: no dependency is scanned
assert parameterCount( "empty" ) == 0
// a single entry: only maven-surefire-common (3.1.0) is scanned
assert parameterCount( "single" ) == 51

return true
