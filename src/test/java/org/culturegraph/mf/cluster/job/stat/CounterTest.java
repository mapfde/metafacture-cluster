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

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.culturegraph.mf.cluster.job.stat.CounterUtil.CountingReceiver;
import org.junit.Test;
import org.mockito.Mockito;

public final class CounterTest {

	public static final String HAWAII = "Hawaii";
	public static final String OAHU = "Oahu";
	public static final String HULA = "Hula";
	public static final String ALOHA = "Aloha";
	private static final String COUNTERGROUP = "slkf";

	@SuppressWarnings({"unchecked", "rawtypes"}) // cannot get a typed mock Context.
	@Test
	public void testCounterUtil() throws IOException, InterruptedException {
		
		final CountingReceiver receiver = new CountingReceiver();
		receiver.literal (HAWAII, HULA);
		receiver.literal(OAHU, HULA);
				
		final Context context = Mockito.mock(Context.class);
		final Counter counter = Mockito.mock(Counter.class);
		Mockito.when(context.getCounter(COUNTERGROUP, CounterUtil.PROPTERTIES_COUNTED)).thenReturn(counter);
		
		CounterUtil.writeCounts(receiver, context, COUNTERGROUP);
		Mockito.verify(counter).increment(2);
		Mockito.verify(context).write(new Text(HAWAII+HULA), new IntWritable(1));
		Mockito.verify(context).write(new Text(OAHU+HULA), new IntWritable(1));
	}

}
