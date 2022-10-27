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

def mojoDoc = new File( basedir, 'target/site/test-mojo.html' )

assert mojoDoc.isFile()

assert mojoDoc.text.contains('<b>See also:</b> <a class="externalLink" href="https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html">java.util.Collections</a></div>') // mojo description see javadoc tag

assert mojoDoc.text.contains('beans parameter leveraging <a href="apidocs/org/SimpleBean.html"><code>SimpleBean</code></a>.') // parameter description
assert mojoDoc.text.contains('<td><code><a href="apidocs/org/SimpleBean.html">Collection&lt;SimpleBean&gt;</a></code></td>') // type link in parameter overview
assert mojoDoc.text.contains('<li><b>Type</b>: <code><a href="apidocs/org/SimpleBean.html">java.util.Collection&lt;org.SimpleBean&gt;</a></code></li>') // type link in parameter details

assert mojoDoc.text.contains('<div>invalid javadoc reference <code>org.apache.maven.artifact.Artifact</code>.</div>') // second parameter description with link being removed (as no javadoc site associated)

// the third parameter contains an invalid link (as the internal link validation has been switched off)
assert mojoDoc.text.contains(' <code><a href="apidocs/org/internal/PrivateBean.html">org.internal.PrivateBean</a></code>')
