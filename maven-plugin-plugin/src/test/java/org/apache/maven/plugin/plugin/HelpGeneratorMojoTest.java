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

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelpGeneratorMojoTest
{

    public static Stream<Arguments> packageNameShouldBeCorrect()
    {
        return Stream.of(
            Arguments.of( aProject( "groupId", "artifactId" ), "groupId.artifactId" ),
            Arguments.of( aProject( "groupId", "123-artifactId" ), "groupId._123_artifactId" ),
            Arguments.of( aProject( "group-Id", "artifact-Id" ), "group_Id.artifact_Id" ),
            Arguments.of( aProject( "group-Id", "int" ), "group_Id._int" )
        );
    }

    @ParameterizedTest
    @MethodSource
    void packageNameShouldBeCorrect( MavenProject project, String expectedPackageName )
    {
        HelpGeneratorMojo mojo = new HelpGeneratorMojo();
        mojo.project = project;

        String packageName = mojo.getHelpPackageName();
        assertEquals( expectedPackageName, packageName );
    }

    private static MavenProject aProject( String groupId, String artifactId )
    {

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId( groupId );
        mavenProject.setArtifactId( artifactId );
        return mavenProject;
    }
}
