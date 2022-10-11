
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
assert new File( basedir, 'target/generated-site' ).exists()

content = new File( basedir, 'target/generated-site/xdoc/noop-mojo.xml' ).text

assert content.contains( '<li>Since version: <code>1.0</code>.</li>' )
assert content.contains( '<td><code>-</code></td>' )
assert content.contains( '<td><code>1.1</code></td>' )
assert content.contains( '<li><strong>Since</strong>: <code>1.1</code></li>' )

return true
