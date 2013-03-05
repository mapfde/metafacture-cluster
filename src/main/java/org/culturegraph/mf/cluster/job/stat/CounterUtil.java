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
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.framework.DefaultStreamReceiver;

import com.google.common.base.Charsets;

final class CounterUtil {
	public static final String PROPTERTIES_COUNTED = "properties counted";
	private static final IntWritable ONE = new IntWritable(1);
	

	private CounterUtil() {
		// no instances
	}

	public static void writeCounts(final CountingReceiver receiver,
			final Mapper<? extends Writable, ? extends Writable, Text, IntWritable>.Context context, final String counterName)
			throws IOException, InterruptedException {
		for (Text key : receiver.getList()) {
			context.write(key, ONE);
		}
		context.getCounter(counterName, PROPTERTIES_COUNTED).increment(receiver.getList().size());
	}

	static final class CountingReceiver extends DefaultStreamReceiver {
		private final List<Text> list = new ArrayList<Text>();

		public List<Text> getList() {
			return list;
		}

		@Override
		public void startRecord(final String identifier) {
			list.clear();
		}

		@Override
		public void literal(final String name, final String value) {
			final Text key = new Text(name);
			final byte[] valueBytes = value.getBytes(Charsets.UTF_8);
			key.append(valueBytes, 0, valueBytes.length);
			list.add(key);
		}
	}
}
