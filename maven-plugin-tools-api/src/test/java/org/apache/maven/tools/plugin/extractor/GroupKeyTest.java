package org.apache.maven.tools.plugin.extractor;

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

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * UT for {@link GroupKey}.
 */
public class GroupKeyTest
{
    @Test
    public void sortOrder()
    {
        TreeSet<GroupKey> groupKeys = new TreeSet<>();
        groupKeys.add( new GroupKey( "ant", 1 ) );
        groupKeys.add( new GroupKey( "bsh", 1 ) );
        groupKeys.add( new GroupKey( "foo", 1 ) );
        groupKeys.add( new GroupKey( "zzz", 1 ) );
        groupKeys.add( new GroupKey( GroupKey.JAVA_GROUP, 1 ) );
        groupKeys.add( new GroupKey( "aaa", 2 ) );
        groupKeys.add( new GroupKey( "bbb", 3 ) );
        groupKeys.add( new GroupKey( "bsh", 100 ) );
        groupKeys.add( new GroupKey( "ant", 5 ) );
        groupKeys.add( new GroupKey( GroupKey.JAVA_GROUP, 2 ) );
        assertFalse( groupKeys.add( new GroupKey( GroupKey.JAVA_GROUP, 1 ) ) ); // already present

        StringBuilder stringBuilder = new StringBuilder();
        for ( GroupKey groupKey : groupKeys )
        {
            stringBuilder.append( groupKey.getGroup() ).append( ":" ).append( groupKey.getOrder() ).append( " " );
        }
        // Sort order:
        // 'java' group is always first
        // non-java groups are sorted lexicographically after java
        // within same named groups, int order defines ordering
        assertEquals(
                "java:1 java:2 aaa:2 ant:1 ant:5 bbb:3 bsh:1 bsh:100 foo:1 zzz:1 ",
                stringBuilder.toString()
        );
    }
}
