package test;

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
 * This is the source class to be scanned for annotations. While scanning, QDox must not try to resolve references to
 * other types like the super class from the plugin class realm. The plugin class realm has no relation at all to
 * the project class path. In particular, the plugin class realm could (by incident) contain different versions of those
 * types or could be incomplete (due to exclusions). The later case leads to NoClassDefFoundErrors, crashing the scan.
 *
 * @goal test
 */
public class SomeMojo
    extends ClassB
{

}
