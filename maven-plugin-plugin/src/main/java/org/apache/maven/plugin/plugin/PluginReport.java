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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.plugin.descriptor.MNG6109PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Generates the Plugin's documentation report: <code>plugin-info.html</code> plugin overview page,
 * and one <code><i>goal</i>-mojo.html</code> per goal.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.0
 */
@Mojo( name = "report", threadSafe = true )
@Execute( phase = LifecyclePhase.PROCESS_CLASSES )
public class PluginReport
    extends AbstractMavenReport
{
    /**
     * Report output directory for mojos' documentation.
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
     * 
     * If not is specified, Maven requirement is extracted from
     * <code>&lt;project&gt;&lt;prerequisites&gt;&lt;maven&gt;</code>
     * and JDK requirement is extracted from maven-compiler-plugin configuration.
     */
    @Parameter
    private Requirements requirements;

    /**
     * <p>
     * The goal prefix that will appear before the ":".
     * By default, this plugin applies a heuristic to derive a heuristic from
     * the plugin's artifactId.
     * </p>
     * <p>
     * It removes any occurrences of the regular expression <strong>-?maven-?</strong>,
     * and then removes any occurrences of <strong>-?plugin-?</strong>.
     * </p>
     * <p>
     * For example, horsefeature-maven-plugin becomes horsefeature.
     * </p>
     * <p>
     * (There is a special case for maven-plugin-plugin: it is mapped to 'plugin')
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
     * @since 3.5.1
     */
    @Component
    private RuntimeInformation rtInfo;

    /**
     * Path to {@code plugin.xml} plugin descriptor to generate the report from.
     *
     * @since 3.5.1
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}/META-INF/maven/plugin.xml", required = true,
                    readonly = true )
    private File pluginXmlFile;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOutputDirectory()
    {
        // PLUGIN-191: output directory of plugin.html, not *-mojo.xml
        return project.getReporting().getOutputDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport()
    {
        return pluginXmlFile != null && pluginXmlFile.isFile() && pluginXmlFile.canRead();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

        PluginDescriptor pluginDescriptor = extractPluginDescriptor();

        // Generate the mojos' documentation
        generateMojosDocumentation( pluginDescriptor, locale );

        // Write the overview
        PluginOverviewRenderer r =
            new PluginOverviewRenderer( project, requirements, getSink(), pluginDescriptor, locale );
        r.render();
    }

    private PluginDescriptor extractPluginDescriptor()
        throws MavenReportException
    {
        PluginDescriptorBuilder builder = getPluginDescriptorBuilder();
        
        try
        {
            return builder.build( new FileReader( pluginXmlFile ) );
        }
        catch ( FileNotFoundException | PlexusConfigurationException e )
        {
            getLog().debug( "Failed to read " + pluginXmlFile + ", fall back to mojoScanner" );
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
            List<ComponentDependency> deps = GeneratorUtils.toComponentDependencies( project.getArtifacts() );
            pluginDescriptor.setDependencies( deps );

            PluginToolsRequest request = new DefaultPluginToolsRequest( project, pluginDescriptor );
            request.setEncoding( encoding );
            request.setSkipErrorNoDescriptorsFound( true );
            request.setDependencies( new LinkedHashSet<>( project.getArtifacts() ) );
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
        }
        catch ( ExtractionException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                            e );
        }
        return pluginDescriptor;
    }

    /**
     * Return the pluginDescriptorBuilder to use based on the Maven version: either use the original from the 
     * maven-plugin-api or a patched version for Maven versions before the MNG-6109 fix 
     * (because of Maven MNG-6109 bug that won't give accurate 'since' info when reading plugin.xml).
     * 
     * @return the proper pluginDescriptorBuilder
     * @see <a href="https://issues.apache.org/jira/browse/MNG-6109">MNG-6109</a>
     * @see <a href="https://issues.apache.org/jira/browse/MPLUGIN-319">MPLUGIN-319</a>
     */
    private PluginDescriptorBuilder getPluginDescriptorBuilder()
    {
        PluginDescriptorBuilder pluginDescriptorBuilder;

        if ( rtInfo.isMavenVersion( "(3.3.9,)" ) )
        {
            pluginDescriptorBuilder = new PluginDescriptorBuilder();
        }
        else
        {
            pluginDescriptorBuilder = new MNG6109PluginDescriptorBuilder();
        }

        return pluginDescriptorBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName()
    {
        return "plugin-info";
    }

    /**
     * Generate the mojos documentation, as xdoc files.
     *
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     */
    private void generateMojosDocumentation( PluginDescriptor pluginDescriptor, Locale locale )
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
        PluginOverviewRenderer( MavenProject project, Requirements requirements, Sink sink,
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
        @Override
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        /**
         * {@inheritDoc}
         */
        @Override
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

            List<MojoDescriptor> mojos = new ArrayList<>();
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
         * <ol>
         * <li>use configured jdk requirement</li>
         * <li>use <code>target</code> configuration of <code>org.apache.maven.plugins:maven-compiler-plugin</code></li>
         * <li>use <code>target</code> configuration of <code>org.apache.maven.plugins:maven-compiler-plugin</code> in
         * <code>pluginManagement</code></li>
         * <li>use <code>maven.compiler.target</code> property</li>
         * </ol>
         *
         * @param project      not null
         * @param requirements not null
         * @return the JDK version
         */
        private static String discoverJdkRequirement( MavenProject project, Requirements requirements )
        {
            String jdk = requirements.getJdk();

            if ( jdk != null )
            {
                return jdk;
            }

            Plugin compiler = getCompilerPlugin( project.getBuild().getPluginsAsMap() );
            if ( compiler == null )
            {
                compiler = getCompilerPlugin( project.getPluginManagement().getPluginsAsMap() );
            }

            jdk = getPluginParameter( compiler, "target" );
            if ( jdk != null )
            {
                return jdk;
            }

            jdk = getPluginParameter( compiler, "release" );
            if ( jdk != null )
            {
                return jdk;
            }

            // default value
            jdk = project.getProperties().getProperty( "maven.compiler.target" );
            if ( jdk != null )
            {
                return jdk;
            }

            // return "1.5" by default?

            String version = ( compiler == null ) ? null : compiler.getVersion();

            if ( version != null )
            {
                return "Default target for maven-compiler-plugin version " + version;
            }

            return "Unknown";
        }

        private static Plugin getCompilerPlugin( Map<String, Plugin> pluginsAsMap )
        {
            return pluginsAsMap.get( "org.apache.maven.plugins:maven-compiler-plugin" );
        }

        private static String getPluginParameter( Plugin plugin, String parameter )
        {
            if ( plugin != null )
            {
                Xpp3Dom pluginConf = (Xpp3Dom) plugin.getConfiguration();

                if ( pluginConf != null )
                {
                    Xpp3Dom target = pluginConf.getChild( parameter );

                    if ( target != null )
                    {
                        return target.getValue();
                    }
                }
            }

            return null;
        }
    }
}
