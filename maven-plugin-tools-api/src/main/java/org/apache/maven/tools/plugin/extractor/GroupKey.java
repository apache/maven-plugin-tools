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

import java.util.Objects;

/**
 * Group key: defines "grouping" for descriptor (based on source of extraction) and rank within
 * group.
 *
 * @since TBD
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

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        GroupKey groupKey = (GroupKey) o;
        return order == groupKey.order && group.equals( groupKey.group );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( group, order );
    }

    /**
     * Compares by group then by order, with special case of {@link #JAVA_GROUP} group:
     * <ul>
     *     <li>First, {@link #group} is considered, if equals to {@link #JAVA_GROUP}, is always first, other
     *     groups are in natural order (string)</li>
     *     <li>within same named groups, order is defined by {@link #order}</li>
     * </ul>
     */
    @Override
    public int compareTo( final GroupKey o )
    {
        if ( JAVA_GROUP.equals( this.group ) && !JAVA_GROUP.equals( o.group ) )
        {
            return -1;
        }
        else if ( !JAVA_GROUP.equals( this.group ) && JAVA_GROUP.equals( o.group ) )
        {
            return 1;
        }
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