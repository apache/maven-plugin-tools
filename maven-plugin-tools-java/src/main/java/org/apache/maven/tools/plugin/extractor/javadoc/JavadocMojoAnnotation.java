package org.apache.maven.tools.plugin.extractor.javadoc;

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

/**
 * List of all Javadoc annotations used to describe a java Mojo.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.4
 */
@SuppressWarnings( "checkstyle:interfaceistype" )
public interface JavadocMojoAnnotation
{
    // ----------------------------------------------------------------------
    // Descriptor for type i.e. Mojo
    // ----------------------------------------------------------------------

    /**
     * <p>
     * Flags this Mojo to run it in a multi module way, i.e. aggregate the build with the set of projects listed
     * as modules.
     * </p>
     * <p>
     * Refer to <code>&#64;aggregator</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String AGGREGATOR = "aggregator";

    /**
     * <p>
     * The configurator type to use when injecting parameter values into this Mojo. The value is normally deduced
     * from the Mojo's implementation language, but can be specified to allow a custom ComponentConfigurator
     * implementation to be used.
     * </p>
     * <p>
     * Refer to <code>&#64;configurator &lt;roleHint&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String CONFIGURATOR = "configurator";

    /**
     * <p>
     * The description for the Mojo.
     * </p>
     * <p>
     * Refer to <code>&#64;description</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     * <p>
     * <b>Note</b>: Mojo's description is auto-detected.
     * </p>
     */
    String DESCRIPTION = "description";

    /**
     * <p>
     * Refer to <code>&#64;execute ...</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String EXECUTE = "execute";

    /**
     * <p>
     * The Mojo goal will be executed in isolation.
     * </p>
     * <p>
     * Refer to <code>&#64;execute goal="&lt;goalName&gt;"</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String EXECUTE_GOAL = "goal";

    /**
     * <p>
     * The Mojo will be invoked in a parallel lifecycle.
     * </p>
     * <p>
     * Refer to <code>&#64;execute lifecycle="&lt;lifecycleId&gt;"</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String EXECUTE_LIFECYCLE = "lifecycle";

    /**
     * <p>
     * The Mojo will be invoked in a parallel lifecycle, ending at the given phase.
     * </p>
     * <p>
     * Refer to <code>&#64;execute phase="&lt;phaseName&gt;"</code>.
     * </p>
     * <p>
     * Refer to <code>&#64;execute lifecycle="&lt;lifecycleId&gt;" phase="&lt;phaseName&gt;"</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String EXECUTE_PHASE = "phase";

    /**
     * <p>
     * Refer to <code>&#64;executionStrategy &lt;strategy&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String EXECUTION_STATEGY = "executionStrategy";

    /**
     * <p>
     * The name for the Mojo that users will reference to execute it.
     * </p>
     * <p>
     * Refer to <code>&#64;goal &lt;goalName&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String GOAL = "goal";

    /**
     * <p>
     * The Mojo's fully-qualified class name.
     * </p>
     * <p>
     * Refer to <code>&#64;implementation</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     * <p>
     * <b>Note</b>: Mojo's implementation is auto-detected.
     * </p>
     */
    String IMPLEMENTATION = "implementation";

    /**
     * <p>
     * Allow Mojo inheritance.
     * </p>
     * <p>
     * Refer to <code>&#64;inheritByDefault &lt;true|false&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String INHERIT_BY_DEFAULT = "inheritByDefault";

    /**
     * <p>
     * Refer to <code>&#64;instantiationStrategy &lt;per-lookup&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String INSTANTIATION_STRATEGY = "instantiationStrategy";

    /**
     * <p>
     * The implementation language for the Mojo.
     * </p>
     * <p>
     * Refer to <code>&#64;language</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     * <p>
     * <b>Note</b>: Mojo's implementation is auto-detected.
     * </p>
     */
    String LANGUAGE = "language";

    /**
     * <p>
     * Specifies the execution strategy.
     * </p>
     * <p>
     * Refer to <code>&#64;attainAlways</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     * @deprecated use <code>&#64;executionStrategy always</code> instead
     */
    String MULTI_EXECUTION_STRATEGY = "attainAlways";

    /**
     * <p>
     * Refer to <code>&#64;phase &lt;phaseName&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String PHASE = "phase";

    /**
     * <p>
     * Flags this Mojo as requiring the dependencies in the specified scope (or an implied scope) to be resolved
     * before it can execute. Currently supports <code>compile</code>, <code>runtime</code>,
     * <code>compile+runtime</code> and <code>test</code> scopes.
     * </p>
     * <p>
     * Refer to <code>&#64;requiresDependencyResolution &lt;requiredScope&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    /**
     * <p>
     * Flags this Mojo as requiring the dependencies in the specified scope (or an implied scope) to be collected
     * before it can execute. Currently supports <code>compile</code>, <code>runtime</code>,
     * <code>compile+runtime</code> and <code>test</code> scopes.
     * </p>
     * <p>
     * Refer to <code>&#64;requiresDependencyCollection &lt;requiredScope&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_DEPENDENCY_COLLECTION = "requiresDependencyCollection";


    /**
     * <p>
     * Refer to <code>&#64;requiresDirectInvocation &lt;true|false&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_DIRECT_INVOCATION = "requiresDirectInvocation";

    /**
     * <p>
     * Flags this Mojo to run online.
     * </p>
     * <p>
     * Refer to <code>&#64;requiresOnline &lt;true|false&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_ONLINE = "requiresOnline";

    /**
     * <p>
     * Flags this Mojo to run inside of a project.
     * </p>
     * <p>
     * Refer to <code>&#64;requiresProject &lt;true|false&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_PROJECT = "requiresProject";

    /**
     * <p>
     * Flags this Mojo to run inside reports.
     * </p>
     * <p>
     * Refer to <code>&#64;requiresReports &lt;true|false&gt;</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String REQUIRES_REPORTS = "requiresReports";

    /**
     * <p>
     * Indicates that this mojo is thread-safe and can be run in parallel.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * </p>
     */
    String THREAD_SAFE = "threadSafe";


    // ----------------------------------------------------------------------
    // Descriptor for fields i.e. parameters
    // ----------------------------------------------------------------------

    /**
     * <p>
     * Populate the field with an instance of a Plexus component. This is like declaring a requirement in a
     * Plexus component.
     * </p>
     * <p>
     * Refer to <code>&#64;component ...</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String COMPONENT = "component";

    /**
     * <p>
     * Refer to <code>&#64;component role="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String COMPONENT_ROLE = "role";

    /**
     * <p>
     * Refer to <code>&#64;component roleHint="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String COMPONENT_ROLEHINT = "roleHint";

    /**
     * <p>
     * Refer to <code>&#64;parameter ...</code>
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER = "parameter";

    /**
     * <p>
     * This defines the name of the bean property used to get/set the field: by default, field name is used.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter name="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER_NAME = "name";

    /**
     * <p>
     * This defines an alias which can be used to configure a parameter. This is primarily useful to improve
     * user-friendliness.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter alias="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER_ALIAS = "alias";

    /**
     * <p>
     * This defines the default value to be injected into this parameter of the Mojo at build time.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter default-value="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER_DEFAULT_VALUE = "default-value";

    /**
     * <p>
     * This defines the expression used to calculate the value to be injected into this parameter of the
     * Mojo at build time.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter expression="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     * @deprecated use PARAMETER_PROPERTY instead
     */
    String PARAMETER_EXPRESSION = "expression";

    /**
     * <p>
     * This defines the property used to calculate the value to be injected into this parameter of the
     * Mojo at build time, which can come from <code>-D</code> execution, setting properties or pom properties.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter property="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER_PROPERTY = "property";

    /**
     * <p>
     * This defines the default implementation in the case the parameter type is an interface.
     * </p>
     * <p>
     * Refer to <code>&#64;parameter implementation="..."</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String PARAMETER_IMPLEMENTATION = "implementation";

    /**
     * <p>
     * Specifies that this parameter cannot be configured directly by the user (as in the case of POM-specified
     * configuration).
     * </p>
     * <p>
     * Refer to <code>&#64;readonly</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String READONLY = "readonly";

    /**
     * <p>
     * Specifies that this parameter is required for the Mojo to function.
     * </p>
     * <p>
     * Refer to <code>&#64;required</code>.
     * </p>
     * <p>
     * <b>Note</b>: Should be defined in a Mojo Field.
     * </p>
     */
    String REQUIRED = "required";

    // ----------------------------------------------------------------------
    // Descriptor for type and fields
    // ----------------------------------------------------------------------

    /**
     * <p>
     * Refer to <code>&#64;since &lt;deprecated-text&gt;</code>
     * </p>
     * <p>
     * <b>Note</b>: Could be defined in a Mojo Type or a Mojo Field.
     * </p>
     */
    String SINCE = "since";

    /**
     * <p>
     * Refer to <code>&#64;deprecated &lt;since-text&gt;</code>
     * </p>
     * <p>
     * <b>Note</b>: Could be defined in a Mojo Type or a Mojo Field.
     * </p>
     */
    String DEPRECATED = "deprecated";

}
