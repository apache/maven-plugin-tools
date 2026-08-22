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

/**
 * Holds scanned data from a {@code @After} annotation
 * ({@code org.apache.maven.api.plugin.annotations.After}).
 * <p>
 * The {@code @After} annotation declares that the annotated mojo must run
 * after another phase has completed.  It carries three attributes:
 * <ul>
 *   <li>{@code phase}  &ndash; the phase this mojo must run after</li>
 *   <li>{@code type}   &ndash; the scope type ({@code PROJECT}, {@code DEPENDENCIES}, {@code CHILDREN})</li>
 *   <li>{@code scope}  &ndash; the dependency scope</li>
 * </ul>
 * <p>
 * Setter method names match the annotation attribute names so that
 * {@link org.apache.maven.tools.plugin.extractor.annotations.scanner.DefaultMojoAnnotationsScanner#populateAnnotationContent}
 * can fill them via reflection.
 *
 * @since 4.0.0
 */
public class AfterAnnotationContent {
    private String phase;

    private String type;

    private String scope;

    public String phase() {
        return this.phase;
    }

    public void phase(String phase) {
        this.phase = phase;
    }

    public String type() {
        return this.type;
    }

    public void type(String type) {
        this.type = type;
    }

    public String scope() {
        return this.scope;
    }

    public void scope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AfterAnnotationContent");
        sb.append("{phase='").append(phase).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
