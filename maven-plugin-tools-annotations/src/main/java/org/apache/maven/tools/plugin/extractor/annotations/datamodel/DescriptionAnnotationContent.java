package org.apache.maven.tools.plugin.extractor.annotations.datamodel;

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

import java.util.StringJoiner;

/**
 * @author Benjamin Marwell
 * @since 3.7.0
 */
public class DescriptionAnnotationContent
{

    private String content;

    private String since;

    private boolean deprecated;

    private String deprecatedBecause;

    private String deprecatedSince;

    public String getContent()
    {
        return content;
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    public String getSince()
    {
        return since;
    }

    public void setSince( String since )
    {
        this.since = since;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated( boolean deprecated )
    {
        this.deprecated = deprecated;
    }

    public String getDeprecatedBecause()
    {
        return deprecatedBecause;
    }

    public void setDeprecatedBecause( String deprecatedBecause )
    {
        this.deprecatedBecause = deprecatedBecause;
    }

    public String getDeprecatedSince()
    {
        return deprecatedSince;
    }

    public void setDeprecatedSince( String deprecatedSince )
    {
        this.deprecatedSince = deprecatedSince;
    }

    @Override
    public String toString()
    {
        return new StringJoiner( ", ", DescriptionAnnotationContent.class.getSimpleName() + "[", "]" )
                .add( "content='" + content + "'" )
                .add( "since='" + since + "'" )
                .add( "deprecated=" + deprecated )
                .add( "deprecatedBecause='" + deprecatedBecause + "'" )
                .add( "deprecatedSince='" + deprecatedSince + "'" )
                .toString();
    }
}
