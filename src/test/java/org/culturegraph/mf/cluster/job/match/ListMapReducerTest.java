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

package org.culturegraph.mf.cluster.job.match;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.mapreduce.ListMapReducer;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.cluster.type.StringListMapWritable;
import org.culturegraph.mf.types.ListMap;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;



public final class ListMapReducerTest {
	
	private static final String ID1 = "id";
	private static final String VALUE1 = "v1";
	private static final String VALUE2 = "v2";
	private static final String PROPERTY1 = "prop1";
	private static final String PROPERTY2 = "prop2";
	
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testListMapReducer() throws IOException, InterruptedException {
		final ListMapReducer.FromNamedValues listMapReducer = new ListMapReducer.FromNamedValues();
		final Reducer.Context context = Mockito.mock(Reducer.Context.class);
		final Counter counter = Mockito.mock(Counter.class);
		when(context.getCounter(anyString(), anyString())).thenReturn(counter);
		
		final List<NamedValueWritable> values = new ArrayList<NamedValueWritable>();
		
		values.add(new NamedValueWritable(PROPERTY1, VALUE1));
		values.add(new NamedValueWritable(PROPERTY1, VALUE2));
		values.add(new NamedValueWritable(PROPERTY2, VALUE2));
		
		listMapReducer.reduce(new Text(ID1), values, context);
		
		final ArgumentCaptor<StringListMapWritable> result = ArgumentCaptor.forClass(StringListMapWritable.class);
		
		verify(context).write(eq(new Text(ID1)), result.capture());
		final ListMap<String, String> listMap = result.getValue().getListMap();
		Assert.assertTrue(listMap.get(PROPERTY1).contains(VALUE1));
		Assert.assertTrue(listMap.get(PROPERTY1).contains(VALUE2));
		Assert.assertTrue(listMap.get(PROPERTY2).contains(VALUE2));
		Assert.assertEquals(2, listMap.keySet().size());
		Assert.assertEquals(ID1, listMap.getId());
		
	}
}
