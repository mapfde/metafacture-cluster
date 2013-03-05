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

package org.culturegraph.mf.cluster.job.stat;

import java.io.IOException;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.culturegraph.mf.cluster.mapreduce.MorphMapper;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.stream.sink.StringListMap;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Charsets;

public final class HTableMorphTest {

	private static final String ROW1 = "1";
	private static final String ROW2 = "2";

	private static final String NAME = "Aloha";
	private static final String NAME2 = "Hula";
	private static final String VALUE = "Hawaii";
	private static final String VALUE2 = "Oahu";

	// private final MapDriver<ImmutableBytesWritable, Result, Text,
	// NamedValueWritable> mapDriver = new MapDriver<ImmutableBytesWritable,
	// Result, Text, NamedValueWritable>(
	// new HTableMorph2.MorphMapper());

	@Test
	public void testSimpleMapping() throws InterruptedException, IOException {
		map(NAME, VALUE, ROW1, NAME, VALUE);
	}

	@Test
	public void testMappingWithRedirect() throws InterruptedException, IOException {
		map("{to:" + ROW2 + "}" + NAME2, VALUE2, ROW2, NAME2, VALUE2);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	// cannot get a typed mock Context.
	public void map(final String name, final String value, final String expectedRow, final String expectedName,
			final String expectedValue) throws InterruptedException, IOException {
		final MorphMapper morphMapper = new MorphMapper();
		final StringListMap mapWriter = new StringListMap();

		final Context context = Mockito.mock(Context.class);
		// final Counter counter = Mockito.mock(Counter.class);
		Mockito.when(context.getCounter(MorphMapper.NAME, MorphMapper.PROPERTIES_WRITTEN)).thenReturn(
				Mockito.mock(Counter.class));
		Mockito.when(context.getCounter(MorphMapper.NAME, MorphMapper.LITERAL_REDIRECTS)).thenReturn(
				Mockito.mock(Counter.class));
		mapWriter.add(name, value);
		morphMapper.map(new ImmutableBytesWritable(ROW1.getBytes(Charsets.UTF_8)), mapWriter, null, context);
		Mockito.verify(context).write(new Text(expectedRow), new NamedValueWritable(expectedName, expectedValue));
	}
}
