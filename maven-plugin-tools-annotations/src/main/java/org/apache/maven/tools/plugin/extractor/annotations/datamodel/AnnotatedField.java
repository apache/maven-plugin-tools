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
 * @author Olivier Lamy
 * @since 3.0
 */
public class AnnotatedField extends AnnotatedContent implements Comparable<AnnotatedField> {
    private String fieldName;

    public AnnotatedField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String name) {
        this.fieldName = name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AnnotatedField");
        sb.append("{fieldName='").append(fieldName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(AnnotatedField annotatedField) {
        return getFieldName().compareTo(annotatedField.getFieldName());
    }
}
