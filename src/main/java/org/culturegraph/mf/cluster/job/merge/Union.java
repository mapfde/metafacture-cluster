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

package org.culturegraph.mf.cluster.job.merge;

import java.io.IOException;
import java.util.UUID;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigChecker;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.cluster.util.TextArrayWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Markus Michael Geipel
 */

public final class Union extends Configured implements Tool {

	public static final Text SEPARATED = new Text("S");
	public static final Text OPEN = new Text("O");
	public static final String UNION_FIND = "union find";
	public static final String OPEN_CLASSES = "open classes";
	public static final String SEPARATED_CLASSES = "separated classes";

	private static final Logger LOG = LoggerFactory.getLogger(Union.class);
	private final ConfigChecker configChecker = new ConfigChecker();

	public Union() {
		super();
		configChecker.addRequired(ConfigConst.INPUT_TABLE, ConfigConst.OUTPUT_TABLE, ConfigConst.MORPH_DEF);
	}

	public static void main(final String[] args) {
		try {
			System.exit(ToolRunner.run(new Union(), args));
		} catch (Exception e) {
			throw new CulturegraphClusterException("Error during job launch", e);
		}
	}

	@Override
	public int run(final String[] args) throws Exception {
		final String tmp = makeTmp();

		if (!configChecker.logAndVerify(LOG, getConf())) {
			return -1;
		}

		Job job;
		boolean ongoingMerges = true;

		job = new Job(getConf(), "initial explode");
		job.setSpeculativeExecution(false);
		job.setJarByClass(Union.class);
		AbstractJobLauncher.configurePropertyTableMapper(job, getConf(), InputTableMapper.class, Text.class,
				TextArrayWritable.class);
		configureReducer(job, ExplodeReducer.class, new Path(tmp + "explode_0"), SequenceFileOutputFormat.class);
		job.setNumReduceTasks(2);
		job.waitForCompletion(true);

		int count = 0;
		while (ongoingMerges) {

			job = new Job(getConf(), "recollect");

			job.setJarByClass(Union.class);
			configureMapper(job, RecollectMapper.class, new Path(tmp + "explode_" + count),
					SequenceFileInputFormat.class);

			configureReducer(job, RecollectReducer.class, new Path(tmp + "recollect_" + count),
					SequenceFileOutputFormat.class);
			job.setNumReduceTasks(2);
			job.waitForCompletion(true);

			job = new Job(getConf(), "explode");
			job.setJarByClass(Union.class);
			configureMapper(job, ExplodeMapper.class, new Path(tmp + "recollect_" + count),
					SequenceFileInputFormat.class);
			++count;
			configureReducer(job, ExplodeReducer.class, new Path(tmp + "explode_" + count),
					SequenceFileOutputFormat.class);
			job.setNumReduceTasks(2);
			job.waitForCompletion(true);

			ongoingMerges = job.getCounters().getGroup(UNION_FIND).findCounter(OPEN_CLASSES).getValue() != 0;
			LOG.info("ongoingMerges=" + ongoingMerges);
		}

		job = new Job(HBaseConfiguration.create(getConf()), "collect result");
		job.setSpeculativeExecution(false);
		job.setJarByClass(Union.class);
		final Path path = new Path(tmp + "recollect_*");
		FileInputFormat.addInputPath(job, path);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapperClass(ResultMapper.class);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(NullOutputFormat.class);
		job.waitForCompletion(true);

		return 1;
	}

	private String makeTmp() throws IOException {
		final String temporary = "tmp/unionfind/" + UUID.randomUUID() + "/";
		final Path tmp = new Path(temporary);
		final FileSystem fileSys = tmp.getFileSystem(getConf());
		fileSys.mkdirs(tmp);
		fileSys.deleteOnExit(tmp);
		return temporary;
	}

	private static void configureMapper(final Job job,
			final Class<? extends Mapper<?, ?, Text, TextArrayWritable>> mapper, final Path input,
			final Class<? extends InputFormat> inputFormat) throws IOException {
		job.setJarByClass(AbstractJobLauncher.class);
		FileInputFormat.addInputPath(job, input);
		job.setMapperClass(mapper);
		job.setInputFormatClass(inputFormat);
	}

	private static void configureReducer(final Job job,
			final Class<? extends Reducer<Text, TextArrayWritable, Text, TextArrayWritable>> reducer,
			final Path output, final Class<? extends OutputFormat> outputFormat) throws IOException {
		job.setReducerClass(reducer);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(TextArrayWritable.class);
		job.setOutputFormatClass(outputFormat);
		FileOutputFormat.setOutputPath(job, output);
	}

}
