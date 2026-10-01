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
package org.apache.maven.tools.plugin.extractor.annotations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnzipTest {

    @TempDir
    Path tempDir;

    private File zip(String... namesAndContents) throws IOException {
        File zip = tempDir.resolve("sources.jar").toFile();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                out.putNextEntry(new ZipEntry(namesAndContents[i]));
                if (!namesAndContents[i].endsWith("/")) {
                    out.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                }
                out.closeEntry();
            }
        }
        return zip;
    }

    @Test
    void extractsFilesAndDirectories() throws Exception {
        File zip = zip("META-INF/", null, "META-INF/MANIFEST.MF", "m", "a/b/Foo.java", "class Foo {}", "empty/", null);
        File target = tempDir.resolve("out/nested").toFile();

        JavaAnnotationsMojoDescriptorExtractor.unzip(zip, target);

        assertEquals(
                "class Foo {}",
                new String(Files.readAllBytes(target.toPath().resolve("a/b/Foo.java")), StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(target.toPath().resolve("META-INF/MANIFEST.MF")));
        assertTrue(Files.isDirectory(target.toPath().resolve("empty")));
    }

    @Test
    void rejectsZipSlipEntry() throws Exception {
        File zip = zip("ok/Foo.java", "x", "../evil.txt", "pwned");
        File target = tempDir.resolve("out").toFile();

        IOException e =
                assertThrows(IOException.class, () -> JavaAnnotationsMojoDescriptorExtractor.unzip(zip, target));

        assertTrue(e.getMessage().contains("../evil.txt"), e.getMessage());
        assertFalse(Files.exists(tempDir.resolve("evil.txt")));
    }
}
