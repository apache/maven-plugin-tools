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
 * Used to configure your Mojo parameters to be injected by
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/MavenPluginManager.html">
 * <code>MavenPluginManager.getConfiguredMojo(...)</code></a>.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Documented
@Retention( RetentionPolicy.CLASS )
@Target( { ElementType.FIELD } )
@Inherited
public @interface Parameter
{
    /**
     * name of the bean property used to get/set the field: by default, field name is used.
     * @return the name of the bean property
     */
    String name() default "";

    /**
     * alias supported to get parameter value.
     * @return the alias
     */
    String alias() default "";

    /**
     * Property to use to retrieve a value. Can come from <code>-D</code> execution, setting properties or pom
     * properties.
     * @return property name
     */
    String property() default "";

    /**
     * parameter default value, eventually containing <code>${...}</code> expressions which will be interpreted at
     * inject time: see
     * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html">
     * PluginParameterExpressionEvaluator</a>. 
     * @return the default value
     */
    String defaultValue() default "";

    /**
     * is the parameter required?
     * @return <code>true</code> if the Mojo should fail when the parameter cannot be injected
     */
    boolean required() default false;

    /**
     * Specifies that this parameter cannot be configured directly by the user (as in the case of POM-specified
     * configuration). This is useful when you want to force the user to use common POM elements rather than plugin
     * configurations, as in the case where you want to use the artifact's final name as a parameter. In this case, you
     * want the user to modify <code>&lt;build&gt;&lt;finalName/&gt;&lt;/build&gt;</code> rather than specifying a value
     * for finalName directly in the plugin configuration section. It is also useful to ensure that - for example - a
     * List-typed parameter which expects items of type Artifact doesn't get a List full of Strings.
     * 
     * @return <code>true</code> if the user should not be allowed to configure the parameter directly
     */
    boolean readonly() default false;

    /**
     * Description for this parameter. Has the same format as a Javadoc comment body (that is, HTML with Javadoc inline
     * tags).
     *
     * <p>Ordinarily, this information is taken from Javadoc comments. This annotation is used when documenting a Maven
     * plugin that is written in a language other than Java, but which supports Java annotations, such as Groovy or
     * Scala.</p>
     *
     * @since 3.5
     */
    String description() default "";

    /**
     * The first version of the plugin when this parameter was added.
     *
     * <p>Ordinarily, this information is taken from Javadoc comments. This annotation is used when documenting a Maven
     * plugin that is written in a language other than Java, but which supports Java annotations, such as Groovy or
     * Scala.</p>
     *
     * @since 3.5
     */
    String since() default "";

    /**
     * The reason why this parameter is deprecated.
     *
     * <p>If this is given, then the parameter should also be annotated with {@code @}{@link Deprecated}, like so:</p>
     *
     * <pre><code>@Deprecated
&#64;Parameter(..., deprecated = "this parameter is no longer used")
private String oldParameterThatNowDoesNothing;</code></pre>
     *
     * <p>Ordinarily, this information is taken from Javadoc comments. This annotation is used when documenting a Maven
     * plugin that is written in a language other than Java, but which supports Java annotations, such as Groovy or
     * Scala.</p>
     *
     * @since 3.5
     */
    String deprecated() default "";
}
