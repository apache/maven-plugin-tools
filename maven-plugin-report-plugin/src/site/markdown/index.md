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

# Maven Plugin Report Plugin

The Maven Plugin Report Plugin is used to create report files for the Mojos.

## Goals Overview

The Plugin Report Plugin has two goals:

- [plugin-report:report](./report-mojo.html) which generates the plugin documentation: one overview report and documentation for each plugin's goal (mojo).
- [plugin-report:report-no-fork](./report-no-fork-mojo.html) which generates the plugin documentation: one overview report and documentation for each plugin's goal (mojo) without forking the `process-classes` phase again. Note that this goal does require generation of classes before site generation, e.g. by invoking `mvn clean verify site`.
## Usage

General instructions on how to use the Plugin Report Plugin can be found on the [usage page](./usage.html).

In case you still have questions regarding the plugin's usage feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

The following examples shows how to use the Plugin Report Plugin in more advanced usecases:

- [Configuring Generation of Documentation Reports](./examples/generate-report.html)
