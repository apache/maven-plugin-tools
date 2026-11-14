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
package org.apache.maven.tools.plugin.generator;

import org.codehaus.plexus.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Replaces (X)HTML content by plain text equivalent.
 * Based on work from
 * <a href="https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java">
 * JSoup Example: HtmlToPlainText</a>.
 */
public class HtmlToPlainTextConverter implements Converter {
    @Override
    public String convert(String text) {
        return convert(text, false);
    }

    /**
     * Converts (X)HTML to plain text, optionally encoding spaces inside {@code <pre>} blocks
     * as XML-safe non-breaking spaces (U+00A0) for the limited help descriptor.
     * The generated help templates preserve these spaces during word wrapping and decode
     * them to ordinary spaces on output. Ordinary prose keeps its existing wrapping behavior.
     *
     * <p>{@link PluginDescriptorFilesGenerator} enables this only for
     * {@code LIMITED_FOR_HELP_MOJO}; {@link #convert(String)} and standard descriptors
     * retain ordinary spaces.
     *
     * @param text the (X)HTML to convert
     * @param preserveHelpWhitespace whether to encode preformatted spaces for generated help
     * @return the plain-text equivalent, with encoded preformatted spaces when requested
     */
    String convert(String text, boolean preserveHelpWhitespace) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        Document document = Jsoup.parse(text);
        return getPlainText(document, preserveHelpWhitespace);
    }

    /**
     * Format an Element to plain-text
     *
     * @param element the root element to format
     * @return formatted text
     */
    private String getPlainText(Element element, boolean preserveHelpWhitespace) {
        FormattingVisitor formatter = new FormattingVisitor(preserveHelpWhitespace);
        NodeTraversor.traverse(formatter, element); // walk the DOM, and call .head() and .tail() for each node

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {
        private static final char PRE_START = '\u0001';
        private static final char PRE_END = '\u0002';

        private StringBuilder accum = new StringBuilder(); // holds the accumulated text

        // Nested <pre> elements are invalid HTML; Jsoup normalises them before traversal.
        private int preformatted;
        private final boolean preserveHelpWhitespace;

        FormattingVisitor(boolean preserveHelpWhitespace) {
            this.preserveHelpWhitespace = preserveHelpWhitespace;
        }

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                // TextNode#text() normalizes whitespace. Keep the original text inside preformatted blocks.
                accum.append(preformatted > 0 ? textNode.getWholeText() : textNode.text());
            } else if (name.equals("li")) {
                accum.append("\n * ");
            } else if (name.equals("dt")) {
                accum.append("  ");
            } else if (name.equals("pre")) {
                if (accum.length() > 0 && accum.charAt(accum.length() - 1) != '\n') {
                    accum.append('\n');
                }
                accum.append(PRE_START);
                preformatted++;
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                accum.append("\n");
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            boolean preformattedEndsWithNewline = false;
            if (name.equals("pre")) {
                preformatted--;
                preformattedEndsWithNewline = accum.length() > 0 && accum.charAt(accum.length() - 1) == '\n';
                accum.append(PRE_END);
                if (!preformattedEndsWithNewline) {
                    accum.append('\n');
                }
            } else if (name.equals("br")) {
                accum.append('\n');
            } else if (StringUtil.in(name, "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                if (accum.length() == 0 || accum.charAt(accum.length() - 1) != '\n') {
                    accum.append("\n");
                }
            } else if (name.equals("a")) {
                // link is empty if it cannot be made absolute
                String link = node.absUrl("href");
                if (!link.isEmpty()) {
                    accum.append(String.format(" <%s>", link));
                }
            }
        }

        @Override
        public String toString() {
            String text = accum.toString();
            StringBuilder result = new StringBuilder(text.length());
            int start = 0;
            int preStart;
            while ((preStart = text.indexOf(PRE_START, start)) >= 0) {
                appendNormalText(result, text.substring(start, preStart));
                int preEnd = text.indexOf(PRE_END, preStart);
                if (preEnd < 0) {
                    appendNormalText(result, text.substring(preStart + 1));
                    return result.toString();
                }
                String preformattedText = text.substring(preStart + 1, preEnd);
                // Help templates already decode NBSP after word wrapping. Keep spaces inside
                // preformatted blocks out of that wrapping without changing ordinary descriptors.
                result.append(preserveHelpWhitespace ? preformattedText.replace(' ', '\u00a0') : preformattedText);
                start = preEnd + 1;
            }
            appendNormalText(result, text.substring(start));
            return result.toString();
        }

        private static void appendNormalText(StringBuilder result, String text) {
            result.append(text.replaceAll(" +", " ").replace("\n ", "\n"));
        }
    }
}
