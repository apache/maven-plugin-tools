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
package org.apache.maven.tools.plugin.extractor.annotations.converter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;

import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.JavadocTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.JavadocInlineTagToHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces inline javadoc taglets by their according XHTML representation.
 */
@Named
@Singleton
public class JavadocInlineTagsToXhtmlConverter {
    private static final Logger LOG = LoggerFactory.getLogger(JavadocInlineTagsToXhtmlConverter.class);

    private final Map<String, JavadocInlineTagToHtmlConverter> converters;

    private static final String INLINE_TAG_START = "{@";

    @Inject
    public JavadocInlineTagsToXhtmlConverter(Map<String, JavadocInlineTagToHtmlConverter> converters) {
        this.converters = converters;
    }

    /**
     * Converts the given text containing arbitrarily many inline javadoc tags with their according HTML replacement.
     * @param text
     * @param context
     * @return
     */
    public String convert(String text, ConverterContext context) {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int start;
        while ((start = text.indexOf(INLINE_TAG_START, pos)) >= 0) {
            int nameEnd = start + INLINE_TAG_START.length();
            while (nameEnd < text.length()
                    && !Character.isWhitespace(text.charAt(nameEnd))
                    && text.charAt(nameEnd) != '}') {
                nameEnd++;
            }
            int end = findClosingBrace(text, nameEnd);
            if (end < 0) {
                // unbalanced braces: leave the rest of the text untouched
                break;
            }
            String tagName = text.substring(start + INLINE_TAG_START.length(), nameEnd);
            // the single whitespace character separating the tag name from its argument is not part of the argument
            String reference = nameEnd < end ? text.substring(nameEnd + 1, end) : null;
            String original = text.substring(start, end + 1);
            sb.append(text, pos, start);
            sb.append(convertTag(tagName, reference, original, context));
            pos = end + 1;
        }
        sb.append(text, pos, text.length());
        return toXHTML(sb.toString());
    }

    /**
     * Finds the brace closing the inline tag whose argument starts at {@code from}. Braces inside the argument are
     * allowed as long as they are balanced, as with {@code {@code ${project.basedir}}} in javadoc itself.
     *
     * @return the index of the closing brace, or {@code -1} if the braces are not balanced
     */
    private static int findClosingBrace(String text, int from) {
        int depth = 1;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String convertTag(String tagName, String reference, String original, ConverterContext context) {
        JavadocTagToHtmlConverter converter = converters.get(tagName);
        if (converter == null) {
            LOG.warn("Found unsupported javadoc inline tag '{}' in {}", tagName, context.getLocation());
            return original + "<!-- unsupported tag '" + tagName + "' -->";
        }
        try {
            return converter.convert(reference, context);
        } catch (Throwable t) {
            LOG.warn("Error converting javadoc inline tag '{}' in {}", tagName, context.getLocation(), t);
            // leave original javadoc in place
            return original + "<!-- error processing javadoc tag '" + tagName + "': " + t.getMessage() + " -->";
        }
    }

    static String toXHTML(String bodySnippet) {
        String html = "<html><head></head><body>" + bodySnippet + "</body>"; // make it a valid HTML document
        final Document document = Jsoup.parse(html);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return document.body().html();
    }
}
