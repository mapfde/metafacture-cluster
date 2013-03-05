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

import java.io.IOException;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.MorphErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * persists data to HBase
 * 
 * @author Markus Michael Geipel
 */

public final class HTableReducer {

	// private static final Logger LOG =
	// LoggerFactory.getLogger(HTableReducer.class);

	private HTableReducer() {
		// no instances
	}

	public static final class Generic extends TableReducer<ImmutableBytesWritable, ImmutableBytesWritable, Writable> {

		private final ComplexPutWriter putWriter = new ComplexPutWriter();

		@Override
		public void reduce(final ImmutableBytesWritable key, final Iterable<ImmutableBytesWritable> values,
				final Context context) throws IOException, InterruptedException {

			putWriter.startRecord(key.toString());
			for (ImmutableBytesWritable value : values) {
				putWriter.literal(value.copyBytes());
			}
			putWriter.endRecord();
			context.write(key, putWriter.getCurrentPut());
		}
	}

	public static final class ForNamedValue extends TableReducer<Text, NamedValueWritable, Writable> implements
			MorphErrorHandler {

		private final ComplexPutWriter putWriter = new ComplexPutWriter();
		private StreamReceiver receiver;
		private static final Logger LOG = LoggerFactory.getLogger(HTableReducer.ForNamedValue.class);

		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {

			final String morphDef = context.getConfiguration().get(ConfigConst.REDUCE_MORPH_DEF);
			if (morphDef != null && !morphDef.isEmpty()) {
				final Metamorph metamorph = new Metamorph(morphDef);
				metamorph.setErrorHandler(this);
				metamorph.setReceiver(putWriter);
				receiver = metamorph;
			} else {
				receiver = putWriter;
			}
		}

		@Override
		public void reduce(final Text key, final Iterable<NamedValueWritable> namedValues, final Context context)
				throws IOException, InterruptedException {

			receiver.startRecord(key.toString());
			for (NamedValueWritable namedValue : namedValues) {
				receiver.literal(namedValue.getName(), namedValue.getValue());
			}
			receiver.endRecord();

			try {
				final Put put = putWriter.getCurrentPut();
				if (put.isEmpty()) {
					context.getCounter("HTableReducer", "empty puts").increment(1);
				} else {
					context.write(key, put);
				}
			} catch (RetriesExhaustedWithDetailsException e) {
				for (Throwable throwable : e.getCauses()) {
					LOG.error(throwable.getMessage(), throwable);
					throwable.printStackTrace();
				}
				throw new CulturegraphClusterException(e);
			}
		}

		@Override
		public void error(final Exception exception) {
			LOG.error("Exception in Metamorph", exception);
		}
	}
}