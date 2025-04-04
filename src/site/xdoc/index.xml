<?xml version="1.0"?>

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

<document xmlns="http://maven.apache.org/XDOC/2.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/XDOC/2.0 http://maven.apache.org/xsd/xdoc-2.0.xsd">

  <properties>
    <title>Introduction</title>
    <author>Vincent Siveton</author>
    <author email="hboutemy_AT_apache_DOT_org">Hervé Boutemy</author>
  </properties>

  <body>

    <section name="Maven Plugin Tools">

      <p>The Maven Plugin Tools contains the necessary tools to generate repetitive content such as descriptor,
      help, and documentation.</p>

      <p>
        <object type="image/svg+xml" data="images/plugin-tools-deps.svg" width="643" height="284">
        </object>
      </p>

      <table>
        <tr><th><b>Module</b></th><th><b>Overview</b></th></tr>
        <tr>
          <td><b><a href="./maven-plugin-plugin/index.html">maven-plugin-plugin</a></b></td>
          <td>Create a Maven plugin descriptor for any mojos found in the source tree, generate reports, create help goal.</td>
        </tr>
        <tr>
          <td><b><a href="./maven-plugin-report-plugin/index.html">maven-plugin-report-plugin</a></b></td>
          <td>The Plugin Report Plugin is used to create reports about the plugin being built.</td>
        </tr>
        <tr>
          <td><a href="./maven-plugin-tools-generators/index.html">maven-plugin-tools-generators</a></td>
          <td>Generators (XML descriptor, help, documentation), used by maven-plugin-plugin to generate content from descriptor extracted from sources.</td>
        </tr>
        <tr>
          <td><a href="./maven-plugin-tools-api/index.html">maven-plugin-tools-api</a></td>
          <td>Extractor API, used by maven-plugin-plugin to extract Mojo information.</td>
        </tr>
        <tr>
          <td>&nbsp;&nbsp;<a href="./maven-plugin-tools-java/index.html">maven-plugin-tools-java</a></td>
          <td>Extractor for plugins written in Java annotated with Mojo Javadoc Tags.</td>
        </tr>
        <tr>
          <td>&nbsp;&nbsp;<a href="./maven-plugin-tools-annotations/index.html">maven-plugin-tools-annotations</a></td>
          <td>Extractor for plugins written in Java with Java annotations.</td>
        </tr>
        <tr>
          <td>&nbsp;&nbsp;&nbsp;&nbsp;<a href="./maven-plugin-annotations/index.html">maven-plugin-annotations</a></td>
          <td>Provides the Java annotations to use in Mojos.</td>
        </tr>
        <tr>
          <td><a href="./maven-script/index.html">maven-script</a> (deprecated)</td>
          <td>Maven Script Mojo Support lets developer write Maven plugins/goals with scripting languages instead of compiled Java.<br/>
          Deprecated since 3.7.0</td>
        </tr>
      </table>

      <subsection name="Plugin Descriptors">
        <p>The plugin descriptor is first being generated in memory finally containing some values in HTML format before being persisted into three different XML files.
        The formats differ in
          <ul>
            <li>whether they contain all elements or just a limited set of elements defined by the <a href="https://maven.apache.org/ref/current/maven-plugin-api/plugin.html">Plugin Descriptor Spec</a></li>
            <li>whether descriptive elements contain HTML or plain text values</li>
            <li>whether they are packaged in the resulting JAR or not</li>
          </ul>
        Javadoc tags are in general being resolved and replaced by their XHTML value before they end up in the according plugin descriptor attributes <code>description</code> and <code>deprecated</code>.
        Also javadoc code links via <code>{@link}</code> or <code>@see</code> are replaced by links to the according Javadoc pages if configured accordingly.
        Plaintext is afterwards being generated out of the XHTML (where most XHTML element are just stripped and links are emitted inside angle brackets).
        </p>
        <table>
          <tr>
            <th>File name</th>
            <th>Allows HTML</th>
            <th>Limited Elements</th>
            <th>Contained in JAR</th>
          </tr>
          <tr>
            <td>plugin.xml</td>
            <td>no</td>
            <td>no</td>
            <td>yes</td>
          </tr>
          <tr>
            <td>plugin-help.xml</td>
            <td>no</td>
            <td>yes</td>
            <td>yes</td>
          </tr>
          <tr>
            <td>plugin-enhanced.xml</td>
            <td>yes</td>
            <td>yes</td>
            <td>no (volatile file)</td>
          </tr>
        </table>
        <p>
          <img src="images/plugin-descriptors.svg" width="759" border="0" />
        </p>
      </subsection>

      <subsection name="See Also">
        <ul>
          <li><a href="/plugin-testing/">Maven Plugin Testing</a></li>
          <li><a href="/ref/current/maven-plugin-api/">Maven Plugin API</a></li>
        </ul>
      </subsection>

    </section>

  </body>

</document>
