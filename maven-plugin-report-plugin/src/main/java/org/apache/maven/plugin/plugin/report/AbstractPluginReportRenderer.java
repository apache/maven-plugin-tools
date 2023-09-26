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

import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

/** Base class for all reports generated by the plugin report plugin. */
public abstract class AbstractPluginReportRenderer extends AbstractMavenReportRenderer {

    private static final String RESOURCE_BASENAME = "plugin-report";

    private final I18N i18n;

    protected final Locale locale;

    protected final MavenProject project;

    protected AbstractPluginReportRenderer(Sink sink, Locale locale, I18N i18n, MavenProject project) {
        super(sink);
        this.i18n = i18n;
        this.locale = locale;
        this.project = project;
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * Returns
     * @param key The key .
     * @return The translated string.
     */
    protected String getI18nString(String key) {
        return getI18nString(getI18nSection(), key);
    }

    /**
     * @param section The section.
     * @param key The key to translate.
     * @return the translated key.
     */
    protected String getI18nString(String section, String key) {
        return i18n.getString(RESOURCE_BASENAME, locale, "report." + section + '.' + key);
    }

    /**
     *
     * @return the key prefix to be used with every key. Is prepended by {@code report.}.
     */
    protected abstract String getI18nSection();
}
