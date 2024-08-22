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
package org.apache.maven.tools.plugin.extractor.annotations;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

@Mojo(name = "parameter-with-generics")
public class ParametersWithGenericsMojo extends AbstractMojo {

    @Parameter
    private String string;

    @Parameter
    private Map<String, Boolean> stringBooleanMap;

    @Parameter
    private Collection<Integer> integerCollection;

    @Parameter
    private Collection<Collection<String>> nestedStringCollection;

    @Parameter
    private Collection<Integer[]> integerArrayCollection;

    @Parameter
    private Map<String, List<String>> stringListStringMap;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {}

    @Parameter(name = "numberList")
    public void setNumberList(List<Number> numberList) {}

    public static class NestedClass<E extends Number> {
        /**
         * Some field without type parameter but non-empty signature
         */
        protected E filter;
    }
}
