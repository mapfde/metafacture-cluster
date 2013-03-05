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

package org.culturegraph.mf.cluster.job.text;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.LengthFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TypeTokenFilter;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.German2Stemmer;

/**
 * @author "Markus Michael Geipel"
 * 
 */
public final class MyGermanAnalyzer extends ReusableAnalyzerBase {

	private static final int MIN_TOKEN_LENGTH = 3;
	private final Set<?> stopwords;
	private final Set<String> stoptypes = new HashSet<String>();

	public MyGermanAnalyzer() throws IOException {
		super();
		stopwords = WordlistLoader.getSnowballWordSet(
				IOUtils.getDecodingReader(SnowballFilter.class, "german_stop.txt", IOUtils.CHARSET_UTF_8),
				Version.LUCENE_36);
		stoptypes.add("<NUM>");
		stoptypes.add("<EMAIL>");
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
		final Tokenizer source = new StandardTokenizer(Version.LUCENE_36, reader);
		TokenStream result = new StandardFilter(Version.LUCENE_36, source);
		result = new LowerCaseFilter(Version.LUCENE_36, result);

		result = new StopFilter(Version.LUCENE_36, result, stopwords);
		// result = new KeywordMarkerFilter(result, exclusionSet);
		result = new LengthFilter(true, result, MIN_TOKEN_LENGTH, Integer.MAX_VALUE);
		result = new TypeTokenFilter(true, result, stoptypes);
		result = new SnowballFilter(result, new German2Stemmer());

		return new TokenStreamComponents(source, result);
	}
}
