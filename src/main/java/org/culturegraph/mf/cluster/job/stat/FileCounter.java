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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.culturegraph.mf.cluster.job.stat.CounterUtil.CountingReceiver;
import org.culturegraph.mf.cluster.mapreduce.AbstractFileMorphMapper;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.morph.Metamorph;

/**
 * Computes histograms on data in the HDFS. Which pieces of data are counted is
 * defined with a {@link Metamorph} definition.
 * 
 * @author Markus Michael Geipel
 */
public final class FileCounter extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new FileCounter(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName("Count '" + getConf().get(ConfigConst.MORPH_DEF) + "' in '" + getConf().get(ConfigConst.INPUT_PATH)
				+ "'");

		addRequiredArguments(ConfigConst.OUTPUT_PATH);
		addRequiredArguments(AbstractFileMorphMapper.REQUIREMENTS);
		return getConf();
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configureFileInputMapper(job, conf, CountMapper.class, Text.class, IntWritable.class);
		job.setCombinerClass(IntSumReducer.class);
		configureTextOutputReducer(job, conf, IntSumReducer.class);
	}

	static final class CountMapper extends AbstractFileMorphMapper<CountingReceiver, Text, IntWritable> {
		private static final String COUNTER = CountMapper.class.getSimpleName();

		@Override
		protected CountingReceiver createStreamReceiver() {
			return new CountingReceiver();
		}

		@Override
		protected String getCounterName() {
			return COUNTER;
		}

		@Override
		protected void map(final LongWritable row, final CountingReceiver data, final Text rawInput,
				final Context context) throws IOException, InterruptedException {
			CounterUtil.writeCounts(data, context, COUNTER);
		}
	}
}
