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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet.Semantics;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.EnhancedParameterWrapper;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.i18n.I18N;

public class GoalRenderer extends AbstractPluginReportRenderer {

    /** Regular expression matching an XHTML link with group 1 = link target, group 2 = link label. */
    private static final Pattern HTML_LINK_PATTERN = Pattern.compile("<a href=\\\"([^\\\"]*)\\\">(.*?)</a>");

    /** The directory where the generated site is written. Used for resolving relative links to javadoc. */
    private final File reportOutputDirectory;

    private final MojoDescriptor descriptor;
    private final boolean disableInternalJavadocLinkValidation;

    private final Log log;

    public GoalRenderer(
            Sink sink,
            I18N i18n,
            Locale locale,
            MavenProject project,
            MojoDescriptor descriptor,
            File reportOutputDirectory,
            boolean disableInternalJavadocLinkValidation,
            Log log) {
        super(sink, locale, i18n, project);
        this.reportOutputDirectory = reportOutputDirectory;
        this.descriptor = descriptor;
        this.disableInternalJavadocLinkValidation = disableInternalJavadocLinkValidation;
        this.log = log;
    }

    @Override
    public String getTitle() {
        return descriptor.getFullGoalName();
    }

    @Override
    protected void renderBody() {
        startSection(descriptor.getFullGoalName());
        renderReportNotice();
        renderDescription("fullname", descriptor.getPluginDescriptor().getId() + ":" + descriptor.getGoal(), false);

        String context = "goal " + descriptor.getGoal();
        if (StringUtils.isNotEmpty(descriptor.getDeprecated())) {
            renderDescription("deprecated", getXhtmlWithValidatedLinks(descriptor.getDeprecated(), context), true);
        }
        if (StringUtils.isNotEmpty(descriptor.getDescription())) {
            renderDescription("description", getXhtmlWithValidatedLinks(descriptor.getDescription(), context), true);
        } else {
            renderDescription("description", getI18nString("nodescription"), false);
        }
        renderAttributes();

        List<Parameter> parameterList = filterParameters(
                descriptor.getParameters() != null ? descriptor.getParameters() : Collections.emptyList());
        if (parameterList.isEmpty()) {
            startSection(getI18nString("parameters"));
            sink.paragraph();
            sink.text(getI18nString("noParameter"));
            sink.paragraph_();
            endSection();
        } else {
            renderParameterOverviewTable(
                    getI18nString("requiredParameters"),
                    parameterList.stream().filter(Parameter::isRequired).iterator());
            renderParameterOverviewTable(
                    getI18nString("optionalParameters"),
                    parameterList.stream().filter(p -> !p.isRequired()).iterator());
            renderParameterDetails(parameterList.iterator());
        }
        endSection();
    }

    /** Filter parameters to only retain those which must be documented, i.e. neither components nor read-only ones.
     *
     * @param parameterList not null
     * @return the parameters list without components. */
    private static List<Parameter> filterParameters(Collection<Parameter> parameterList) {
        return parameterList.stream()
                .filter(p -> p.isEditable()
                        && (p.getExpression() == null || !p.getExpression().startsWith("${component.")))
                .collect(Collectors.toList());
    }

    private void renderReportNotice() {
        if (PluginUtils.isMavenReport(descriptor.getImplementation(), project)) {
            renderDescription("notice.prefix", getI18nString("notice.isMavenReport"), false);
        }
    }

    /**
     * A description consists of a term/prefix and the actual description text
     */
    private void renderDescription(String prefixKey, String description, boolean isHtmlMarkup) {
        // TODO: convert to dt and dd elements
        renderDescriptionPrefix(prefixKey);
        sink.paragraph();
        if (isHtmlMarkup) {
            sink.rawText(description);
        } else {
            sink.text(description);
        }
        sink.paragraph_(); // p
    }

    private void renderDescriptionPrefix(String prefixKey) {
        sink.paragraph();
        sink.inline(Semantics.STRONG);
        sink.text(getI18nString(prefixKey));
        sink.inline_();
        sink.text(":");
        sink.paragraph_();
    }

    @SuppressWarnings("deprecation")
    private void renderAttributes() {
        renderDescriptionPrefix("attributes");
        sink.list();

        renderAttribute(descriptor.isProjectRequired(), "projectRequired");
        renderAttribute(descriptor.isRequiresReports(), "reportingMojo");
        renderAttribute(descriptor.isAggregator(), "aggregator");
        renderAttribute(descriptor.isDirectInvocationOnly(), "directInvocationOnly");
        renderAttribute(descriptor.isDependencyResolutionRequired(), "dependencyResolutionRequired");

        if (descriptor instanceof ExtendedMojoDescriptor) {
            ExtendedMojoDescriptor extendedDescriptor = (ExtendedMojoDescriptor) descriptor;
            renderAttribute(extendedDescriptor.getDependencyCollectionRequired(), "dependencyCollectionRequired");
        }

        renderAttribute(descriptor.isThreadSafe(), "threadSafe");
        renderAttribute(!descriptor.isThreadSafe(), "notThreadSafe");
        renderAttribute(descriptor.getSince(), "since");
        renderAttribute(descriptor.getPhase(), "phase");
        renderAttribute(descriptor.getExecutePhase(), "executePhase");
        renderAttribute(descriptor.getExecuteGoal(), "executeGoal");
        renderAttribute(descriptor.getExecuteLifecycle(), "executeLifecycle");
        renderAttribute(descriptor.isOnlineRequired(), "onlineRequired");
        renderAttribute(!descriptor.isInheritedByDefault(), "notInheritedByDefault");

        sink.list_();
    }

    private void renderAttribute(boolean condition, String attributeKey) {
        renderAttribute(condition, attributeKey, Optional.empty());
    }

    private void renderAttribute(String conditionAndCodeArgument, String attributeKey) {
        renderAttribute(
                StringUtils.isNotEmpty(conditionAndCodeArgument),
                attributeKey,
                Optional.ofNullable(conditionAndCodeArgument));
    }

    private void renderAttribute(boolean condition, String attributeKey, Optional<String> codeArgument) {
        if (condition) {
            sink.listItem();
            linkPatternedText(getI18nString(attributeKey));
            if (codeArgument.isPresent()) {
                text(": ");
                sink.inline(Semantics.CODE);
                sink.text(codeArgument.get());
                sink.inline_();
            }
            text(".");
            sink.listItem_();
        }
    }

    private void renderParameterOverviewTable(String title, Iterator<Parameter> parameters) {
        // don't emit empty tables
        if (!parameters.hasNext()) {
            return;
        }
        startSection(title);
        startTable();
        tableHeader(new String[] {
            getI18nString("parameter.name.header"),
            getI18nString("parameter.type.header"),
            getI18nString("parameter.since.header"),
            getI18nString("parameter.description.header")
        });
        while (parameters.hasNext()) {
            renderParameterOverviewTableRow(parameters.next());
        }
        endTable();
        endSection();
    }

    private void renderTableCellWithCode(String text) {
        renderTableCellWithCode(text, Optional.empty());
    }

    private void renderTableCellWithCode(String text, Optional<String> link) {
        sink.tableCell();
        if (link.isPresent()) {
            sink.link(link.get(), null);
        }
        sink.inline(Semantics.CODE);
        sink.text(text);
        sink.inline_();
        if (link.isPresent()) {
            sink.link_();
        }
        sink.tableCell_();
    }

    private void renderParameterOverviewTableRow(Parameter parameter) {
        sink.tableRow();
        // name
        // link to appropriate section
        renderTableCellWithCode(
                format("parameter.name", parameter.getName()),
                // no need for additional URI encoding as it returns only URI safe characters
                Optional.of("#" + DoxiaUtils.encodeId(parameter.getName())));

        // type
        Map.Entry<String, Optional<String>> type = getLinkedType(parameter, true);
        renderTableCellWithCode(type.getKey(), type.getValue());

        // since
        String since = StringUtils.defaultIfEmpty(parameter.getSince(), "-");
        renderTableCellWithCode(since);

        // description
        sink.tableCell();
        String description;
        String context = "Parameter " + parameter.getName() + " in goal " + descriptor.getGoal();
        renderDeprecatedParameterDescription(parameter.getDeprecated(), context);
        if (StringUtils.isNotEmpty(parameter.getDescription())) {
            description = getXhtmlWithValidatedLinks(parameter.getDescription(), context);
        } else {
            description = getI18nString("nodescription");
        }
        sink.rawText(description);
        renderTableCellDetail("parameter.defaultValue", parameter.getDefaultValue());
        renderTableCellDetail("parameter.property", getPropertyFromExpression(parameter.getExpression()));
        renderTableCellDetail("parameter.alias", parameter.getAlias());
        sink.tableCell_();

        sink.tableRow_();
    }

    private void renderParameterDetails(Iterator<Parameter> parameters) {

        startSection(getI18nString("parameter.details"));

        while (parameters.hasNext()) {
            Parameter parameter = parameters.next();
            // deprecated anchor for backwards-compatibility with XDoc (upper and lower case)
            // TODO: replace once migrated to Doxia 2.x with two-arg startSection(String, String) method
            sink.anchor(parameter.getName());
            sink.anchor_();

            startSection(format("parameter.name", parameter.getName()));
            String context = "Parameter " + parameter.getName() + " in goal " + descriptor.getGoal();
            renderDeprecatedParameterDescription(parameter.getDeprecated(), context);
            sink.division();
            if (StringUtils.isNotEmpty(parameter.getDescription())) {
                sink.rawText(getXhtmlWithValidatedLinks(parameter.getDescription(), context));
            } else {
                sink.text(getI18nString("nodescription"));
            }
            sink.division_();

            sink.list();
            Map.Entry<String, Optional<String>> typeAndLink = getLinkedType(parameter, false);
            renderDetail(getI18nString("parameter.type"), typeAndLink.getKey(), typeAndLink.getValue());

            if (StringUtils.isNotEmpty(parameter.getSince())) {
                renderDetail(getI18nString("parameter.since"), parameter.getSince());
            }

            if (parameter.isRequired()) {
                renderDetail(getI18nString("parameter.required"), getI18nString("yes"));
            } else {
                renderDetail(getI18nString("parameter.required"), getI18nString("no"));
            }

            String expression = parameter.getExpression();
            String property = getPropertyFromExpression(expression);
            if (property == null) {
                renderDetail(getI18nString("parameter.expression"), expression);
            } else {
                renderDetail(getI18nString("parameter.property"), property);
            }

            renderDetail(getI18nString("parameter.defaultValue"), parameter.getDefaultValue());

            renderDetail(getI18nString("parameter.alias"), parameter.getAlias());

            sink.list_(); // ul

            if (parameters.hasNext()) {
                sink.horizontalRule();
            }
            endSection();
        }
        endSection();
    }

    private void renderDeprecatedParameterDescription(String deprecated, String context) {
        if (StringUtils.isNotEmpty(deprecated)) {
            String deprecatedXhtml = getXhtmlWithValidatedLinks(deprecated, context);
            sink.division();
            sink.inline(Semantics.STRONG);
            sink.text(getI18nString("parameter.deprecated"));
            sink.inline_();
            sink.lineBreak();
            sink.rawText(deprecatedXhtml);
            sink.division_();
            sink.lineBreak();
        }
    }

    private void renderTableCellDetail(String nameKey, String value) {
        if (StringUtils.isNotEmpty(value)) {
            sink.lineBreak();
            sink.inline(Semantics.STRONG);
            sink.text(getI18nString(nameKey));
            sink.inline_();
            sink.text(": ");
            sink.inline(Semantics.CODE);
            sink.text(value);
            sink.inline_();
        }
    }

    private void renderDetail(String param, String value) {
        renderDetail(param, value, Optional.empty());
    }

    private void renderDetail(String param, String value, Optional<String> valueLink) {
        if (value != null && !value.isEmpty()) {
            sink.listItem();
            sink.inline(Semantics.STRONG);
            sink.text(param);
            sink.inline_();
            sink.text(": ");
            if (valueLink.isPresent()) {
                sink.link(valueLink.get());
            }
            sink.inline(Semantics.CODE);
            sink.text(value);
            sink.inline_();
            if (valueLink.isPresent()) {
                sink.link_();
            }
            sink.listItem_();
        }
    }

    private static String getPropertyFromExpression(String expression) {
        if ((expression != null && !expression.isEmpty())
                && expression.startsWith("${")
                && expression.endsWith("}")
                && !expression.substring(2).contains("${")) {
            // expression="${xxx}" -> property="xxx"
            return expression.substring(2, expression.length() - 1);
        }
        // no property can be extracted
        return null;
    }

    static String getShortType(String type) {
        // split into type arguments and main type
        int startTypeArguments = type.indexOf('<');
        if (startTypeArguments == -1) {
            return getShortTypeOfSimpleType(type);
        } else {
            StringBuilder shortType = new StringBuilder();
            shortType.append(getShortTypeOfSimpleType(type.substring(0, startTypeArguments)));
            shortType
                    .append("<")
                    .append(getShortTypeOfTypeArgument(type.substring(startTypeArguments + 1, type.lastIndexOf(">"))))
                    .append(">");
            return shortType.toString();
        }
    }

    private static String getShortTypeOfTypeArgument(String type) {
        String[] typeArguments = type.split(",\\s*");
        StringBuilder shortType = new StringBuilder();
        for (int i = 0; i < typeArguments.length; i++) {
            String typeArgument = typeArguments[i];
            if (typeArgument.contains("<")) {
                // nested type arguments lead to ellipsis
                return "...";
            } else {
                shortType.append(getShortTypeOfSimpleType(typeArgument));
                if (i < typeArguments.length - 1) {
                    shortType.append(",");
                }
            }
        }
        return shortType.toString();
    }

    private static String getShortTypeOfSimpleType(String type) {
        int index = type.lastIndexOf('.');
        return type.substring(index + 1);
    }

    private Map.Entry<String, Optional<String>> getLinkedType(Parameter parameter, boolean isShortType) {
        final String typeValue;
        if (isShortType) {
            typeValue = getShortType(parameter.getType());
        } else {
            typeValue = parameter.getType();
        }
        URI uri = null;
        if (parameter instanceof EnhancedParameterWrapper) {
            EnhancedParameterWrapper enhancedParameter = (EnhancedParameterWrapper) parameter;
            if (enhancedParameter.getTypeJavadocUrl() != null) {
                URI javadocUrl = enhancedParameter.getTypeJavadocUrl();
                // optionally check if link is valid
                if (javadocUrl.isAbsolute()
                        || disableInternalJavadocLinkValidation
                        || JavadocLinkGenerator.isLinkValid(javadocUrl, reportOutputDirectory.toPath())) {
                    uri = enhancedParameter.getTypeJavadocUrl();
                }
            }
        }
        // rely on the encoded URI
        return new SimpleEntry<>(typeValue, Optional.ofNullable(uri).map(URI::toASCIIString));
    }

    String getXhtmlWithValidatedLinks(String xhtmlText, String context) {
        if (disableInternalJavadocLinkValidation) {
            return xhtmlText;
        }
        StringBuffer sanitizedXhtmlText = new StringBuffer();
        // find all links which are not absolute
        Matcher matcher = HTML_LINK_PATTERN.matcher(xhtmlText);
        while (matcher.find()) {
            URI link;
            try {
                link = new URI(matcher.group(1));
                if (!link.isAbsolute() && !JavadocLinkGenerator.isLinkValid(link, reportOutputDirectory.toPath())) {
                    matcher.appendReplacement(sanitizedXhtmlText, matcher.group(2));
                    log.debug(String.format("Removed invalid link %s in %s", link, context));
                } else {
                    matcher.appendReplacement(sanitizedXhtmlText, matcher.group(0));
                }
            } catch (URISyntaxException e) {
                log.warn(String.format(
                        "Invalid URI %s found in %s. Cannot validate, leave untouched", matcher.group(1), context));
                matcher.appendReplacement(sanitizedXhtmlText, matcher.group(0));
            }
        }
        matcher.appendTail(sanitizedXhtmlText);
        return sanitizedXhtmlText.toString();
    }

    /** Convenience method.
     *
     * @param key  not null
     * @param arg1 not null
     * @return Localized, formatted text identified by <code>key</code>.
     * @see #format(String, Object[]) */
    private String format(String key, Object arg1) {
        return format(key, new Object[] {arg1});
    }

    /** Looks up the value for <code>key</code> in the <code>ResourceBundle</code>, then formats that value for the specified
     * <code>Locale</code> using <code>args</code>.
     *
     * @param key  not null
     * @param args not null
     * @return Localized, formatted text identified by <code>key</code>. */
    private String format(String key, Object[] args) {
        String pattern = getI18nString(key);
        // we don't need quoting so spare us the confusion in the resource bundle to double them up in some keys
        pattern = StringUtils.replace(pattern, "'", "''");

        MessageFormat messageFormat = new MessageFormat(pattern, locale);
        return messageFormat.format(args);
    }

    @Override
    protected String getI18nSection() {
        return "plugin.goal";
    }
}
