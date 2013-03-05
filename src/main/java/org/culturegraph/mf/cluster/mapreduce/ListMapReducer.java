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

package org.culturegraph.mf.cluster.mapreduce;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.cluster.type.StringListMapWritable;
import org.culturegraph.mf.types.ListMap;
import org.culturegraph.mf.types.NamedValue;

/**
 * 
 * @author Markus Michael Geipel
 * 
 */

public final class ListMapReducer {

	private ListMapReducer() {
		// no instances
	}

	public static Class<FromNamedValues> fromNamedValues() {
		return FromNamedValues.class;
	}

	// /**
	// * @return
	// * @deprecated not ready not used yet
	// */
	// @Deprecated
	// public static Class<FromListMaps> fromListMaps() {
	// return FromListMaps.class;
	// }

	/**
	 * collects all {@link NamedValue}s in a {@link ListMap}.
	 * 
	 */
	public static final class FromNamedValues extends Reducer<Text, NamedValueWritable, Text, StringListMapWritable> {
		private final StringListMapWritable writable = new StringListMapWritable();

		@Override
		public void reduce(final Text identifier, final Iterable<NamedValueWritable> namedValues, final Context context)
				throws java.io.IOException, InterruptedException {

			final ListMap<String, String> listMap = new ListMap<String, String>();
			listMap.setId(identifier.toString());
			for (NamedValueWritable namedValueWritable : namedValues) {
				listMap.add(namedValueWritable.getName(), namedValueWritable.getValue());
			}
			writable.setListMap(listMap);
			context.write(identifier, writable);

		}
	}

	// /**
	// * collects all {@link ListMap}s in one {@link ListMap}.
	// *
	// * @deprecated not ready not used yet
	// */
	// @Deprecated
	// public static final class FromListMaps extends Reducer<Text,
	// StringListMapWritable, Text, StringListMapWritable> {
	// private final StringListMapWritable writable = new
	// StringListMapWritable();
	//
	// @Override
	// protected void reduce(final Text identifier, final
	// Iterable<StringListMapWritable> listMapWritables,
	// final Context context) throws java.io.IOException, InterruptedException {
	//
	// final ListMap<String, String> listMap = new ListMap<String, String>();
	// listMap.setId(identifier.toString());
	// for (StringListMapWritable listMapWritable : listMapWritables) {
	// // TODO
	// }
	// writable.setListMap(listMap);
	// context.write(identifier, writable);
	// }
	// }
}
