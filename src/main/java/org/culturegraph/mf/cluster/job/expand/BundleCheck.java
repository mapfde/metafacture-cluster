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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.cluster.inputformat.MultiTableInputFormat;
import org.culturegraph.mf.cluster.mapreduce.KeyChangeMapper;
import org.culturegraph.mf.cluster.mapreduce.ListMapReducer;
import org.culturegraph.mf.cluster.mapreduce.MorphMapper;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.cluster.type.StringListMapWritable;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.cluster.util.ConfigChecker;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.cluster.util.MapReduceUtil;
import org.culturegraph.mf.types.ListMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Markus Michael Geipel

 */

public final class BundleCheck extends Configured implements Tool {

	private static final Logger LOG = LoggerFactory.getLogger(BundleCheck.class);

	private final ConfigChecker configChecker = new ConfigChecker();

	public BundleCheck() {
		super();
		configChecker.addOptional(ConfigConst.KEY_NAME);
		configChecker.addRequired(ConfigConst.INPUT_TABLE, ConfigConst.OUTPUT_PATH,
				ConfigConst.MORPH_DEF);
	}
	
	public static void main(final String[] args) {
		try {
			System.exit(ToolRunner.run(new BundleCheck(), args));
		} catch (Exception e) {
			throw new CulturegraphClusterException("Error during job launch", e);
		}
	}
	


	@Override
	public int run(final String[] args) throws Exception {
		
		if (configChecker.logAndVerify(LOG, getConf())) {
			final String tmp = MapReduceUtil.makeTmp(getConf()) + "out/";
			runDataCollectJob(tmp);
			runCheckJob(tmp);
			return 1;
		}
		return -1;
	}

	private void runCheckJob(final String tmp) throws IOException, InterruptedException, ClassNotFoundException {

		final Job mergeDataAndCheckJob = new Job(getConf(), "merge data and check");
		mergeDataAndCheckJob.setJarByClass(BundleCheck.class);
		mergeDataAndCheckJob.setInputFormatClass(SequenceFileInputFormat.class);
		FileInputFormat.addInputPath(mergeDataAndCheckJob, new Path(tmp));
		mergeDataAndCheckJob.setMapperClass(KeyChangeMapper.class);
		mergeDataAndCheckJob.setMapOutputKeyClass(Text.class);
		mergeDataAndCheckJob.setMapOutputValueClass(StringListMapWritable.class);

		mergeDataAndCheckJob.setReducerClass(CheckReducer.class);
		mergeDataAndCheckJob.setOutputFormatClass(TextOutputFormat.class);
		FileOutputFormat.setOutputPath(mergeDataAndCheckJob, new Path(getConf().get(ConfigConst.OUTPUT_PATH)));

		mergeDataAndCheckJob.waitForCompletion(true);
		
	}

	private void runDataCollectJob(final String tmp) throws IOException, InterruptedException, ClassNotFoundException {
		

		final Job collectDataJob = new Job(getConf(), "collect data");
		collectDataJob.setJarByClass(BundleCheck.class);

		final Scan scan = new Scan();
		scan.addFamily(Column.Family.PROPERTY);
		scan.setCaching(getConf().getInt(ConfigConst.CACHED_ROWS, ConfigConst.DEFAULT_CACHED_ROWS));
		scan.setCacheBlocks(false);
		MultiTableInputFormat.initTableMapperJob(MorphMapper.class, Text.class, Text.class, collectDataJob);
		MultiTableInputFormat.setTablesInJob(collectDataJob, getConf().get(ConfigConst.INPUT_TABLE), scan);

		MorphMapper.configureJob(collectDataJob);
		
		collectDataJob.setReducerClass(ListMapReducer.fromNamedValues());
		collectDataJob.setOutputFormatClass(SequenceFileOutputFormat.class);
		collectDataJob.setOutputKeyClass(Text.class);
		collectDataJob.setOutputValueClass(StringListMapWritable.class);
		FileOutputFormat.setOutputPath(collectDataJob, new Path(tmp));

		collectDataJob.setNumReduceTasks(2);
		collectDataJob.waitForCompletion(true);
		
	}

	public static final class CheckReducer extends Reducer<Text, StringListMapWritable, Text, NamedValueWritable> {
		private static final String NAME = "CheckReducer";
		private final Set<String> tempSet = new HashSet<String>();

		@Override
		protected void reduce(final Text key, final Iterable<StringListMapWritable> values, final Context context)
				throws java.io.IOException, InterruptedException {
			final Set<String> properties = new HashSet<String>();
			final List<ListMap<String, String>> records = new ArrayList<ListMap<String, String>>();
			
			for (StringListMapWritable writable : values) {
				final ListMap<String, String> record = writable.detachListMap();
				properties.addAll(record.keySet());
				records.add(record);
			}

			for (String property : properties) {
				tempSet.clear();
				for (ListMap<String, String> record : records) {
					tempSet.addAll(record.get(property));
				}
				
				if (tempSet.size() > 1) {
					context.write(key, new NamedValueWritable("conflict", property
							+ ":" + tempSet));
					context.getCounter(NAME, "conflicts in " + property).increment(1);
				}
				
			}
			context.getCounter(NAME, "properties checked").increment(properties.size());
			context.getCounter(NAME, "records checked").increment(records.size());
			context.getCounter(NAME, "bundles checked").increment(1);
		}
	}
}
