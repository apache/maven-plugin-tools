package org.apache.maven.plugin.plugin;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Generates the Plugin's documentation report.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "report", threadSafe = true )
@Execute( phase = LifecyclePhase.PROCESS_CLASSES )
public class PluginReport
    extends AbstractMavenReport
{
    /**
     * Report output directory for mojo pages.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-site/xdoc" )
    private File outputDirectory;

    /**
     * Doxia Site Renderer.
     */
    @Component
    private Renderer siteRenderer;

    /**
     * The Maven Project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Mojo scanner tools.
     */
    @Component
    protected MojoScanner mojoScanner;

    /**
     * The file encoding of the source files.
     *
     * @since 2.7
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Specify some requirements to execute this plugin.
     * Example:
     * <pre>
     * &lt;requirements&gt;
     *   &lt;maven&gt;2.0&lt;/maven&gt;
     *   &lt;jdk&gt;1.4&lt;/jdk&gt;
     *   &lt;memory&gt;256m&lt;/memory&gt;
     *   &lt;diskSpace&gt;1m&lt;/diskSpace&gt;
     *   &lt;others&gt;
     *     &lt;property&gt;
     *       &lt;name&gt;SVN&lt;/name&gt;
     *       &lt;value&gt;1.4.6&lt;/value&gt;
     *     &lt;/property&gt;
     *   &lt;/others&gt;
     * &lt;/requirements&gt;
     * </pre>
     */
    @Parameter
    private Requirements requirements;

    /**
     * The goal prefix that will appear before the ":".
     * By default, this plugin applies a heuristic to derive a heuristic from
     * the plugin's artifactId.
     * <p/>
     * It removes any occurrences of the regular expression <strong>-?maven-?</strong>,
     * and then removes any occurrences of <strong>-?plugin-?</strong>.
     * <p>
     * For example, horsefeature-maven-plugin becomes horsefeature.
     * </p>
     * <p>
     * (There is a special for maven-plugin-plugin; it is mapped to 'plugin'.
     * </p>
     *
     * @since 2.4
     */
    @Parameter( property = "goalPrefix" )
    protected String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.skip" )
    private boolean skip;

    /**
     * Set this to "true" to skip generating the report.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.report.skip" )
    private boolean skipReport;

    /**
     * The set of dependencies for the current project
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "${project.artifacts}", required = true, readonly = true )
    protected Set<Artifact> dependencies;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Location of the local repository.
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository local;

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    protected String getOutputDirectory()
    {
        // PLUGIN-191: output directory of plugin.html, not *-mojo.xml
        return project.getReporting().getOutputDirectory();
    }

    /**
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canGenerateReport()
    {
        return "maven-plugin".equals( project.getPackaging() );
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !canGenerateReport() )
        {
            return;
        }
        if ( skip || skipReport )
        {
            getLog().info( "Maven Plugin Plugin Report generation skipped." );
            return;
        }

        // Copy from AbstractGeneratorMojo#execute()
        String defaultGoalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        if ( goalPrefix == null )
        {
            goalPrefix = defaultGoalPrefix;
        }
        else
        {
            getLog().warn( "\n\nGoal prefix is specified as: '" + goalPrefix + "'. Maven currently expects it to be '"
                               + defaultGoalPrefix + "'.\n" );
        }

        // TODO: could use this more, eg in the writing of the plugin descriptor!
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setGroupId( project.getGroupId() );

        pluginDescriptor.setArtifactId( project.getArtifactId() );

        pluginDescriptor.setVersion( project.getVersion() );

        pluginDescriptor.setGoalPrefix( goalPrefix );

        try
        {
            pluginDescriptor.setDependencies( GeneratorUtils.toComponentDependencies( project.getRuntimeDependencies() ) );

            PluginToolsRequest request = new DefaultPluginToolsRequest( project, pluginDescriptor );
            request.setEncoding( encoding );
            request.setSkipErrorNoDescriptorsFound( true );
            request.setDependencies( dependencies );
            request.setLocal( this.local );
            request.setRemoteRepos( this.remoteRepos );


            try
            {
                mojoScanner.populatePluginDescriptor( request );
            }
            catch ( InvalidPluginDescriptorException e )
            {
                // this is OK, it happens to lifecycle plugins. Allow generation to proceed.
                getLog().debug( "Plugin without mojos.", e );

            }

            // Generate the plugin's documentation
            generatePluginDocumentation( pluginDescriptor, locale );

            // Write the overview
            PluginOverviewRenderer r =
                new PluginOverviewRenderer( project, requirements, getSink(), pluginDescriptor, locale );
            r.render();
        }

        catch ( ExtractionException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                            e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "plugin-info";
    }

    /**
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     */
    private void generatePluginDocumentation( PluginDescriptor pluginDescriptor, Locale locale )
        throws MavenReportException
    {
        try
        {
            File outputDir = outputDirectory;
            outputDir.mkdirs();

            PluginXdocGenerator generator = new PluginXdocGenerator( project, locale );
            PluginToolsRequest pluginToolsRequest = new DefaultPluginToolsRequest( project, pluginDescriptor );
            generator.execute( outputDir, pluginToolsRequest );
        }
        catch ( GeneratorException e )
        {
            throw new MavenReportException( "Error writing plugin documentation", e );
        }

    }

    /**
     * @param locale not null
     * @return the bundle for this report
     */
    protected static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "plugin-report", locale, PluginReport.class.getClassLoader() );
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer
        extends AbstractMavenReportRenderer
    {
        private final MavenProject project;

        private final Requirements requirements;

        private final PluginDescriptor pluginDescriptor;

        private final Locale locale;

        /**
         * @param project          not null
         * @param requirements     not null
         * @param sink             not null
         * @param pluginDescriptor not null
         * @param locale           not null
         */
        public PluginOverviewRenderer( MavenProject project, Requirements requirements, Sink sink,
                                       PluginDescriptor pluginDescriptor, Locale locale )
        {
            super( sink );

            this.project = project;

            this.requirements = ( requirements == null ? new Requirements() : requirements );

            this.pluginDescriptor = pluginDescriptor;

            this.locale = locale;
        }

        /**
         * {@inheritDoc}
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        public void renderBody()
        {
            startSection( getTitle() );

            if ( !( pluginDescriptor.getMojos() != null && pluginDescriptor.getMojos().size() > 0 ) )
            {
                paragraph( getBundle( locale ).getString( "report.plugin.goals.nogoal" ) );
                endSection();
                return;
            }

            paragraph( getBundle( locale ).getString( "report.plugin.goals.intro" ) );

            boolean hasMavenReport = false;
            for ( Iterator<MojoDescriptor> i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
            {
                MojoDescriptor mojo = i.next();

                if ( GeneratorUtils.isMavenReport( mojo.getImplementation(), project ) )
                {
                    hasMavenReport = true;
                }
            }

            startTable();

            String goalColumnName = getBundle( locale ).getString( "report.plugin.goals.column.goal" );
            String isMavenReport = getBundle( locale ).getString( "report.plugin.goals.column.isMavenReport" );
            String descriptionColumnName = getBundle( locale ).getString( "report.plugin.goals.column.description" );
            if ( hasMavenReport )
            {
                tableHeader( new String[]{ goalColumnName, isMavenReport, descriptionColumnName } );
            }
            else
            {
                tableHeader( new String[]{ goalColumnName, descriptionColumnName } );
            }

            List<MojoDescriptor> mojos = new ArrayList<MojoDescriptor>();
            mojos.addAll( pluginDescriptor.getMojos() );
            PluginUtils.sortMojos( mojos );
            for ( MojoDescriptor mojo : mojos )
            {
                String goalName = mojo.getFullGoalName();

                /*
                 * Added ./ to define a relative path
                 * @see AbstractMavenReportRenderer#getValidHref(java.lang.String)
                 */
                String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";

                String description;
                if ( StringUtils.isNotEmpty( mojo.getDeprecated() ) )
                {
                    description =
                        "<strong>" + getBundle( locale ).getString( "report.plugin.goal.deprecated" ) + "</strong> "
                            + GeneratorUtils.makeHtmlValid( mojo.getDeprecated() );
                }
                else if ( StringUtils.isNotEmpty( mojo.getDescription() ) )
                {
                    description = GeneratorUtils.makeHtmlValid( mojo.getDescription() );
                }
                else
                {
                    description = getBundle( locale ).getString( "report.plugin.goal.nodescription" );
                }

                sink.tableRow();
                tableCell( createLinkPatternedText( goalName, goalDocumentationLink ) );
                if ( hasMavenReport )
                {
                    if ( GeneratorUtils.isMavenReport( mojo.getImplementation(), project ) )
                    {
                        sink.tableCell();
                        sink.text( getBundle( locale ).getString( "report.plugin.isReport" ) );
                        sink.tableCell_();
                    }
                    else
                    {
                        sink.tableCell();
                        sink.text( getBundle( locale ).getString( "report.plugin.isNotReport" ) );
                        sink.tableCell_();
                    }
                }
                tableCell( description, true );
                sink.tableRow_();
            }

            endTable();

            startSection( getBundle( locale ).getString( "report.plugin.systemrequirements" ) );

            paragraph( getBundle( locale ).getString( "report.plugin.systemrequirements.intro" ) );

            startTable();

            String maven = discoverMavenRequirement( project, requirements );
            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.maven" ) );
            tableCell( ( maven != null
                ? maven
                : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            String jdk = discoverJdkRequirement( project, requirements );
            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.jdk" ) );
            tableCell(
                ( jdk != null ? jdk : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.memory" ) );
            tableCell( ( StringUtils.isNotEmpty( requirements.getMemory() )
                ? requirements.getMemory()
                : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.diskspace" ) );
            tableCell( ( StringUtils.isNotEmpty( requirements.getDiskSpace() )
                ? requirements.getDiskSpace()
                : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            if ( requirements.getOthers() != null && requirements.getOthers().size() > 0 )
            {
                for ( Iterator it = requirements.getOthers().keySet().iterator(); it.hasNext(); )
                {
                    String key = it.next().toString();

                    sink.tableRow();
                    tableCell( key );
                    tableCell( ( StringUtils.isNotEmpty( requirements.getOthers().getProperty( key ) )
                        ? requirements.getOthers().getProperty( key )
                        : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
                    sink.tableRow_();
                }
            }
            endTable();

            endSection();

            renderUsageSection( hasMavenReport );

            endSection();
        }

        /**
         * Render the section about the usage of the plugin.
         *
         * @param hasMavenReport If the plugin has a report or not
         */
        private void renderUsageSection( boolean hasMavenReport )
        {
            startSection( getBundle( locale ).getString( "report.plugin.usage" ) );

            // Configuration
            sink.paragraph();
            text( getBundle( locale ).getString( "report.plugin.usage.intro" ) );
            sink.paragraph_();

            StringBuilder sb = new StringBuilder();
            sb.append( "<project>" ).append( '\n' );
            sb.append( "  ..." ).append( '\n' );
            sb.append( "  <build>" ).append( '\n' );
            sb.append(
                "    <!-- " + getBundle( locale ).getString( "report.plugin.usage.pluginManagement" ) + " -->" ).append(
                '\n' );
            sb.append( "    <pluginManagement>" ).append( '\n' );
            sb.append( "      <plugins>" ).append( '\n' );
            sb.append( "        <plugin>" ).append( '\n' );
            sb.append( "          <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                '\n' );
            sb.append( "          <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                "</artifactId>" ).append( '\n' );
            sb.append( "          <version>" ).append( pluginDescriptor.getVersion() ).append( "</version>" ).append(
                '\n' );
            sb.append( "        </plugin>" ).append( '\n' );
            sb.append( "        ..." ).append( '\n' );
            sb.append( "      </plugins>" ).append( '\n' );
            sb.append( "    </pluginManagement>" ).append( '\n' );
            sb.append( "    <!-- " + getBundle( locale ).getString( "report.plugin.usage.plugins" ) + " -->" ).append(
                '\n' );
            sb.append( "    <plugins>" ).append( '\n' );
            sb.append( "      <plugin>" ).append( '\n' );
            sb.append( "        <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                '\n' );
            sb.append( "        <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                "</artifactId>" ).append( '\n' );
            sb.append( "        <version>" ).append( pluginDescriptor.getVersion() ).append( "</version>" ).append(
                '\n' );
            sb.append( "      </plugin>" ).append( '\n' );
            sb.append( "      ..." ).append( '\n' );
            sb.append( "    </plugins>" ).append( '\n' );
            sb.append( "  </build>" ).append( '\n' );

            if ( hasMavenReport )
            {
                sb.append( "  ..." ).append( '\n' );
                sb.append(
                    "  <!-- " + getBundle( locale ).getString( "report.plugin.usage.reporting" ) + " -->" ).append(
                    '\n' );
                sb.append( "  <reporting>" ).append( '\n' );
                sb.append( "    <plugins>" ).append( '\n' );
                sb.append( "      <plugin>" ).append( '\n' );
                sb.append( "        <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                    '\n' );
                sb.append( "        <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                    "</artifactId>" ).append( '\n' );
                sb.append( "        <version>" ).append( pluginDescriptor.getVersion() ).append( "</version>" ).append(
                    '\n' );
                sb.append( "      </plugin>" ).append( '\n' );
                sb.append( "      ..." ).append( '\n' );
                sb.append( "    </plugins>" ).append( '\n' );
                sb.append( "  </reporting>" ).append( '\n' );
            }

            sb.append( "  ..." ).append( '\n' );
            sb.append( "</project>" ).append( '\n' );

            verbatimText( sb.toString() );

            sink.paragraph();
            linkPatternedText( getBundle( locale ).getString( "report.plugin.configuration.end" ) );
            sink.paragraph_();

            endSection();
        }

        /**
         * Try to lookup on the Maven prerequisites property.
         * If not specified, uses the value defined by the user.
         *
         * @param project      not null
         * @param requirements not null
         * @return the Maven version
         */
        private static String discoverMavenRequirement( MavenProject project, Requirements requirements )
        {
            String maven = requirements.getMaven();
            if ( maven == null )
            {
                maven = ( project.getPrerequisites() != null ? project.getPrerequisites().getMaven() : null );
            }
            if ( maven == null )
            {
                maven = "2.0";
            }

            return maven;
        }

        /**
         * Try to lookup on the <code>org.apache.maven.plugins:maven-compiler-plugin</code> plugin to
         * find the value of the <code>target</code> option.
         * If not specified, uses the value defined by the user.
         * If not specified, uses the value of the system property <code>java.specification.version</code>.
         *
         * @param project      not null
         * @param requirements not null
         * @return the JDK version
         */
        private static String discoverJdkRequirement( MavenProject project, Requirements requirements )
        {
            String jdk = requirements.getJdk();
            if ( jdk == null )
            {
                jdk = discoverJdkRequirementFromPlugins( project.getBuild().getPluginsAsMap(), project.getProperties() );
            }
            if ( jdk == null && project.getPluginManagement() != null )
            {
                jdk =
                    discoverJdkRequirementFromPlugins( project.getPluginManagement().getPluginsAsMap(),
                                                       project.getProperties() );
            }
            if ( jdk == null )
            {
                jdk = "Unknown";
            }

            return jdk;
        }

        /**
         * @param pluginsAsMap could be null
         * @return the value of the <code>target</code> in the configuration of <code>maven-compiler-plugin</code>.
         */
        private static String discoverJdkRequirementFromPlugins( Map<String, Object> pluginsAsMap, Properties props )
        {
            if ( pluginsAsMap == null )
            {
                return null;
            }

            // default value
            String jdk = props.getProperty( "maven.compiler.target" );

            String backupJdk = null;
            for ( Map.Entry<String, Object> entry : pluginsAsMap.entrySet() )
            {
                if ( !entry.getKey().equals( "org.apache.maven.plugins:maven-compiler-plugin" ) )
                {
                    continue;
                }

                Object value = entry.getValue();
                Xpp3Dom pluginConf = null;

                backupJdk = "Default version for maven-compiler-plugin";
                if ( value instanceof Plugin )
                {
                    Plugin plugin = (Plugin) value;
                    backupJdk = "Default target for maven-compiler-plugin version " + plugin.getVersion();
                    pluginConf = (Xpp3Dom) plugin.getConfiguration();
                }

                if ( value instanceof ReportPlugin )
                {
                    ReportPlugin reportPlugin = (ReportPlugin) value;
                    backupJdk = "Default target for maven-compiler-plugin version " + reportPlugin.getVersion();
                    pluginConf = (Xpp3Dom) reportPlugin.getConfiguration();
                }

                if ( pluginConf == null )
                {
                    continue;
                }

                Xpp3Dom target = pluginConf.getChild( "target" );
                if ( target != null )
                {
                    jdk = target.getValue();
                }
            }

            return ( jdk == null ) ? backupJdk : jdk;
        }
    }
}
