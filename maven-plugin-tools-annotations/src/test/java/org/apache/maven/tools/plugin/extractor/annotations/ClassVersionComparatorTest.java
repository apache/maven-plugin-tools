package org.apache.maven.tools.plugin.extractor.annotations;

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

import java.util.Comparator;

import net.bytebuddy.jar.asm.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassVersionComparatorTest
{
    @Test
    void testComparator()
    {
        Comparator<Integer> comparator = new JavaAnnotationsMojoDescriptorExtractor.ClassVersionComparator();
        assertEquals( 0, Integer.signum( comparator.compare( Opcodes.V10, Opcodes.V10 ) ) );
        assertEquals( 1, Integer.signum( comparator.compare( Opcodes.V11, Opcodes.V10 ) ) );
        assertEquals( -1, Integer.signum( comparator.compare( Opcodes.V9, Opcodes.V10 ) ) );
    }
}
