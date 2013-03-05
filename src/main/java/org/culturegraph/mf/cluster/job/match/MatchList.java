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
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.culturegraph.mf.cluster.mapreduce.Printer;
import org.culturegraph.mf.cluster.pipe.HBaseResultDecoder;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.stream.sink.StringListMap;

/**
 * Takes an {@link HTable} containing matchings (see {@link MatcherOld}) and
 * distills a match list with per-record matches.
 * 
 * @author Markus Michael Geipel
 */

public final class MatchList extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new MatchList(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		addRequiredArguments(ConfigConst.INPUT_TABLE, ConfigConst.OUTPUT_PATH);
		return HBaseConfiguration.create(conf);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configurePropertyTableMapper(job, conf.get(ConfigConst.INPUT_TABLE), EquivalenceListGenerator.class,
				Text.class, Text.class);
		configureTextOutputReducer(job, conf, Printer.forText());
	}

	/**
	 * generates equivalence lists
	 */
	static final class EquivalenceListGenerator extends TableMapper<Text, Text> {

		private final Text currentKey = new Text();
		private final StringListMap listMapWriter = new StringListMap();
		private final Text currentValue = new Text();

		@Override
		public void map(final ImmutableBytesWritable row, final Result result, final Context context)
				throws IOException, InterruptedException {

			HBaseResultDecoder.read(result, listMapWriter);

			final List<String> ids = listMapWriter.get(Matcher.CONTAINS_PROPERTY);
			for (String id1 : ids) {
				currentKey.set(id1);
				for (String id2 : ids) {
					currentValue.set(id2);
					if (!id1.equals(id2)) {
						context.write(currentKey, currentValue);
					}
				}
			}
		}
	}
}
