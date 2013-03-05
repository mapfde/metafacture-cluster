/*
 *  Copyright 2013 Deutsche Nationalbibliothek
 *
 *  Licensed under the Apache License, Version 2.0 the "License";
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.culturegraph.mf.cluster.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * collection of methods to parse wikimarkup.
 * 
 * @author Markus Michael Geipel
 *
 */
public final class WikiUtil {

	public static final Pattern REF = Pattern.compile("<ref>.*?</ref>");
	public static final Pattern LANG_LINKS = Pattern.compile("\\[\\[[a-z\\-]+:[^\\]]+\\]\\]");
	public static final Pattern DOUBLE_CURLY = Pattern.compile("\\{\\{.*?\\}\\}");
	public static final Pattern URL = Pattern.compile("http://[^ <]+");
	public static final Pattern HTML_TAG = Pattern.compile("<[^!][^>]*>");
	public static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	public static final Pattern WIKI_LINKS = Pattern.compile("\\[\\[([^\\|\\]]+)(]]|\\|(.+?]]))");

	private WikiUtil() {/* no instances exist */
	}

	public static List<String> extractLinkDestinations(final String wikiText, final boolean excludeSpecialLinks) {
		int start = 0;
		final List<String> links = new LinkedList<String>();

		while (true) {
			start = wikiText.indexOf("[[", start);

			if (start < 0) {
				break;
			}

			final int end = wikiText.indexOf("]]", start);

			if (end < 0) {
				break;
			}

			String linkText = wikiText.substring(start + 2, end);

			// skip empty links
			if (linkText.length() == 0) {
				start = end + 1;
				continue;
			}

			// skip special links
			//if (excludeSpecialLinks && linkText.indexOf(':') != -1) {
			//	start = end + 1;
			//	continue;
			//}

			// if there is anchor text, get only article title
			int tmp;
			tmp = linkText.indexOf('|');
			if (tmp != -1) {
				linkText = linkText.substring(0, tmp);
			}
			tmp = linkText.indexOf('#');
			if (tmp != -1) {
				linkText = linkText.substring(0, tmp);
			}

			// ignore article-internal links, e.g., [[#section|here]]
			if (linkText.length() == 0) {
				start = end + 1;
				continue;
			}

			links.add(linkText.trim());

			start = end + 1;
		}

		return links;
	}

}
