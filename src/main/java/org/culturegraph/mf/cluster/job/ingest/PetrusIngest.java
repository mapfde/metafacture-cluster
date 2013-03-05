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

package org.culturegraph.mf.cluster.job.ingest;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.cluster.util.ConfigConst;

import com.google.common.base.Charsets;

/**
 * Reads text data from hdfs and persists it in an {@link HTable}. Record format: ID\tTYPE\tTEXT.
 * 
 * @author Markus Michael Geipel
 */
public final class PetrusIngest extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new PetrusIngest(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration config) {

		addRequiredArguments(ConfigConst.OUTPUT_TABLE, ConfigConst.INPUT_PATH);
		setJobName("Ingest " + config.get(ConfigConst.INPUT_PATH) + " -> " + config.get(ConfigConst.OUTPUT_TABLE));
		config.setIfUnset("mapred.map.tasks.speculative.execution", "false");
		return HBaseConfiguration.create(config);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		job.setJarByClass(PetrusIngest.class);
		final Path path = new Path(conf.get(ConfigConst.INPUT_PATH));
		FileInputFormat.addInputPath(job, path);

		job.setMapperClass(IngestMapper.class);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(NullOutputFormat.class);
	}

	/**
	 * writes raw records and properties to htable
	 */
	static final class IngestMapper<K, V> extends Mapper<LongWritable, Text, K, V> {

		private static final String SEPARATOR = "\t";
		private static final String INGEST = "Ingest";

		private static final long WRITE_BUFFER = 1024 * 1024 * 16;

		private HTable htable;

		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {
			super.setup(context);
			final Configuration conf = context.getConfiguration();
			htable = new HTable(conf, conf.get(ConfigConst.OUTPUT_TABLE));
			htable.setAutoFlush(false);
			htable.setWriteBufferSize(WRITE_BUFFER);
		}

		@Override
		protected void cleanup(final Context context) throws IOException, InterruptedException {
			super.cleanup(context);
			htable.flushCommits();
			htable.close();
		}

		@Override
		public void map(final LongWritable row, final Text value, final Context context) throws IOException {

			int cut = 0;
			int nextCut;
			nextCut = value.find(SEPARATOR);
			cut = nextCut;
			final String identifier = Text.decode(value.getBytes(), 0, cut-1);
			nextCut = value.find(SEPARATOR, cut + 1);
			final String type = Text.decode(value.getBytes(), cut+1, nextCut - cut-1);
			cut = nextCut;
			final String text = Text.decode(value.getBytes(), cut+1, value.getLength() - cut-1);
			final Put put = new Put(identifier.getBytes(Charsets.UTF_8));
			put.add(Column.Family.RAW, type.getBytes(Charsets.UTF_8), text.getBytes(Charsets.UTF_8));
			ComplexPutWriter.write(put, "cg:raw", type);
			put.add(Column.Family.PROPERTY, Column.Family.RAW, type.getBytes(Charsets.UTF_8));
			htable.put(put);
			context.getCounter(INGEST, type).increment(1);
		}
	}
}
