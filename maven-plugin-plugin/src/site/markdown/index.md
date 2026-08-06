<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Maven Plugin Plugin

The Maven Plugin Plugin is used to create a [Maven plugin descriptor](/ref/current/maven-plugin-api/plugin.html) for any [Mojo](/general.html#What_is_a_Mojo)'s found in the source tree, to include in the JAR. It is also used to generate report files for the Mojos as well as the artifact metadata and generating a generic help goal.

## Goals Overview

The Plugin Plugin has four goals:

- [plugin:descriptor](./descriptor-mojo.html) generates a plugin descriptor,
- [plugin:addPluginArtifactMetadata](./addPluginArtifactMetadata-mojo.html) injects any plugin-specific artifact metadata to the project's artifact, for subsequent installation and deployment,
- [plugin:helpmojo](./helpmojo-mojo.html) generates a help mojo which describes all mojos in a plugin,
- [plugin:help](./help-mojo.html) display help information on maven-plugin-plugin.
## Usage

General instructions on how to use the Plugin Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below. Last but not least, users occasionally contribute additional examples, tips or errata to the [Plugin Developers Centre page](https://maven.apache.org/plugin-developers/index.html).

In case you still have questions regarding the plugin's usage feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

The following examples shows how to use the Plugin Plugin in more advanced usecases:

- [Using Plugin Tools Java5 annotations for your Mojo](./examples/using-annotations.html)
- [Configuring Generation of Plugin Descriptor](./examples/generate-descriptor.html)
- [Configuring Generation of Help Mojo](./examples/generate-help.html)
