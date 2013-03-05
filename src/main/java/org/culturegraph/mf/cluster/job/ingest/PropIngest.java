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
import org.culturegraph.mf.cluster.util.ConfigConst;

/**
 * Reads property records from hdfs and
 * persists them in an {@link HTable}
 * 
 * @author Markus Michael Geipel
 */
public final class PropIngest extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new PropIngest(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration config) {

		addRequiredArguments(ConfigConst.OUTPUT_TABLE, ConfigConst.INPUT_PATH);

		final Configuration conf = HBaseConfiguration.create(config);
		config.setIfUnset("mapred.map.tasks.speculative.execution", "false");
		setJobName("Ingest " + config.get(ConfigConst.INPUT_PATH) + " -> " + config.get(ConfigConst.OUTPUT_TABLE));
		return conf;
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		job.setJarByClass(PropIngest.class);
		final Path path = new Path(conf.get(ConfigConst.INPUT_PATH));
		FileInputFormat.addInputPath(job, path);

		job.setMapperClass(IngestMapper.class);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(NullOutputFormat.class);
	}

	/**
	 * writes raw records and properties to htable
	 */
	static final class IngestMapper<K, V> extends Mapper<LongWritable, Text, K, V>{

	//	private static final Log LOG = LogFactory.getLog(PropIngest.class);

		private static final String INGEST = "Ingest";

		private static final long WRITE_BUFFER = 1024 * 1024 * 16;

		private static final String SEPARATOR = "\t";

		private final ComplexPutWriter collector = new ComplexPutWriter();
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

			final String record = value.toString();
			if (record.isEmpty()) {
				context.getCounter(INGEST, "empty input lines").increment(1);
				return;
			}
			collector.reset();
			
			final String[] parts = record.split(SEPARATOR);
			
			collector.startRecord(parts[0]);
			for (int i = 1; i < parts.length; ++i) {
				final String prop = parts[i];
				final int cut = prop.indexOf('=');
				if(cut>0 && cut + 1 < prop.length()){
					collector.literal(prop.substring(0, cut), prop.substring(cut+1));
				}
			}
			collector.endRecord();

			final Put put = collector.getCurrentPut();
			if (put.isEmpty()) {
				context.getCounter(INGEST, "records without content").increment(1);
			} else {
				htable.put(put);
				context.getCounter(INGEST, "records ingested").increment(1);
			}
		}
	}
}
