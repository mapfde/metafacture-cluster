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
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.culturegraph.mf.cluster.job.stat.CounterUtil.CountingReceiver;
import org.culturegraph.mf.cluster.mapreduce.AbstractTableMorphMapper;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.morph.Metamorph;

/**
 * Computes histograms on data in an {@link HTable}. Which pieces of data are counted is defined with a {@link Metamorph} definition.
 * 
 * @author Markus Michael Geipel
 */
public final class HTableCounter extends AbstractJobLauncher {
	
	private static final String NAME = "Counter";

	
	public static void main(final String[] args){
		launch(new HTableCounter(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName("Count '" + getConf().get(ConfigConst.MORPH_DEF) + "' in '" + getConf().get(ConfigConst.INPUT_TABLE) +"'");
		addRequiredArguments(ConfigConst.OUTPUT_PATH);
		addRequiredArguments(AbstractTableMorphMapper.REQUIREMENTS);
		return HBaseConfiguration.create(getConf());
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configurePropertyTableMapper(job, conf, CountMapper.class, Text.class,IntWritable.class);
		job.setCombinerClass(IntSumReducer.class);
		configureTextOutputReducer(job, conf, IntSumReducer.class);
	}

	static final class CountMapper extends AbstractTableMorphMapper<CountingReceiver, Text, IntWritable>{

		@Override
		protected CountingReceiver createStreamReceiver() {
			return new CountingReceiver();
		}

		@Override
		protected String getCounterName() {
			return NAME;
		}

		@Override
		protected void map(final ImmutableBytesWritable row, final CountingReceiver data, final Result raw,
				final Context context) throws InterruptedException, IOException {
				CounterUtil.writeCounts(data, context, NAME);
		}
	}

}
