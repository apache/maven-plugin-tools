---
title: Introduction
author: 
  - Vincent Siveton
  - Hervé Boutemy
---

<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->

<object type="image/svg+xml" data="images/plugin-tools-deps.svg" width="643" height="284">
</object>

# Maven Plugin Tools

The Maven Plugin Tools contains the necessary tools to generate repetitive content such as descriptor, help, and documentation.

|**Module**|**Overview**|
|---|---|
|**[maven-plugin-plugin](./maven-plugin-plugin/index.html)**|Create a Maven plugin descriptor for any mojos found in the source tree, generate reports, create help goal.|
|**[maven-plugin-report-plugin](./maven-plugin-report-plugin/index.html)**|The Plugin Report Plugin is used to create reports about the plugin being built.|
|[maven-plugin-tools-generators](./maven-plugin-tools-generators/index.html)|Generators (XML descriptor, help, documentation), used by maven-plugin-plugin to generate content from descriptor extracted from sources.|
|[maven-plugin-tools-api](./maven-plugin-tools-api/index.html)|Extractor API, used by maven-plugin-plugin to extract Mojo information.|
|&nbsp;&nbsp;[maven-plugin-tools-java](./maven-plugin-tools-java/index.html)|Extractor for plugins written in Java annotated with Mojo Javadoc Tags.|
|&nbsp;&nbsp;[maven-plugin-tools-annotations](./maven-plugin-tools-annotations/index.html)|Extractor for plugins written in Java with Java annotations.|
|&nbsp;&nbsp;&nbsp;&nbsp;[maven-plugin-annotations](./maven-plugin-annotations/index.html)|Provides the Java annotations to use in Mojos.|
|[maven-script](./maven-script/index.html) (deprecated)|Maven Script Mojo Support lets developer write Maven plugins/goals with scripting languages instead of compiled Java.<br />Deprecated since 3.7.0|

## Plugin Descriptors

The plugin descriptor is first being generated in memory finally containing some values in HTML format before being persisted into three different XML files. The formats differ in 
- whether they contain all elements or just a limited set of elements defined by the [Plugin Descriptor Spec](https://maven.apache.org/ref/current/maven-plugin-api/plugin.html)

- whether descriptive elements contain HTML or plain text values

- whether they are packaged in the resulting JAR or not

Javadoc tags are in general being resolved and replaced by their XHTML value before they end up in the according plugin descriptor attributes `description` and `deprecated`. Also javadoc code links via `{@link}` or `@see` are replaced by links to the according Javadoc pages if configured accordingly. Plaintext is afterwards being generated out of the XHTML (where most XHTML element are just stripped and links are emitted inside angle brackets). 

|File name|Allows HTML|Limited Elements|Contained in JAR|
|---|---|---|---|
|plugin.xml|no|no|yes|
|plugin-help.xml|no|yes|yes|
|plugin-enhanced.xml|yes|yes|no (volatile file)|

![](images/plugin-descriptors.svg) 

## See Also

- [Maven Plugin Testing](/plugin-testing/)

- [Maven Plugin API](/ref/current/maven-plugin-api/)

