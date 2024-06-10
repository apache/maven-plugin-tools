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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.DuplicateMojoDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Extensions to {@link PluginDescriptor} not supported by Maven 3.2.5.
 * This is a wrapper around an existing PluginDescriptor.
 */
public class ExtendedPluginDescriptor extends PluginDescriptor {
    private final PluginDescriptor delegate;
    private String requiredJavaVersion;

    public ExtendedPluginDescriptor(PluginDescriptor delegate) {
        this.delegate = delegate;
        // populate the fields feeding the final methods of ComponentSetDescriptor
        // which can't be overridden by this wrapper
        this.setIsolatedRealm(delegate.isIsolatedRealm());
        this.setDependencies(delegate.getDependencies());
        this.setComponents(delegate.getComponents());
    }

    public void setRequiredJavaVersion(String requiredJavaVersion) {
        this.requiredJavaVersion = requiredJavaVersion;
    }

    public String getRequiredJavaVersion() {
        return requiredJavaVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtendedPluginDescriptor other = (ExtendedPluginDescriptor) obj;
        return Objects.equals(delegate, other.delegate)
                && Objects.equals(requiredJavaVersion, other.requiredJavaVersion);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(delegate, requiredJavaVersion);
        return result;
    }

    /* -- START delegate methods --*/
    public List<MojoDescriptor> getMojos() {
        return delegate.getMojos();
    }

    public void addMojo(MojoDescriptor mojoDescriptor) throws DuplicateMojoDescriptorException {
        delegate.addMojo(mojoDescriptor);
    }

    public void addMojos(List<MojoDescriptor> mojos) throws DuplicateMojoDescriptorException {
        for (MojoDescriptor mojoDescriptor : mojos) {
            addMojo(mojoDescriptor);
        }
    }

    public String getGroupId() {
        return delegate.getGroupId();
    }

    public void setGroupId(String groupId) {
        delegate.setGroupId(groupId);
    }

    public String getArtifactId() {
        return delegate.getArtifactId();
    }

    public void setArtifactId(String artifactId) {
        delegate.setArtifactId(artifactId);
    }

    public String getPluginLookupKey() {
        return delegate.getPluginLookupKey();
    }

    public String getId() {
        return delegate.getId();
    }

    public String getGoalPrefix() {
        return delegate.getGoalPrefix();
    }

    public void setGoalPrefix(String goalPrefix) {
        delegate.setGoalPrefix(goalPrefix);
    }

    public void setVersion(String version) {
        delegate.setVersion(version);
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public void setSource(String source) {
        delegate.setSource(source);
    }

    public String getSource() {
        return delegate.getSource();
    }

    public boolean isInheritedByDefault() {
        return delegate.isInheritedByDefault();
    }

    public void setInheritedByDefault(boolean inheritedByDefault) {
        delegate.setInheritedByDefault(inheritedByDefault);
    }

    public List<Artifact> getArtifacts() {
        return delegate.getArtifacts();
    }

    public void setArtifacts(List<Artifact> artifacts) {
        delegate.setArtifacts(artifacts);
    }

    public Map<String, Artifact> getArtifactMap() {
        return delegate.getArtifactMap();
    }

    public MojoDescriptor getMojo(String goal) {
        return delegate.getMojo(goal);
    }

    public void setClassRealm(ClassRealm classRealm) {
        delegate.setClassRealm(classRealm);
    }

    public ClassRealm getClassRealm() {
        return delegate.getClassRealm();
    }

    public void setIntroducedDependencyArtifacts(Set<Artifact> introducedDependencyArtifacts) {
        delegate.setIntroducedDependencyArtifacts(introducedDependencyArtifacts);
    }

    public Set<Artifact> getIntroducedDependencyArtifacts() {
        return delegate.getIntroducedDependencyArtifacts();
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public String getName() {
        return delegate.getName();
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public void setRequiredMavenVersion(String requiredMavenVersion) {
        delegate.setRequiredMavenVersion(requiredMavenVersion);
    }

    public String getRequiredMavenVersion() {
        return delegate.getRequiredMavenVersion();
    }

    public void setPlugin(Plugin plugin) {
        delegate.setPlugin(plugin);
    }

    @Override
    public Plugin getPlugin() {
        return delegate.getPlugin();
    }

    @Override
    public Artifact getPluginArtifact() {
        return delegate.getPluginArtifact();
    }

    @Override
    public void setPluginArtifact(Artifact pluginArtifact) {
        delegate.setPluginArtifact(pluginArtifact);
    }

    public PluginDescriptor clone() {
        return delegate.clone();
    }
}
