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
 * Used to configure your Mojo parameters.
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
     * alias supported to get parameter value.
     * @return the alias
     */
    String alias() default "";

    /**
     * Property to use to retrieve a value. Can come from <code>-D</code> execution, setting properties or pom properties.
     * @return property name
     */
    String expression() default "";

    /**
     * parameter default value, eventually containing <code>${...}</code> expressions which will be interpreted at inject time. 
     * @return the default value
     */
    String defaultValue() default "";

    /**
     * is the parameter required?
     * @return <code>true</code> if the Mojo should fail when the parameter cannot be injected
     */
    boolean required() default false;

    /**
     * ignored...
     * @return
     */
    boolean readonly() default false;
}
