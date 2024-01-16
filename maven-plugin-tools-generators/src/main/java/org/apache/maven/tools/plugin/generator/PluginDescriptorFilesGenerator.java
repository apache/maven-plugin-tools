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
package org.apache.maven.tools.plugin.generator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.ExtendedPluginDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.io.CachingOutputStream;
import org.codehaus.stax2.util.StreamWriterDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Serializes
 * <ol>
 * <li>a standard <a href="/ref/current/maven-plugin-api/plugin.html">Maven Plugin Descriptor XML file</a></li>
 * <li>a descriptor containing a limited set of elements for {@link PluginHelpGenerator}</li>
 * <li>an enhanced descriptor containing HTML values for some elements (instead of plain text as for the other two)
 * for {@code org.apache.maven.plugin.plugin.report.GoalRenderer}</li>
 * </ol>
 * from a given in-memory descriptor. The in-memory descriptor acting as source is supposed to contain XHTML values
 * for description elements.
 *
 */
public class PluginDescriptorFilesGenerator implements Generator {
    private static final Logger LOG = LoggerFactory.getLogger(PluginDescriptorFilesGenerator.class);

    /**
     * The type of the plugin descriptor file
     */
    enum DescriptorType {
        STANDARD,
        LIMITED_FOR_HELP_MOJO,
        XHTML
    }

    @Override
    public void execute(File destinationDirectory, PluginToolsRequest request) throws GeneratorException {
        try {
            // write standard plugin.xml descriptor
            File f = new File(destinationDirectory, "plugin.xml");
            writeDescriptor(f, request, DescriptorType.STANDARD);

            // write plugin-help.xml help-descriptor (containing only a limited set of attributes)
            MavenProject mavenProject = request.getProject();
            f = new File(destinationDirectory, PluginHelpGenerator.getPluginHelpPath(mavenProject));
            writeDescriptor(f, request, DescriptorType.LIMITED_FOR_HELP_MOJO);

            // write enhanced plugin-enhanced.xml descriptor (containing some XHTML values)
            f = getEnhancedDescriptorFilePath(mavenProject);
            writeDescriptor(f, request, DescriptorType.XHTML);
        } catch (IOException | XMLStreamException e) {
            throw new GeneratorException(e.getMessage(), e);
        }
    }

    public static File getEnhancedDescriptorFilePath(MavenProject project) {
        return new File(project.getBuild().getDirectory(), "plugin-enhanced.xml");
    }

    private String getVersion() {
        Package p = this.getClass().getPackage();
        String version = (p == null) ? null : p.getSpecificationVersion();
        return (version == null) ? "SNAPSHOT" : version;
    }

    public void writeDescriptor(File destinationFile, PluginToolsRequest request, DescriptorType type)
            throws IOException, XMLStreamException {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();

        String apiVersion = request.getPluginDescriptor().getRequiredMavenVersion();
        boolean isV4 = apiVersion != null && apiVersion.startsWith("4.");
        String namespace = isV4 ? "http://maven.apache.org/PLUGIN/2.0.0" : null;
        String location = isV4 ? "https://maven.apache.org/xsd/plugin-2.0.0-alpha-10.xsd" : null;

        if (!destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }

        try (Writer writer = new OutputStreamWriter(new CachingOutputStream(destinationFile), UTF_8)) {
            XMLOutputFactory factory = new com.ctc.wstx.stax.WstxOutputFactory();
            factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
            factory.setProperty(com.ctc.wstx.api.WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
            factory.setProperty(com.ctc.wstx.api.WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
            XMLStreamWriter w = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(writer));
            w.writeStartDocument(UTF_8.name(), null);

            final String additionalInfo;
            switch (type) {
                case LIMITED_FOR_HELP_MOJO:
                    additionalInfo = " (for help mojo with limited elements)";
                    break;
                case XHTML:
                    additionalInfo = " (enhanced XHTML version (used for plugin:report))";
                    break;
                default:
                    additionalInfo = "";
                    break;
            }
            w.writeCharacters("\n");
            w.writeComment(" Generated by maven-plugin-tools " + getVersion() + additionalInfo + " ");

            if (isV4) {
                w.writeStartElement("", "plugin", namespace);
                // xmlns="http://maven.apache.org/PLUGIN/2.0.0"
                // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                // xsi:schemaLocation="http://maven.apache.org/PLUGIN/2.0.0
                // https://maven.apache.org/xsd/plugin-2.0.0-alpha-9.xsd"
                w.writeNamespace("", namespace);
                w.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
                w.writeAttribute(
                        "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", namespace + " " + location);
            } else {
                w.writeStartElement("plugin");
            }
            GeneratorUtils.element(w, "name", pluginDescriptor.getName());
            GeneratorUtils.element(w, "description", pluginDescriptor.getDescription());
            GeneratorUtils.element(w, "groupId", pluginDescriptor.getGroupId());
            GeneratorUtils.element(w, "artifactId", pluginDescriptor.getArtifactId());
            GeneratorUtils.element(w, "version", pluginDescriptor.getVersion());
            GeneratorUtils.element(w, "goalPrefix", pluginDescriptor.getGoalPrefix());

            if (type != DescriptorType.LIMITED_FOR_HELP_MOJO) {
                GeneratorUtils.element(w, "isolatedRealm", String.valueOf(pluginDescriptor.isIsolatedRealm()));
                GeneratorUtils.element(
                        w, "inheritedByDefault", String.valueOf(pluginDescriptor.isInheritedByDefault()));
                if (pluginDescriptor instanceof ExtendedPluginDescriptor) {
                    ExtendedPluginDescriptor extPluginDescriptor = (ExtendedPluginDescriptor) pluginDescriptor;
                    if (StringUtils.isNotBlank(extPluginDescriptor.getRequiredJavaVersion())) {
                        GeneratorUtils.element(w, "requiredJavaVersion", extPluginDescriptor.getRequiredJavaVersion());
                    }
                }
                if (StringUtils.isNotBlank(pluginDescriptor.getRequiredMavenVersion())) {
                    GeneratorUtils.element(w, "requiredMavenVersion", pluginDescriptor.getRequiredMavenVersion());
                }
            }

            w.writeStartElement("mojos");

            final JavadocLinkGenerator javadocLinkGenerator;
            if (request.getInternalJavadocBaseUrl() != null
                    || (request.getExternalJavadocBaseUrls() != null
                            && !request.getExternalJavadocBaseUrls().isEmpty())) {
                javadocLinkGenerator = new JavadocLinkGenerator(
                        request.getInternalJavadocBaseUrl(),
                        request.getInternalJavadocVersion(),
                        request.getExternalJavadocBaseUrls(),
                        request.getSettings());
            } else {
                javadocLinkGenerator = null;
            }
            if (pluginDescriptor.getMojos() != null) {
                List<MojoDescriptor> descriptors = pluginDescriptor.getMojos();
                PluginUtils.sortMojos(descriptors);
                for (MojoDescriptor descriptor : descriptors) {
                    processMojoDescriptor(descriptor, w, type, javadocLinkGenerator, isV4);
                }
            }

            w.writeEndElement();
            if (type != DescriptorType.LIMITED_FOR_HELP_MOJO && !isV4) {
                GeneratorUtils.writeDependencies(w, pluginDescriptor);
            }
            w.writeEndElement();
            w.close();
            writer.flush();
        }
    }

    /**
     *
     * @param type
     * @param containsXhtmlValue
     * @param text
     * @return the normalized text value (i.e. potentially converted to XHTML)
     */
    private static String getTextValue(DescriptorType type, boolean containsXhtmlValue, String text) {
        final String xhtmlText;
        if (!containsXhtmlValue) // text comes from legacy extractor
        {
            xhtmlText = GeneratorUtils.makeHtmlValid(text);
        } else {
            xhtmlText = text;
        }
        if (type != DescriptorType.XHTML) {
            return new HtmlToPlainTextConverter().convert(text);
        } else {
            return xhtmlText;
        }
    }

    @SuppressWarnings("deprecation")
    protected void processMojoDescriptor(
            MojoDescriptor mojoDescriptor,
            XMLStreamWriter w,
            DescriptorType type,
            JavadocLinkGenerator javadocLinkGenerator,
            boolean isV4)
            throws XMLStreamException {
        boolean containsXhtmlTextValues = mojoDescriptor instanceof ExtendedMojoDescriptor
                && ((ExtendedMojoDescriptor) mojoDescriptor).containsXhtmlTextValues();

        w.writeStartElement("mojo");

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.writeStartElement("goal");
        w.writeCharacters(mojoDescriptor.getGoal());
        w.writeEndElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String description = mojoDescriptor.getDescription();

        if (description != null && !description.isEmpty()) {
            w.writeStartElement("description");
            w.writeCharacters(getTextValue(type, containsXhtmlTextValues, mojoDescriptor.getDescription()));
            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(mojoDescriptor.isDependencyResolutionRequired())) {
            GeneratorUtils.element(
                    w,
                    isV4 ? "dependencyResolution" : "requiresDependencyResolution",
                    mojoDescriptor.isDependencyResolutionRequired());
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------
        if (!isV4) {
            GeneratorUtils.element(
                    w, "requiresDirectInvocation", String.valueOf(mojoDescriptor.isDirectInvocationOnly()));
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element(
                w, isV4 ? "projectRequired" : "requiresProject", String.valueOf(mojoDescriptor.isProjectRequired()));

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (!isV4) {
            GeneratorUtils.element(w, "requiresReports", String.valueOf(mojoDescriptor.isRequiresReports()));
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element(w, "aggregator", String.valueOf(mojoDescriptor.isAggregator()));

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element(
                w, isV4 ? "onlineRequired" : "requiresOnline", String.valueOf(mojoDescriptor.isOnlineRequired()));

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element(w, "inheritedByDefault", String.valueOf(mojoDescriptor.isInheritedByDefault()));

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(mojoDescriptor.getPhase())) {
            GeneratorUtils.element(w, "phase", mojoDescriptor.getPhase());
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(mojoDescriptor.getExecutePhase())) {
            GeneratorUtils.element(w, "executePhase", mojoDescriptor.getExecutePhase());
        }

        if (StringUtils.isNotEmpty(mojoDescriptor.getExecuteGoal())) {
            GeneratorUtils.element(w, "executeGoal", mojoDescriptor.getExecuteGoal());
        }

        if (StringUtils.isNotEmpty(mojoDescriptor.getExecuteLifecycle())) {
            GeneratorUtils.element(w, "executeLifecycle", mojoDescriptor.getExecuteLifecycle());
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.writeStartElement("implementation");
        w.writeCharacters(mojoDescriptor.getImplementation());
        w.writeEndElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.writeStartElement("language");
        w.writeCharacters(mojoDescriptor.getLanguage());
        w.writeEndElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(mojoDescriptor.getComponentConfigurator())) {
            w.writeStartElement("configurator");
            w.writeCharacters(mojoDescriptor.getComponentConfigurator());
            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(mojoDescriptor.getComponentComposer())) {
            w.writeStartElement("composer");
            w.writeCharacters(mojoDescriptor.getComponentComposer());
            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (!isV4) {
            w.writeStartElement("instantiationStrategy");
            w.writeCharacters(mojoDescriptor.getInstantiationStrategy());
            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        // Strategy for handling repeated reference to mojo in
        // the calculated (decorated, resolved) execution stack
        // ----------------------------------------------------------------------
        if (!isV4) {
            w.writeStartElement("executionStrategy");
            w.writeCharacters(mojoDescriptor.getExecutionStrategy());
            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (mojoDescriptor.getSince() != null) {
            w.writeStartElement("since");

            if (StringUtils.isEmpty(mojoDescriptor.getSince())) {
                w.writeCharacters("No version given");
            } else {
                w.writeCharacters(mojoDescriptor.getSince());
            }

            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if (mojoDescriptor.getDeprecated() != null) {
            w.writeStartElement("deprecated");

            if (StringUtils.isEmpty(mojoDescriptor.getDeprecated())) {
                w.writeCharacters("No reason given");
            } else {
                w.writeCharacters(getTextValue(type, containsXhtmlTextValues, mojoDescriptor.getDeprecated()));
            }

            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        // Extended (3.0) descriptor
        // ----------------------------------------------------------------------

        if (mojoDescriptor instanceof ExtendedMojoDescriptor) {
            ExtendedMojoDescriptor extendedMojoDescriptor = (ExtendedMojoDescriptor) mojoDescriptor;
            if (extendedMojoDescriptor.getDependencyCollectionRequired() != null) {
                GeneratorUtils.element(
                        w,
                        isV4 ? "dependencyCollection" : "requiresDependencyCollection",
                        extendedMojoDescriptor.getDependencyCollectionRequired());
            }

            if (!isV4) {
                GeneratorUtils.element(w, "threadSafe", String.valueOf(extendedMojoDescriptor.isThreadSafe()));
            }
        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        List<Parameter> parameters = mojoDescriptor.getParameters();

        w.writeStartElement("parameters");

        Map<String, Requirement> requirements = new LinkedHashMap<>();

        Set<Parameter> configuration = new LinkedHashSet<>();

        if (parameters != null) {
            if (type == DescriptorType.LIMITED_FOR_HELP_MOJO) {
                PluginUtils.sortMojoParameters(parameters);
            }

            for (Parameter parameter : parameters) {
                String expression = getExpression(parameter);

                if ((expression != null && !expression.isEmpty()) && expression.startsWith("${component.")) {
                    // treat it as a component...a requirement, in other words.

                    // remove "component." plus expression delimiters
                    String role = expression.substring("${component.".length(), expression.length() - 1);

                    String roleHint = null;

                    int posRoleHintSeparator = role.indexOf('#');
                    if (posRoleHintSeparator > 0) {
                        roleHint = role.substring(posRoleHintSeparator + 1);

                        role = role.substring(0, posRoleHintSeparator);
                    }

                    // TODO: remove deprecated expression
                    requirements.put(parameter.getName(), new Requirement(role, roleHint));
                } else if (parameter.getRequirement() != null) {
                    requirements.put(parameter.getName(), parameter.getRequirement());
                }
                // don't show readonly parameters in help
                else if (type != DescriptorType.LIMITED_FOR_HELP_MOJO || parameter.isEditable()) {
                    // treat it as a normal parameter.

                    w.writeStartElement("parameter");

                    GeneratorUtils.element(w, "name", parameter.getName());

                    if (parameter.getAlias() != null) {
                        GeneratorUtils.element(w, "alias", parameter.getAlias());
                    }

                    if (isV4 && type == DescriptorType.STANDARD) {
                        GeneratorUtils.element(w, "type", parameter.getType());
                    } else {
                        writeParameterType(w, type, javadocLinkGenerator, parameter, mojoDescriptor.getGoal());
                    }

                    if (parameter.getSince() != null) {
                        w.writeStartElement("since");

                        if (StringUtils.isEmpty(parameter.getSince())) {
                            w.writeCharacters("No version given");
                        } else {
                            w.writeCharacters(parameter.getSince());
                        }

                        w.writeEndElement();
                    }

                    if (parameter.getDeprecated() != null) {
                        if (StringUtils.isEmpty(parameter.getDeprecated())) {
                            GeneratorUtils.element(w, "deprecated", "No reason given");
                        } else {
                            GeneratorUtils.element(
                                    w,
                                    "deprecated",
                                    getTextValue(type, containsXhtmlTextValues, parameter.getDeprecated()));
                        }
                    }

                    if (!isV4 && parameter.getImplementation() != null) {
                        GeneratorUtils.element(w, "implementation", parameter.getImplementation());
                    }

                    GeneratorUtils.element(w, "required", Boolean.toString(parameter.isRequired()));

                    GeneratorUtils.element(w, "editable", Boolean.toString(parameter.isEditable()));

                    GeneratorUtils.element(
                            w, "description", getTextValue(type, containsXhtmlTextValues, parameter.getDescription()));

                    if (isV4) {
                        if (parameter.getDefaultValue() != null) {
                            GeneratorUtils.element(w, "defaultValue", parameter.getDefaultValue());
                        }
                        if (StringUtils.isNotEmpty(parameter.getExpression())) {
                            GeneratorUtils.element(w, "expression", parameter.getExpression());
                        }
                    } else if (StringUtils.isNotEmpty(parameter.getDefaultValue())
                            || StringUtils.isNotEmpty(parameter.getExpression())) {
                        configuration.add(parameter);
                    }

                    w.writeEndElement();
                }
            }
        }

        w.writeEndElement();

        // ----------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------

        if (!isV4 && !configuration.isEmpty()) {
            w.writeStartElement("configuration");

            for (Parameter parameter : configuration) {
                if (type == DescriptorType.LIMITED_FOR_HELP_MOJO && !parameter.isEditable()) {
                    // don't show readonly parameters in help
                    continue;
                }

                w.writeStartElement(parameter.getName());

                // strip type by parameter type (generics) information
                String parameterType = StringUtils.chomp(parameter.getType(), "<");
                if (parameterType != null && !parameterType.isEmpty()) {
                    w.writeAttribute("implementation", parameterType);
                }

                if (parameter.getDefaultValue() != null) {
                    w.writeAttribute("default-value", parameter.getDefaultValue());
                }

                if (StringUtils.isNotEmpty(parameter.getExpression())) {
                    w.writeCharacters(parameter.getExpression());
                }

                w.writeEndElement();
            }

            w.writeEndElement();
        }

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        if (!isV4 && !requirements.isEmpty() && type != DescriptorType.LIMITED_FOR_HELP_MOJO) {
            w.writeStartElement("requirements");

            for (Map.Entry<String, Requirement> entry : requirements.entrySet()) {
                String key = entry.getKey();
                Requirement requirement = entry.getValue();

                w.writeStartElement("requirement");

                GeneratorUtils.element(w, "role", requirement.getRole());

                if (StringUtils.isNotEmpty(requirement.getRoleHint())) {
                    GeneratorUtils.element(w, "role-hint", requirement.getRoleHint());
                }

                GeneratorUtils.element(w, "field-name", key);

                w.writeEndElement();
            }

            w.writeEndElement();
        }

        w.writeEndElement();
    }

    /**
     * Writes parameter type information and potentially also the related javadoc URL.
     * @param w
     * @param type
     * @param javadocLinkGenerator
     * @param parameter
     * @param goal
     */
    protected void writeParameterType(
            XMLStreamWriter w,
            DescriptorType type,
            JavadocLinkGenerator javadocLinkGenerator,
            Parameter parameter,
            String goal)
            throws XMLStreamException {
        String parameterType = parameter.getType();

        if (type == DescriptorType.STANDARD) {
            // strip type by parameter type (generics) information for standard plugin descriptor
            parameterType = StringUtils.chomp(parameterType, "<");
        }
        GeneratorUtils.element(w, "type", parameterType);

        if (type == DescriptorType.XHTML && javadocLinkGenerator != null) {
            // skip primitives which never has javadoc
            if (parameter.getType().indexOf('.') == -1) {
                LOG.debug("Javadoc URLs are not available for primitive types like {}", parameter.getType());
            } else {
                try {
                    URI javadocUrl = getJavadocUrlForType(javadocLinkGenerator, parameterType);
                    GeneratorUtils.element(w, "typeJavadocUrl", javadocUrl.toString());
                } catch (IllegalArgumentException e) {
                    LOG.warn(
                            "Could not get javadoc URL for type {} of parameter {} from goal {}: {}",
                            parameter.getType(),
                            parameter.getName(),
                            goal,
                            e.getMessage());
                }
            }
        }
    }

    private static String extractBinaryNameForJavadoc(String type) {
        final String binaryName;
        int startOfParameterType = type.indexOf("<");
        if (startOfParameterType != -1) {
            // parse parameter type
            String mainType = type.substring(0, startOfParameterType);

            // some heuristics here
            String[] parameterTypes = type.substring(startOfParameterType + 1, type.lastIndexOf(">"))
                    .split(",\\s*");
            switch (parameterTypes.length) {
                case 1: // if only one parameter type, assume collection, first parameter type is most interesting
                    binaryName = extractBinaryNameForJavadoc(parameterTypes[0]);
                    break;
                case 2: // if two parameter types assume map, second parameter type is most interesting
                    binaryName = extractBinaryNameForJavadoc(parameterTypes[1]);
                    break;
                default:
                    // all other cases link to main type
                    binaryName = mainType;
            }
        } else {
            binaryName = type;
        }
        return binaryName;
    }

    static URI getJavadocUrlForType(JavadocLinkGenerator javadocLinkGenerator, String type) {
        return javadocLinkGenerator.createLink(extractBinaryNameForJavadoc(type));
    }

    /**
     * Get the expression value, eventually surrounding it with <code>${ }</code>.
     *
     * @param parameter the parameter
     * @return the expression value
     */
    private String getExpression(Parameter parameter) {
        String expression = parameter.getExpression();
        if (StringUtils.isNotBlank(expression) && !expression.contains("${")) {
            expression = "${" + expression.trim() + "}";
            parameter.setExpression(expression);
        }
        return expression;
    }

    static class IndentingXMLStreamWriter extends StreamWriterDelegate {

        int depth = 0;
        boolean hasChildren = false;

        IndentingXMLStreamWriter(XMLStreamWriter parent) {
            super(parent);
        }

        @Override
        public void writeEmptyElement(String localName) throws XMLStreamException {
            indent();
            super.writeEmptyElement(localName);
            hasChildren = true;
        }

        @Override
        public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
            indent();
            super.writeEmptyElement(namespaceURI, localName);
            hasChildren = true;
        }

        @Override
        public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            indent();
            super.writeEmptyElement(prefix, localName, namespaceURI);
            hasChildren = true;
        }

        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            indent();
            super.writeStartElement(localName);
            depth++;
            hasChildren = false;
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            indent();
            super.writeStartElement(namespaceURI, localName);
            depth++;
            hasChildren = false;
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            indent();
            super.writeStartElement(prefix, localName, namespaceURI);
            depth++;
            hasChildren = false;
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            depth--;
            if (hasChildren) {
                indent();
            }
            super.writeEndElement();
            hasChildren = true;
        }

        private void indent() throws XMLStreamException {
            super.writeCharacters("\n");
            for (int i = 0; i < depth; i++) {
                super.writeCharacters("  ");
            }
        }
    }
}
