package org.apache.maven.plugins.annotations;

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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a {@code Mojo} or a Mojo’s {@code Parameter} when JavaDoc extraction is not feasible (because of deviating
 * documentation goals) or not possible (e.g. for other JVM languages like Scala, Groovy or Kotlin).
 */
@Documented
@Retention( RetentionPolicy.CLASS )
@Target( { ElementType.TYPE, ElementType.FIELD } )
@Inherited
public @interface Description
{
    /**
     * Description content for the {@code Mojo} or Mojo {@code Parameter}.
     *
     * <p>A &quot;Safe HTML&quot; subset can be used. This is achieved by running
     * the content through the <a href="https://github.com/owasp/java-html-sanitizer"<OWASP Java HTML Sanitizer</a>
     * before rendering.</p>
     *
     * @return a description of the Mojo or the parameter.
     */
    String content();

    /**
     * The version of the plugin since when this goal or parameter was introduced (inclusive, optional).
     *
     * @return The version of the plugin since when this goal or parameter was introduced (inclusive) or an empty string
     * of no since version has been given.
     */
    String since() default "";

    /**
     * Marks this Mojo goal or parameter as deprecated.
     *
     * @return {@code true} whether this Mojo goal or parameter is deprecated.
     */
    boolean deprecated() default false;

    /**
     * Deprecation reason (optional).
     *
     * @return an empty String if no deprecation reason has been given or a description.
     */
    String deprecatedBecause() default "";

    /**
     * Version since when this goal or parameter has been deprecated.
     *
     * @return the version since when this goal or parameter has been deprecated (inclusive) or an empty String if no
     * version was given.
     */
    String deprecatedSince() default "";
}
