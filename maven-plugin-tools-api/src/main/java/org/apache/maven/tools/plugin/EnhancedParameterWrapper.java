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
package org.apache.maven.tools.plugin;

import java.net.URI;

import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.Requirement;

/**
 * Wrapper around regular {@link Parameter} which adds capability to
 * read/write a type javadoc URL
 */
public class EnhancedParameterWrapper extends Parameter {
    private final Parameter delegate;
    private URI typeJavadocUrl;

    public EnhancedParameterWrapper(Parameter delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public void setType(String type) {
        delegate.setType(type);
    }

    @Override
    public boolean isRequired() {
        return delegate.isRequired();
    }

    @Override
    public void setRequired(boolean required) {
        delegate.setRequired(required);
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public String getExpression() {
        return delegate.getExpression();
    }

    @Override
    public void setExpression(String expression) {
        delegate.setExpression(expression);
    }

    @Override
    public String getDeprecated() {
        return delegate.getDeprecated();
    }

    @Override
    public void setDeprecated(String deprecated) {
        delegate.setDeprecated(deprecated);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return delegate.equals(other);
    }

    @Override
    public String getAlias() {
        return delegate.getAlias();
    }

    @Override
    public void setAlias(String alias) {
        delegate.setAlias(alias);
    }

    @Override
    public boolean isEditable() {
        return delegate.isEditable();
    }

    @Override
    public void setEditable(boolean editable) {
        delegate.setEditable(editable);
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        delegate.setDefaultValue(defaultValue);
    }

    @Override
    public String getDefaultValue() {
        return delegate.getDefaultValue();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Requirement getRequirement() {
        return delegate.getRequirement();
    }

    @Override
    public void setRequirement(Requirement requirement) {
        delegate.setRequirement(requirement);
    }

    @Override
    public String getImplementation() {
        return delegate.getImplementation();
    }

    @Override
    public void setImplementation(String implementation) {
        delegate.setImplementation(implementation);
    }

    @Override
    public String getSince() {
        return delegate.getSince();
    }

    @Override
    public void setSince(String since) {
        delegate.setSince(since);
    }

    @Override
    public Parameter clone() {
        return delegate.clone();
    }

    public URI getTypeJavadocUrl() {
        return typeJavadocUrl;
    }

    public void setTypeJavadocUrl(URI typeJavadocUrl) {
        this.typeJavadocUrl = typeJavadocUrl;
    }
}
