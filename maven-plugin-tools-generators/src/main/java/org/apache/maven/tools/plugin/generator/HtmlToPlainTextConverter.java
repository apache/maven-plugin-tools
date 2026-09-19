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
        if (StringUtils.isBlank(text)) {
            return text;
        }
        Document document = Jsoup.parse(text);
        return getPlainText(document);
    }

    /**
     * Format an Element to plain-text
     *
     * @param element the root element to format
     * @return formatted text
     */
    private String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor.traverse(formatter, element); // walk the DOM, and call .head() and .tail() for each node

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {
        private final StringBuilder accum = new StringBuilder(); // holds the accumulated text

        /** Text outside {@code <pre>} with whitespace collapsed, plus the preformatted blocks kept as they are. */
        private final StringBuilder result = new StringBuilder();

        /** Stands in for the indentation of nested list items until the whitespace has been collapsed. */
        private static final char INDENT = '\u0001';

        private int preformattedDepth;

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                // TextNodes carry all user-readable text in the DOM.
                accum.append(preformattedDepth > 0 ? textNode.getWholeText() : textNode.text());
            } else if (name.equals("li")) {
                int listDepth = listDepth(node);
                accum.append("\n");
                if (listDepth <= 1) {
                    accum.append(" * ");
                } else {
                    // two spaces per nesting level; the space before the bullet is collapsed away on the top level
                    for (int i = 1; i < listDepth; i++) {
                        accum.append(INDENT).append(INDENT);
                    }
                    accum.append("* ");
                }
            } else if (name.equals("dt")) {
                accum.append("  ");
            } else if (name.equals("pre")) {
                flush();
                preformattedDepth++;
                accum.append("\n");
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                accum.append("\n");
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (name.equals("pre")) {
                accum.append("\n");
                flush();
                preformattedDepth--;
            } else if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                accum.append("\n");
            } else if (name.equals("a")) {
                // link is empty if it cannot be made absolute
                String link = node.absUrl("href");
                if (!link.isEmpty()) {
                    accum.append(String.format(" <%s>", link));
                }
            }
        }

        /** Number of enclosing lists of a list item: 1 for a top-level item. */
        private static int listDepth(Node listItem) {
            int result = 0;
            for (Node parent = listItem.parent(); parent != null; parent = parent.parent()) {
                if (StringUtil.in(parent.nodeName(), "ul", "ol")) {
                    result++;
                }
            }
            return result;
        }

        private void flush() {
            if (preformattedDepth > 0) {
                result.append(accum);
            } else {
                // collate multiple consecutive spaces
                result.append(accum.toString()
                        .replaceAll(" +", " ")
                        .replace("\n ", "\n")
                        .replace(INDENT, ' '));
            }
            accum.setLength(0);
        }

        @Override
        public String toString() {
            flush();
            return result.toString();
        }
    }
}
