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
package org.apache.maven.tools.plugin.extractor.annotations.datamodel;

import java.lang.annotation.Annotation;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class ExecuteAnnotationContent implements Execute {
    private String goal;

    private String lifecycle;

    private LifecyclePhase phase;

    private String customPhase;

    @Override
    public LifecyclePhase phase() {
        return this.phase;
    }

    @Override
    public String customPhase() {
        return customPhase;
    }

    @Override
    public String goal() {
        return this.goal;
    }

    @Override
    public String lifecycle() {
        return this.lifecycle;
    }

    public void phase(String phase) {
        this.phase = LifecyclePhase.valueOf(phase);
    }

    public void customPhase(String customPhase) {
        this.customPhase = customPhase;
    }

    public void goal(String goal) {
        this.goal = goal;
    }

    public void lifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ExecuteAnnotationContent");
        sb.append("{goal='").append(goal).append('\'');
        sb.append(", lifecycle='").append(lifecycle).append('\'');
        sb.append(", phase=").append(phase);
        sb.append(", customPhase=").append(customPhase);
        sb.append('}');
        return sb.toString();
    }
}
