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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.cluster.pipe.HBaseResultDecoder;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.cluster.util.Histogram;
import org.culturegraph.mf.stream.sink.StringListMap;

/**
 * Takes an {@link HTable} containing matchings (see {@link MatcherOld}) and
 * distills an overlap statistic.
 * 
 * @author Markus Michael Geipel
 */
public final class MatchOverlap extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new MatchOverlap(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		addRequiredArguments(ConfigConst.INPUT_TABLE, ConfigConst.OUTPUT_PATH);
		return HBaseConfiguration.create(conf);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configurePropertyTableMapper(job, conf.get(ConfigConst.INPUT_TABLE), EquivalenceListGenerator.class,
				Text.class, IntWritable.class);
		job.setCombinerClass(IntSumReducer.class);
		configureTextOutputReducer(job, conf, IntSumReducer.class);
	}

	/**
	 * generates equivalence lists
	 */
	static final class EquivalenceListGenerator extends TableMapper<Text, IntWritable> {

		private final Text currentKey = new Text();
		private final List<String> currentIds = new ArrayList<String>();
		// private final String[] nameValue = new String[2];
		private final StringListMap listMapWriter = new StringListMap();
		private final Histogram<String> histogramm = new Histogram<String>();

		@Override
		public void map(final ImmutableBytesWritable row, final Result result, final Context context)
				throws IOException, InterruptedException {

			HBaseResultDecoder.read(result, listMapWriter);
			final String id = listMapWriter.getId();
			final int algIndex = id.indexOf(Matcher.ALG_PREFIX_DELIMITER);
			if (algIndex < 1) {
				throw new IllegalArgumentException("id '" + id + "' is not a valid algorithm id!");
			}
			final String algorithm = id.substring(0, algIndex);

			// build curentIds
			try {
				currentIds.clear();
				for (String contains : listMapWriter.get(Matcher.CONTAINS_PROPERTY)) {
					final int instIndex = contains.indexOf('-');
					currentIds.add(contains.substring(0, instIndex));
				}
			} catch (IndexOutOfBoundsException e) {
				throw new CulturegraphClusterException("Wrong data format in row " + listMapWriter.getId(), e);
			}

			// build Histogramm
			histogramm.clear();
			histogramm.incrementAll(currentIds);

			// build overlap
			for (String id1 : histogramm.keySet()) {
				for (String id2 : histogramm.keySet()) {
					currentKey.set(algorithm + "$" + id1 + "&" + id2);
					final int overlap;
					if (id1.equals(id2)) {
						overlap = histogramm.get(id1) - 1;
					} else {
						overlap = histogramm.get(id2);
					}
					context.write(currentKey, SmallIntWritableCache.getIntWritable(overlap));
				}
			}
		}

		private static final class SmallIntWritableCache {
			private static final int CACHE_SIZE = 20;
			private static IntWritable[] cache;

			private SmallIntWritableCache() {
				// no instances
			}

			static {
				cache = new IntWritable[CACHE_SIZE];
				for (int i = 0; i < cache.length; ++i) {
					cache[i] = new IntWritable(i);
				}
			}

			public static IntWritable getIntWritable(final int integer) {
				if (integer >= 0 && integer < cache.length) {
					return cache[integer];
				}
				return new IntWritable(integer);
			}
		}
	}
}
