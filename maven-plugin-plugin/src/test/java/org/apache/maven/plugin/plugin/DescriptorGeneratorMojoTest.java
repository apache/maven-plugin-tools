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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// at least one test class must be public for test-javadoc report
public class DescriptorGeneratorMojoTest
{
    public static Stream<Arguments> goalPrefixes()
    {
        return Stream.of(
            arguments( null, "maven-plugin-plugin", "plugin" ),
            arguments( null, "maven-plugin-report-plugin", "plugin-report" ),
            arguments( null, "maven-default-plugin", "default" ),
            arguments( null, "default-maven-plugin", "default" ),
            arguments( null, "default-maven-plugin", "default" ),
            arguments( "foo.bar", "maven-plugin", "bar" ),
            arguments( "foo", "maven-plugin", "foo" )
        );
    }

    @ParameterizedTest
    @MethodSource("goalPrefixes")
    void defaultGoalPrefix(String groupId, String artifactId, String expectedGoal)
    {
        assertThat( DescriptorGeneratorMojo.getDefaultGoalPrefix( newProject( groupId, artifactId ) ),
                    is( expectedGoal ) );
    }
    
    private MavenProject newProject( final String groupId, final String artifactId )
    {
        return new MavenProject() {
            @Override
            public String getGroupId()
            {
                return groupId;
            }
            
            @Override
            public String getArtifactId()
            {
                return artifactId;
            }
        };
    }

}
