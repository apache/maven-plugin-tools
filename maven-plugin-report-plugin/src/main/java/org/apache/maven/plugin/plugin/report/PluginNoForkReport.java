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
package org.apache.maven.plugin.plugin.report;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generates the plugin's report: the plugin details page at <code>plugin-info.html</code>,
 * and one <code><i>goal</i>-goal.html</code> per goal.
 * Relies on one output file from <a href="../maven-plugin-plugin/descriptor-goal.html">plugin:descriptor</a>.
 *
 * @since 3.14.0
 */
@Mojo(name = "report-no-fork", threadSafe = true)
@Execute(phase = LifecyclePhase.NONE)
public class PluginNoForkReport extends PluginReport {}
