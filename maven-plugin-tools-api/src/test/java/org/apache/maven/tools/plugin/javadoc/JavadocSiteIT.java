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

import javax.net.ssl.SSLException;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import org.apache.maven.settings.Settings;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assumptions.abort;

@ExtendWith(JavadocSiteIT.AbortWhenSiteUnreachable.class)
class JavadocSiteIT {

    static Stream<Arguments> javadocBaseUrls() {
        // only test LTS versions by default
        return Stream.of(
                Arguments.of(URI.create("https://docs.oracle.com/en/java/javase/17/docs/api/")),
                Arguments.of(URI.create("https://docs.oracle.com/en/java/javase/11/docs/api/")),
                Arguments.of(URI.create("https://docs.oracle.com/javase/8/docs/api/")));
    }

    @ParameterizedTest
    @MethodSource("javadocBaseUrls")
    void constructors(URI javadocBaseUrl) throws Exception {
        JavadocSite site = new JavadocSite(javadocBaseUrl, (Settings) null);
        JavadocSiteTest.assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "java.lang", "String", "String(byte[],int)", MemberType.CONSTRUCTOR, true)));
    }

    @ParameterizedTest
    @MethodSource("javadocBaseUrls")
    void methods(URI javadocBaseUrl) throws Exception {
        JavadocSite site = new JavadocSite(javadocBaseUrl, (Settings) null);
        JavadocSiteTest.assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "java.lang", "String", "copyValueOf(char[],int,int)", MemberType.METHOD, true)));
    }

    @ParameterizedTest
    @MethodSource("javadocBaseUrls")
    void fields(URI javadocBaseUrl) throws Exception {
        JavadocSite site = new JavadocSite(javadocBaseUrl, (Settings) null);
        JavadocSiteTest.assertUrlValid(site.createLink(new FullyQualifiedJavadocReference(
                "java.lang", "String", "CASE_INSENSITIVE_ORDER", MemberType.FIELD, true)));
    }

    @ParameterizedTest
    @MethodSource("javadocBaseUrls")
    void nestedClass(URI javadocBaseUrl) throws Exception {
        JavadocSite site = new JavadocSite(javadocBaseUrl, (Settings) null);
        JavadocSiteTest.assertUrlValid(
                site.createLink(new FullyQualifiedJavadocReference("java.util", "Map.Entry", true)));
    }

    @ParameterizedTest
    @MethodSource("javadocBaseUrls")
    void clazz(URI javadocBaseUrl) throws Exception {
        JavadocSite site = new JavadocSite(javadocBaseUrl, (Settings) null);
        JavadocSiteTest.assertUrlValid(site.createLink("java.lang", "String"));
    }

    /**
     * Turns a failure to reach the remote javadoc site into a skipped test, so that a slow or blocked network does not
     * make the build red. A wrong link still fails: {@link JavadocSite#getReader(java.net.URL,
     * org.apache.maven.settings.Settings)} reports every non-200 as {@code FileNotFoundException}, which is not
     * handled here.
     */
    static final class AbortWhenSiteUnreachable implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException
                        || cause instanceof SocketException
                        || cause instanceof UnknownHostException
                        || cause instanceof SSLException) {
                    abort("Remote javadoc site is unreachable: " + cause);
                }
            }
            throw throwable;
        }
    }
}
