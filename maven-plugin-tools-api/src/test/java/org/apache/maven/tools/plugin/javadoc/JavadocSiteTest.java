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
package org.apache.maven.tools.plugin.javadoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests against the locally available javadoc sites. Doesn't require internet connectivity.
 */
class JavadocSiteTest {

    static Stream<Arguments> jdkNamesAndVersions() {
        return Stream.of(
                Arguments.of("jdk21", JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER, "21"),
                Arguments.of("jdk17", JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER, "17"),
                Arguments.of("jdk11", JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER, "11"),
                Arguments.of("jdk8", JavadocLinkGenerator.JavadocToolVersionRange.JDK8_OR_9, "8"));
    }

    @ParameterizedTest
    @MethodSource("jdkNamesAndVersions")
    void constructorLink(String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version) throws Exception {
        JavadocSite site = getLocalJavadocSite(jdkName, version);
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                "CurrentClass",
                "CurrentClass()",
                MemberType.CONSTRUCTOR,
                false)));
    }

    @ParameterizedTest
    @MethodSource("jdkNamesAndVersions")
    void methodLinks(String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version) throws Exception {
        JavadocSite site = getLocalJavadocSite(jdkName, version);
        // test generics signature
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                "CurrentClass",
                "genericsParamMethod(java.util.Collection,java.util.function.BiConsumer)",
                MemberType.METHOD,
                false)));
        // test array
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                "CurrentClass",
                "arrayParamMethod(int[],java.lang.String[][][])",
                MemberType.METHOD,
                false)));
    }

    @ParameterizedTest
    @MethodSource("jdkNamesAndVersions")
    void fieldLink(String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version) throws Exception {
        JavadocSite site = getLocalJavadocSite(jdkName, version);
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                "CurrentClass",
                "field1",
                MemberType.FIELD,
                false)));
    }

    @ParameterizedTest
    @MethodSource("jdkNamesAndVersions")
    void packageLink(String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version) throws Exception {
        JavadocSite site = getLocalJavadocSite(jdkName, version);
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test", false)));
    }

    @ParameterizedTest
    @MethodSource("jdkNamesAndVersions")
    void classLink(String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version) throws Exception {
        JavadocSite site = getLocalJavadocSite(jdkName, version);
        assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass", false)));
    }

    @Test
    void classLinkFromPackageAndClassNameWithExternalJavadocRequiringModules() throws URISyntaxException, IOException {
        URI baseUri = new URI("https://docs.oracle.com/en/java/javase/11/docs/api/");
        JavadocSite site = new JavadocSite(
                baseUri,
                JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER,
                Collections.singletonMap("java.lang", "java.base"));
        // don't request URL to make test independent of network connectivity
        assertEquals(baseUri.resolve("java.base/java/lang/String.html"), site.createLink("java.lang", "String"));
    }

    @Test
    void getPackageAndClassName() {
        assertEquals(
                new AbstractMap.SimpleEntry<>("java.util", "Map"),
                JavadocSite.getPackageAndClassName(Map.class.getName()));
        assertEquals(
                new AbstractMap.SimpleEntry<>("java.util", "Map.Entry"),
                JavadocSite.getPackageAndClassName(Map.Entry.class.getName()));
        assertEquals(
                new AbstractMap.SimpleEntry<>(
                        "org.apache.maven.tools.plugin.javadoc", "JavadocSiteTest.NestedClass.DeeplyNestedClass"),
                JavadocSite.getPackageAndClassName(NestedClass.DeeplyNestedClass.class.getName()));
        assertThrows(
                IllegalArgumentException.class,
                () -> JavadocSite.getPackageAndClassName("java.util.Map$0001Entry")); // some local class
        assertThrows(IllegalArgumentException.class, () -> JavadocSite.getPackageAndClassName("java.util."));
        assertThrows(IllegalArgumentException.class, () -> JavadocSite.getPackageAndClassName("int"));
    }

    public static final class NestedClass {
        public static final class DeeplyNestedClass {}
    }

    static JavadocSite getLocalJavadocSite(String name, JavadocLinkGenerator.JavadocToolVersionRange version)
            throws URISyntaxException {
        URI javadocBaseUri =
                JavadocSiteTest.class.getResource("/javadoc/" + name + "/").toURI();
        return new JavadocSite(javadocBaseUri, version);
    }

    static void assertUrlValid(final URI url) {
        try (BufferedReader reader = JavadocSite.getReader(url.toURL(), null)) {
            if (url.getFragment() != null) {
                Pattern pattern = JavadocSite.getAnchorPattern(url.getFragment());
                if (reader.lines().noneMatch(pattern.asPredicate())) {
                    fail("Although URL " + url + " exists, no line matching the pattern " + pattern
                            + " found in response");
                }
            }
        } catch (IOException e) {
            fail("Could not find URL " + url, e);
        }
    }
}
