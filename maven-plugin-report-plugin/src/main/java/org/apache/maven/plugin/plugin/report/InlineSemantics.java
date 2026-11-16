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

import javax.swing.text.SimpleAttributeSet;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.doxia.sink.SinkEventAttributes;

/**
 * Inline semantics attributes that only depend on the Sink API.
 * <p>
 * Doxia 2.2.0 moved {@code SinkEventAttributeSet} and its {@code Semantics} constants to doxia-sink-api. The report
 * runs against the Sink API of the site plugin, so referencing those constants fails with
 * {@code NoClassDefFoundError} under a site plugin that provides an older Sink API. Each call returns a new set,
 * as sinks may copy and change it.
 */
final class InlineSemantics extends SimpleAttributeSet implements SinkEventAttributes {

    private static final long serialVersionUID = 1L;

    private InlineSemantics(String semantics) {
        addAttribute(SEMANTICS, semantics);
    }

    /**
     * @return a new attribute set for {@code strong} semantics
     */
    static SinkEventAttributes strong() {
        return new InlineSemantics("strong");
    }

    /**
     * @return a new attribute set for {@code code} semantics
     */
    static SinkEventAttributes code() {
        return new InlineSemantics("code");
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String, Object>> entries = new LinkedHashSet<>();
        Enumeration<?> names = getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            entries.add(new AbstractMap.SimpleImmutableEntry<>(name.toString(), getAttribute(name)));
        }
        return Collections.unmodifiableSet(entries);
    }
}
