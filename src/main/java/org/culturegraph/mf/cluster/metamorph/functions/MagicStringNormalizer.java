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

package org.culturegraph.mf.cluster.metamorph.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.culturegraph.mf.morph.functions.AbstractSimpleStatelessFunction;

public final class MagicStringNormalizer extends AbstractSimpleStatelessFunction{

	private static final Pattern NONCHAR = Pattern.compile("([\\p{L}\\d])+");

	private static final int MIN_TOTAL_LENGTH = 4;

	private static final int MIN_PART_LENTGH = 2;
	
	private final Set<String> set = new HashSet<String>();
	private final List<String> list = new ArrayList<String>();
	
	@Override
	public String process(final String value) {
		set.clear();
		list.clear();
		
		final StringBuilder builder = new StringBuilder();
		final Matcher matcher = NONCHAR.matcher(value.toLowerCase(Locale.GERMAN));
		while(matcher.find()){
			set.add(matcher.group());
		}
		list.addAll(set);
		Collections.sort(list);
		for (String part : list) {
			if(part.length()>=MIN_PART_LENTGH){
				builder.append(part);
			}
		}
		if(builder.length()>=MIN_TOTAL_LENGTH){
			return builder.toString();
		}
		return null;
		
	}
	
	public static void main(final String[] args){
		final MagicStringNormalizer magic1 = new MagicStringNormalizer();
		final String input = "Petite Bibliothèque Payot ; 39 Charles Beaudelaire : un poète lyrique à l'apogée du capitalisme /";
		System.out.println(input);
		System.out.println(magic1.process(input));
	}
}
