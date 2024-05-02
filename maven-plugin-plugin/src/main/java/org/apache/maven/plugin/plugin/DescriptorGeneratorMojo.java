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
package org.apache.maven.plugin.plugin;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedPluginDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginDescriptorFilesGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.ReaderFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * <p>
 * Generate a plugin descriptor.
 * </p>
 * <p>
 * <b>Note:</b> Since 3.0, for Java plugin annotations support,
 * default <a href="http://maven.apache.org/ref/current/maven-core/lifecycles.html">phase</a>
 * defined by this goal is after the "compilation" of any scripts. This doesn't override
 * <a href="/ref/current/maven-core/default-bindings.html#Bindings_for_maven-plugin_packaging">the default binding coded
 * at generate-resources phase</a> in Maven core.
 * </p>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @since 2.0
 */
@Mojo(
        name = "descriptor",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class DescriptorGeneratorMojo extends AbstractGeneratorMojo {
    private static final String VALUE_AUTO = "auto";

    /**
     * The directory where the generated <code>plugin.xml</code> file will be put.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/maven", readonly = true)
    private File outputDirectory;

    /**
     * The file encoding of the source files.
     *
     * @since 2.5
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * A flag to disable generation of the <code>plugin.xml</code> in favor of a hand authored plugin descriptor.
     *
     * @since 2.6
     */
    @Parameter(defaultValue = "false")
    private boolean skipDescriptor;

    /**
     * <p>
     * The role names of mojo extractors to use.
     * </p>
     * <p>
     * If not set, all mojo extractors will be used. If set to an empty extractor name, no mojo extractors
     * will be used.
     * </p>
     * Example:
     * <pre>
     *  &lt;!-- Use all mojo extractors --&gt;
     *  &lt;extractors/&gt;
     *
     *  &lt;!-- Use no mojo extractors --&gt;
     *  &lt;extractors&gt;
     *      &lt;extractor/&gt;
     *  &lt;/extractors&gt;
     *
     *  &lt;!-- Use only bsh mojo extractor --&gt;
     *  &lt;extractors&gt;
     *      &lt;extractor&gt;bsh&lt;/extractor&gt;
     *  &lt;/extractors&gt;
     * </pre>
     * The extractors with the following names ship with {@code maven-plugin-tools}:
     * <ol>
     *  <li>{@code java-annotations}</li>
     *  <li>{@code java-javadoc}, deprecated</li>
     *  <li>{@code ant}, deprecated</li>
     *  <li>{@code bsh}, deprecated</li>
     * </ol>
     */
    @Parameter
    private Set<String> extractors;

    /**
     * By default, an exception is throw if no mojo descriptor is found. As the maven-plugin is defined in core, the
     * descriptor generator mojo is bound to generate-resources phase.
     * But for annotations, the compiled classes are needed, so skip error
     *
     * @since 3.0
     */
    @Parameter(property = "maven.plugin.skipErrorNoDescriptorsFound", defaultValue = "false")
    private boolean skipErrorNoDescriptorsFound;

    /**
     * Flag controlling is "expected dependencies in provided scope" check to be performed or not. Default value:
     * {@code true}.
     *
     * @since 3.6.3
     */
    @Parameter(defaultValue = "true", property = "maven.plugin.checkExpectedProvidedScope")
    private boolean checkExpectedProvidedScope = true;

    /**
     * List of {@code groupId} strings of artifact coordinates that are expected to be in "provided" scope. Default
     * value: {@code ["org.apache.maven"]}.
     *
     * @since 3.6.3
     */
    @Parameter
    private List<String> expectedProvidedScopeGroupIds = Collections.singletonList("org.apache.maven");

    /**
     * List of {@code groupId:artifactId} strings of artifact coordinates that are to be excluded from "expected
     * provided scope" check. Default value:
     * {@code ["org.apache.maven:maven-archiver", "org.apache.maven:maven-jxr", "org.apache.maven:plexus-utils"]}.
     *
     * @since 3.6.3
     */
    @Parameter
    private List<String> expectedProvidedScopeExclusions = Arrays.asList(
            "org.apache.maven:maven-archiver", "org.apache.maven:maven-jxr", "org.apache.maven:plexus-utils");

    /**
     * Specify the dependencies as {@code groupId:artifactId} containing (abstract) Mojos, to filter
     * dependencies scanned at runtime and focus on dependencies that are really useful to Mojo analysis.
     * By default, the value is {@code null} and all dependencies are scanned (as before this parameter was added).
     * If specified in the configuration with no children, no dependencies are scanned.
     *
     * @since 3.5
     */
    @Parameter
    private List<String> mojoDependencies = null;

    /**
     * Creates links to existing external javadoc-generated documentation.
     * <br>
     * <b>Notes</b>:
     * all given links should have a fetchable {@code /package-list} or {@code /element-list} file.
     * For instance:
     * <pre>
     * &lt;externalJavadocBaseUrls&gt;
     *   &lt;externalJavadocBaseUrl&gt;https://docs.oracle.com/javase/8/docs/api/&lt;/externalJavadocBaseUrl&gt;
     * &lt;/externalJavadocBaseUrls&gt;
     * </pre>
     * is valid because <code>https://docs.oracle.com/javase/8/docs/api/package-list</code> exists.
     * See <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html#standard-doclet-options">
     * link option of the javadoc tool</a>.
     * Using this parameter requires connectivity to the given URLs during the goal execution.
     * @since 3.7.0
     */
    @Parameter(property = "externalJavadocBaseUrls", alias = "links")
    protected List<URI> externalJavadocBaseUrls;

    /**
     * The base URL for the Javadoc site containing the current project's API documentation.
     * This may be relative to the root of the generated Maven site.
     * It does not need to exist yet at the time when this goal is executed.
     * Must end with a slash.
     * <b>In case this is set the javadoc reporting goal should be executed prior to
     * <a href="../maven-plugin-report-plugin/index.html">Plugin Report</a>.</b>
     * @since 3.7.0
     */
    @Parameter(property = "internalJavadocBaseUrl")
    protected URI internalJavadocBaseUrl;

    /**
     * The version of the javadoc tool (equal to the container JDK version) used to generate the internal javadoc
     * Only relevant if {@link #internalJavadocBaseUrl} is set.
     * The default value needs to be overwritten in case toolchains are being used for generating Javadoc.
     *
     * @since 3.7.0
     */
    @Parameter(property = "internalJavadocVersion", defaultValue = "${java.version}")
    protected String internalJavadocVersion;

    @Component
    private MavenSession mavenSession;

    /**
     * The required Java version to set in the plugin descriptor. This is evaluated by Maven 4 and ignored by earlier
     * Maven versions. Can be either one of the following formats:
     *
     * <ul>
     * <li>A version range which specifies the supported Java versions. It can either use the usual mathematical
     * syntax like {@code "[1.7,9),[11,)"} or use a single version like {@code "1.8"}. The latter is a short
     * form for {@code "[1.8,)"}, i.e. denotes the minimum version required.</li>
     * <li>{@code "auto"} to determine the minimum Java version from the binary class version being generated during
     * compilation (determined by the extractor).</li>
     * </ul>
     *
     * @since 3.8.0
     */
    @Parameter(defaultValue = VALUE_AUTO)
    String requiredJavaVersion;

    /**
     * The required Maven version to set in the plugin descriptor. This is evaluated by Maven 4 and ignored by earlier
     * Maven versions. Can be either one of the following formats:
     *
     * <ul>
     * <li>A version range which specifies the supported Maven versions. It can either use the usual mathematical
     * syntax like {@code "[2.0.10,2.1.0),[3.0,)"} or use a single version like {@code "2.2.1"}. The latter is a short
     * form for {@code "[2.2.1,)"}, i.e. denotes the minimum version required.</li>
     * <li>{@code "auto"} to determine the minimum Maven version from the POM's Maven prerequisite, or if not set the
     * referenced Maven Plugin API version.</li>
     * </ul>
     * This value takes precedence over the
     * <a href="https://maven.apache.org/pom.html#Prerequisites">POM's Maven prerequisite</a> in Maven 4.
     *
     * @since 3.8.0
     */
    @Parameter(defaultValue = VALUE_AUTO)
    String requiredMavenVersion;

    /**
     * The component used for scanning the source tree for mojos.
     */
    @Component
    private MojoScanner mojoScanner;

    @Component
    protected BuildContext buildContext;

    public void generate() throws MojoExecutionException {

        if (!"maven-plugin".equalsIgnoreCase(project.getArtifactId())
                && project.getArtifactId().toLowerCase().startsWith("maven-")
                && project.getArtifactId().toLowerCase().endsWith("-plugin")
                && !"org.apache.maven.plugins".equals(project.getGroupId())) {
            getLog().warn(LS + LS + "Artifact Ids of the format maven-___-plugin are reserved for" + LS
                    + "plugins in the Group Id org.apache.maven.plugins" + LS
                    + "Please change your artifactId to the format ___-maven-plugin" + LS
                    + "In the future this error will break the build." + LS + LS);
        }

        if (skipDescriptor) {
            getLog().warn("Execution skipped");
            return;
        }

        if (checkExpectedProvidedScope) {
            Set<Artifact> wrongScopedArtifacts = dependenciesNotInProvidedScope();
            if (!wrongScopedArtifacts.isEmpty()) {
                StringBuilder message = new StringBuilder(
                        LS + LS + "Some dependencies of Maven Plugins are expected to be in provided scope." + LS
                                + "Please make sure that dependencies listed below declared in POM" + LS
                                + "have set '<scope>provided</scope>' as well." + LS + LS
                                + "The following dependencies are in wrong scope:" + LS);
                for (Artifact artifact : wrongScopedArtifacts) {
                    message.append(" * ").append(artifact).append(LS);
                }
                message.append(LS).append(LS);

                getLog().warn(message.toString());
            }
        }

        mojoScanner.setActiveExtractors(extractors);

        // TODO: could use this more, eg in the writing of the plugin descriptor!
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setGroupId(project.getGroupId());

        pluginDescriptor.setArtifactId(project.getArtifactId());

        pluginDescriptor.setVersion(project.getVersion());

        pluginDescriptor.setGoalPrefix(goalPrefix);

        pluginDescriptor.setName(project.getName());

        pluginDescriptor.setDescription(project.getDescription());

        if (encoding == null || encoding.length() < 1) {
            getLog().warn("Using platform encoding (" + ReaderFactory.FILE_ENCODING
                    + " actually) to read mojo source files, i.e. build is platform dependent!");
        } else {
            getLog().info("Using '" + encoding + "' encoding to read mojo source files.");
        }

        if (internalJavadocBaseUrl != null && !internalJavadocBaseUrl.getPath().endsWith("/")) {
            throw new MojoExecutionException("Given parameter 'internalJavadocBaseUrl' must end with a slash but is '"
                    + internalJavadocBaseUrl + "'");
        }
        try {
            List<ComponentDependency> deps = GeneratorUtils.toComponentDependencies(project.getArtifacts());
            pluginDescriptor.setDependencies(deps);

            PluginToolsRequest request = new DefaultPluginToolsRequest(project, pluginDescriptor);
            request.setEncoding(encoding);
            request.setSkipErrorNoDescriptorsFound(skipErrorNoDescriptorsFound);
            request.setDependencies(filterMojoDependencies());
            request.setRepoSession(mavenSession.getRepositorySession());
            request.setInternalJavadocBaseUrl(internalJavadocBaseUrl);
            request.setInternalJavadocVersion(internalJavadocVersion);
            request.setExternalJavadocBaseUrls(externalJavadocBaseUrls);
            request.setSettings(mavenSession.getSettings());

            mojoScanner.populatePluginDescriptor(request);
            request.setPluginDescriptor(extendPluginDescriptor(request));

            outputDirectory.mkdirs();

            PluginDescriptorFilesGenerator pluginDescriptorGenerator = new PluginDescriptorFilesGenerator();
            pluginDescriptorGenerator.execute(outputDirectory, request);

            buildContext.refresh(outputDirectory);
        } catch (GeneratorException e) {
            throw new MojoExecutionException("Error writing plugin descriptor", e);
        } catch (InvalidPluginDescriptorException | ExtractionException e) {
            throw new MojoExecutionException(
                    "Error extracting plugin descriptor: '" + e.getLocalizedMessage() + "'", e);
        } catch (LinkageError e) {
            throw new MojoExecutionException(
                    "The API of the mojo scanner is not compatible with this plugin version."
                            + " Please check the plugin dependencies configured"
                            + " in the POM and ensure the versions match.",
                    e);
        }
    }

    private PluginDescriptor extendPluginDescriptor(PluginToolsRequest request) {
        ExtendedPluginDescriptor extendedPluginDescriptor = new ExtendedPluginDescriptor(request.getPluginDescriptor());
        extendedPluginDescriptor.setRequiredJavaVersion(getRequiredJavaVersion(request));
        extendedPluginDescriptor.setRequiredMavenVersion(getRequiredMavenVersion(request));
        return extendedPluginDescriptor;
    }

    private String getRequiredMavenVersion(PluginToolsRequest request) {
        if (!VALUE_AUTO.equals(requiredMavenVersion)) {
            return requiredMavenVersion;
        }
        getLog().debug("Trying to derive Maven version automatically from project prerequisites...");
        String requiredMavenVersion =
                project.getPrerequisites() != null ? project.getPrerequisites().getMaven() : null;
        if (requiredMavenVersion == null) {
            getLog().debug("Trying to derive Maven version automatically from referenced Maven Plugin API artifact "
                    + "version...");
            requiredMavenVersion = request.getUsedMavenApiVersion();
        }
        if (requiredMavenVersion == null) {
            getLog().warn("Cannot determine the required Maven version automatically, it is recommended to "
                    + "configure some explicit value manually.");
        }
        return requiredMavenVersion;
    }

    private String getRequiredJavaVersion(PluginToolsRequest request) {
        if (!VALUE_AUTO.equals(requiredJavaVersion)) {
            return requiredJavaVersion;
        }
        String minRequiredJavaVersion = request.getRequiredJavaVersion();
        if (minRequiredJavaVersion == null) {
            getLog().warn("Cannot determine the minimally required Java version automatically, it is recommended to "
                    + "configure some explicit value manually.");
            return null;
        }

        return minRequiredJavaVersion;
    }

    /**
     * Collects all dependencies expected to be in "provided" scope but are NOT in "provided" scope.
     */
    private Set<Artifact> dependenciesNotInProvidedScope() {
        LinkedHashSet<Artifact> wrongScopedDependencies = new LinkedHashSet<>();

        for (Artifact dependency : project.getArtifacts()) {
            String ga = dependency.getGroupId() + ":" + dependency.getArtifactId();
            if (expectedProvidedScopeGroupIds.contains(dependency.getGroupId())
                    && !expectedProvidedScopeExclusions.contains(ga)
                    && !Artifact.SCOPE_PROVIDED.equals(dependency.getScope())) {
                wrongScopedDependencies.add(dependency);
            }
        }

        return wrongScopedDependencies;
    }

    /**
     * Get dependencies filtered with mojoDependencies configuration.
     *
     * @return eventually filtered dependencies, or even <code>null</code> if configured with empty mojoDependencies
     * list
     * @see #mojoDependencies
     */
    private Set<Artifact> filterMojoDependencies() {
        Set<Artifact> filteredArtifacts;
        if (mojoDependencies == null) {
            filteredArtifacts = new LinkedHashSet<>(project.getArtifacts());
        } else if (mojoDependencies.isEmpty()) {
            filteredArtifacts = null;
        } else {
            filteredArtifacts = new LinkedHashSet<>();

            ArtifactFilter filter = new IncludesArtifactFilter(mojoDependencies);

            for (Artifact artifact : project.getArtifacts()) {
                if (filter.include(artifact)) {
                    filteredArtifacts.add(artifact);
                }
            }
        }

        return filteredArtifacts;
    }
}
