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

/**
 * Group key: defines "grouping" for descriptor (based on source of extraction) and rank within
 * group.
 */
public final class GroupKey
    implements Comparable<GroupKey>
{
    /**
     * Java group is handled a bit special: is always first to be scanned.
     */
    public static final String JAVA_GROUP = "java";

    private final String group;

    private final int order;

    public GroupKey( String group, int order )
    {
        if ( group == null )
        {
            throw new NullPointerException( "GroupKey.group null" );
        }
        this.group = group;
        this.order = order;
    }

    /**
     * Returns the group this key belongs to, never {@code null}.
     */
    public String getGroup()
    {
        return group;
    }

    /**
     * Returns the order within same group of this key. Returns int should be used for ordering only.
     */
    public int getOrder()
    {
        return order;
    }

    /**
     * Compares by group then by order.
     */
    @Override
    public int compareTo( final GroupKey o )
    {
        int result = this.group.compareTo( o.group );
        if ( result != 0 )
        {
            return result;
        }
        return Integer.compare( this.order, o.order );
    }

    @Override
    public String toString()
    {
        return group + ":" + order;
    }
}