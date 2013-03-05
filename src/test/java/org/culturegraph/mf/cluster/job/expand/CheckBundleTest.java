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

package org.culturegraph.mf.cluster.job.expand;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.job.expand.BundleCheck.CheckReducer;
import org.culturegraph.mf.cluster.mapreduce.KeyChangeMapper;
import org.culturegraph.mf.cluster.type.StringListMapWritable;
import org.culturegraph.mf.types.ListMap;
import org.culturegraph.mf.types.NamedValue;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Markus Michael Geipel
 * 
 */
public final class CheckBundleTest {

	private static final String IDENTIFIER = "old_id";
	private static final String NEW_ID1 = "new_id1";
	private static final String NEW_ID2 = "new_id2";
	private static final String KEY_NAME = KeyChangeMapper.DEFAULT_KEY;
	private static final String PROPERTY = "prop";

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testKeyChangeMapper() throws IOException, InterruptedException {
		final KeyChangeMapper keyChangeMapper = new KeyChangeMapper();
		final Context context = Mockito.mock(Context.class);

		final StringListMapWritable value = new StringListMapWritable();
		final ListMap<String, String> listMap = new ListMap<String, String>();
		listMap.setId(IDENTIFIER);
		listMap.add("dfsf", "sdfsf");
		listMap.add(KEY_NAME, NEW_ID1);
		listMap.add(KEY_NAME, NEW_ID2);

		value.setListMap(listMap);

		keyChangeMapper.map(new Text(IDENTIFIER), value, context);

		verify(context).write(eq(new Text(NEW_ID1)), any(StringListMapWritable.class));
		verify(context).write(eq(new Text(NEW_ID2)), any(StringListMapWritable.class));

	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testCheckReducer() throws IOException, InterruptedException {
		final CheckReducer checkReducer = new CheckReducer();
		final Reducer.Context context = Mockito.mock(Reducer.Context.class);
		final Counter counter = Mockito.mock(Counter.class);
		when(context.getCounter(anyString(), anyString())).thenReturn(counter);

		final List<StringListMapWritable> values = new ArrayList<StringListMapWritable>();
		final ListMap<String, String> listMap1 = new ListMap<String, String>();
		listMap1.add(PROPERTY, "b");

		final ListMap<String, String> listMap2 = new ListMap<String, String>();
		listMap1.add(PROPERTY, "c");

		values.add(new StringListMapWritable(listMap1));
		values.add(new StringListMapWritable(listMap2));

		checkReducer.reduce(new Text(IDENTIFIER), values, context);
		verify(context).write(eq(new Text(IDENTIFIER)), any(NamedValue.class));

	}

}
