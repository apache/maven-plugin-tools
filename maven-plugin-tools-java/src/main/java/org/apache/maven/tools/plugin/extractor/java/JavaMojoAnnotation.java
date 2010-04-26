package org.apache.maven.tools.plugin.extractor.java;

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
 * List all Javadoc annotations used to describe a Mojo.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.4
 */
public interface JavaMojoAnnotation
{
    // ----------------------------------------------------------------------
    // Descriptor for type i.e. Mojo
    // ----------------------------------------------------------------------

    /**
     * Flags this Mojo to run it in a multi module way, i.e. aggregate the build with the set of projects listed
     * as modules.
     * <br/>
     * Refer to <code>&#64;aggregator</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String AGGREGATOR = "aggregator";

    /**
     * The configurator type to use when injecting parameter values into this Mojo. The value is normally deduced
     * from the Mojo's implementation language, but can be specified to allow a custom ComponentConfigurator
     * implementation to be used.
     * <br/>
     * Refer to <code>&#64;configurator &lt;roleHint&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String CONFIGURATOR = "configurator";

    /**
     * The description for the Mojo.
     * <br/>
     * Refer to <code>&#64;description</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * <br/>
     * <b>Note</b>: Mojo's description is auto-detect.
     */
    String DESCRIPTION = "description";

    /**
     * Refer to <code>&#64;execute ...</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String EXECUTE = "execute";

    /**
     * The Mojo goal will be executed in isolation.
     * <br/>
     * Refer to <code>&#64;execute goal="&lt;goalName&gt;"</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String EXECUTE_GOAL = "goal";

    /**
     * The Mojo will be invoke in a parallel lifecycle.
     * <br/>
     * Refer to <code>&#64;execute lifecycle="&lt;lifecycleId&gt;"</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String EXECUTE_LIFECYCLE = "lifecycle";

    /**
     * The Mojo will be invoke in a parallel lifecycle, ending at the given phase.
     * <br/>
     * Refer to <code>&#64;execute phase="&lt;phaseName&gt;"</code>.
     * <br/>
     * Refer to <code>&#64;execute lifecycle="&lt;lifecycleId&gt;" phase="&lt;phaseName&gt;"</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String EXECUTE_PHASE = "phase";

    /**
     * Refer to <code>&#64;executionStrategy &lt;strategy&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String EXECUTION_STATEGY = "executionStrategy";

    /**
     * The name for the Mojo that users will reference to execute it.
     * <br/>
     * Refer to <code>&#64;goal &lt;goalName&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String GOAL = "goal";

    /**
     * The Mojo's fully-qualified class name.
     * <br/>
     * Refer to <code>&#64;implementation</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * <br/>
     * <b>Note</b>: Mojo's implementation is auto-detect.
     */
    String IMPLEMENTATION = "implementation";

    /**
     * Allow Mojo inheritance.
     * <br/>
     * Refer to <code>&#64;inheritByDefault &lt;true|false&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String INHERIT_BY_DEFAULT = "inheritByDefault";

    /**
     * Refer to <code>&#64;instantiationStrategy &lt;per-lookup&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String INSTANTIATION_STRATEGY = "instantiationStrategy";

    /**
     * The implementation language for the Mojo.
     * <br/>
     * Refer to <code>&#64;language</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     * <br/>
     * <b>Note</b>: Mojo's implementation is auto-detect.
     */
    String LANGUAGE = "language";

    /**
     * Specifies the execution strategy
     * <br/>
     * Refer to <code>&#64;attainAlways</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String MULTI_EXECUTION_STRATEGY = "attainAlways";

    /**
     * Refer to <code>&#64;phase &lt;phaseName&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String PHASE = "phase";

    /**
     * Flags this Mojo as requiring the dependencies in the specified scope (or an implied scope) to be resolved
     * before it can execute. Currently supports <code>compile</code>, <code>runtime</code>, and
     * <code>test</code> scopes.
     * <br/>
     * Refer to <code>&#64;requiresDependencyResolution &lt;requiredScope&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    /**
     * Flags this Mojo as requiring the dependencies in the specified scope (or an implied scope) to be collected
     * before it can execute. Currently supports <code>compile</code>, <code>runtime</code>, and
     * <code>test</code> scopes.
     * <br/>
     * Refer to <code>&#64;requiresDependencyCollection &lt;requiredScope&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_DEPENDENCY_COLLECTION = "requiresDependencyCollection";


    /**
     * Refer to <code>&#64;requiresDirectInvocation &lt;true|false&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_DIRECT_INVOCATION = "requiresDirectInvocation";

    /**
     * Flags this Mojo to run online.
     * <br/>
     * Refer to <code>&#64;requiresOnline &lt;true|false&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_ONLINE = "requiresOnline";

    /**
     * Flags this Mojo to run inside of a project.
     * <br/>
     * Refer to <code>&#64;requiresProject &lt;true|false&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_PROJECT = "requiresProject";

    /**
     * Flags this Mojo to run inside reports.
     * <br/>
     * Refer to <code>&#64;requiresReports &lt;true|false&gt;</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String REQUIRES_REPORTS = "requiresReports";


    // ----------------------------------------------------------------------
    // Descriptor for fields i.e. parameters
    // ----------------------------------------------------------------------

    /**
     * Populates the field with an instance of a Plexus component. This is like declaring a requirement in a
     * Plexus component.
     * <br/>
     * Refer to <code>&#64;component ...</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String COMPONENT = "component";

    /**
     * Refer to <code>&#64;component role="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String COMPONENT_ROLE = "role";

    /**
     * Refer to <code>&#64;component roleHint="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String COMPONENT_ROLEHINT = "roleHint";

    /**
     * Refer to <code>&#64;parameter ...</code>
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER = "parameter";

    /**
     * This defines an alias which can be used to configure a parameter. This is primarily useful to improve
     * user-friendliness.
     * <br/>
     * Refer to <code>&#64;parameter alias="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER_ALIAS = "alias";

    /**
     * This defines the default value to be injected into this parameter of the Mojo at buildtime.
     * <br/>
     * Refer to <code>&#64;parameter default-value="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER_DEFAULT_VALUE = "default-value";

    /**
     * This defines the expression used to calculate the value to be injected into this parameter of the
     * Mojo at buildtime.
     * <br/>
     * Refer to <code>&#64;parameter expression="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER_EXPRESSION = "expression";

    /**
     * This defines the default implementation in the case the parameter type is an interface.
     * <br/>
     * Refer to <code>&#64;parameter implementation="..."</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER_IMPLEMENTATION = "implementation";

    /**
     * This indicates the base name of the bean properties used to read/write this parameter's value.
     * <br/>
     * Refer to <code>&#64;parameter property="project"</code>
     * <p/>
     * Would say there is a getProject() method and a setProject(Project) method. Here the field
     * name would not be the basis for the parameter's name. This mode of operation will allow the
     * mojos to be usable as beans and will be the promoted form of use.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String PARAMETER_PROPERTY = "property";

    /**
     * Specifies that this parameter cannot be configured directly by the user (as in the case of POM-specified
     * configuration).
     * <br/>
     * Refer to <code>&#64;readonly</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String READONLY = "readonly";

    /**
     * Whether this parameter is required for the Mojo to function
     * <br/>
     * Refer to <code>&#64;required</code>.
     * <br/>
     * <b>Note</b>: Should be defined in a Mojo Field.
     */
    String REQUIRED = "required";

    // ----------------------------------------------------------------------
    // Descriptor for type and fields
    // ----------------------------------------------------------------------

    /**
     * Refer to <code>&#64;since &lt;deprecated-text&gt;</code>
     * <br/>
     * <b>Note</b>: Could be defined in a Mojo Type or a Mojo Field.
     */
    String SINCE = "since";

    /**
     * Refer to <code>&#64;deprecated &lt;since-text&gt;</code>
     * <br/>
     * <b>Note</b>: Could be defined in a Mojo Type or a Mojo Field.
     */
    String DEPRECATED = "deprecated";

    /**
     * Indicates that this mojo is threadsafe and can be run in parallel
     *
     * <b>Note</b>: Should be defined in a Mojo Type.
     */
    String THREADSAFE = "threadSafe";

}
