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

import java.lang.reflect.Method;

import org.apache.maven.plugin.descriptor.PluginDescriptor;

public class PluginDescriptorHelper {

    private static final Method GET_REQUIRED_JAVA_VERSION_METHOD;
    private static final Method SET_REQUIRED_JAVA_VERSION_METHOD;

    static {
        Method getMethod = null;
        Method setMethod = null;
        try {
            getMethod = PluginDescriptor.class.getMethod("getRequiredJavaVersion");
            setMethod = PluginDescriptor.class.getMethod("setRequiredJavaVersion", String.class);
        } catch (NoSuchMethodException e) {
            // Methods don't exist in this version of Maven
        }
        GET_REQUIRED_JAVA_VERSION_METHOD = getMethod;
        SET_REQUIRED_JAVA_VERSION_METHOD = setMethod;
    }

    public static String getRequiredJavaVersion(PluginDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        // First try to use the direct method if available in Maven 4
        if (GET_REQUIRED_JAVA_VERSION_METHOD != null) {
            try {
                return (String) GET_REQUIRED_JAVA_VERSION_METHOD.invoke(descriptor);
            } catch (Exception e) {
                // Fall back to the wrapper approach
            }
        }

        // Fall back to the wrapper approach for Maven 3
        if (descriptor instanceof ExtendedPluginDescriptor) {
            return ((ExtendedPluginDescriptor) descriptor).getRequiredJavaVersion();
        }

        return null;
    }

    /**
     * Sets the required Java version on a plugin descriptor.
     * <p>
     * This method works with both Maven 3 and Maven 4:
     * <ul>
     *   <li>In Maven 4, it uses the direct method on the PluginDescriptor class</li>
     *   <li>In Maven 3, it uses the ExtendedPluginDescriptor wrapper</li>
     * </ul>
     *
     * @param descriptor the plugin descriptor
     * @param requiredJavaVersion the required Java version to set
     * @return the modified plugin descriptor, or null if the input descriptor was null
     */
    public static PluginDescriptor setRequiredJavaVersion(PluginDescriptor descriptor, String requiredJavaVersion) {
        if (descriptor == null) {
            return null;
        }

        // First try to use the direct method if available in Maven 4
        if (SET_REQUIRED_JAVA_VERSION_METHOD != null) {
            try {
                SET_REQUIRED_JAVA_VERSION_METHOD.invoke(descriptor, requiredJavaVersion);
                return descriptor;
            } catch (Exception e) {
                // Fall back to the wrapper approach
            }
        }

        // Fall back to the wrapper approach for Maven 3
        if (!(descriptor instanceof ExtendedPluginDescriptor)) {
            descriptor = new ExtendedPluginDescriptor(descriptor);
        }
        ((ExtendedPluginDescriptor) descriptor).setRequiredJavaVersion(requiredJavaVersion);
        return descriptor;
    }
}
