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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.MorphErrorHandler;
import org.culturegraph.mf.stream.reader.MultiFormatReader;
import org.culturegraph.mf.stream.reader.Reader;

import com.google.common.base.Charsets;

/**
 * Reads bibliographic data from hdfs, transforms it with {@link Metamorph} and
 * persists it in an {@link HTable}
 * 
 * @author Markus Michael Geipel
 */
public final class BibIngest extends AbstractJobLauncher {

	public static void main(final String[] args) {
		launch(new BibIngest(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration config) {

		addRequiredArguments(ConfigConst.OUTPUT_TABLE, ConfigConst.FORMAT, ConfigConst.INGEST_PREFIX,
				ConfigConst.INPUT_PATH, ConfigConst.MORPH_DEF);
		addOptionalArguments(ConfigConst.STORE_RAW_DATA, "hbase.zookeeper.quorum");

		final Configuration conf = HBaseConfiguration.create(config);
		conf.setIfUnset(ConfigConst.STORE_RAW_DATA, ConfigConst.FALSE);

		conf.setIfUnset("mapred.map.tasks.speculative.execution", "false");

		setJobName("Ingest " + config.get(ConfigConst.INPUT_PATH) + " -> " + config.get(ConfigConst.OUTPUT_TABLE));
		return conf;
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		job.setJarByClass(BibIngest.class);

		final Path path = new Path(conf.get(ConfigConst.INPUT_PATH));
		FileInputFormat.addInputPath(job, path);

		job.setMapperClass(IngestMapper.class);
		job.setNumReduceTasks(0);

		job.setOutputFormatClass(NullOutputFormat.class);
	}

	/**
	 * writes raw records and properties to htable
	 */
	static final class IngestMapper<K, V> extends Mapper<LongWritable, Text, K, V> implements MorphErrorHandler {
		private static final String ERROR_IN_ROW = "Error in row %1$s: %2$s %3$s";

		private static final Log LOG = LogFactory.getLog(BibIngest.class);

		private static final String INGEST = "Ingest";
		private static final String EXCEPTION = "cg:mappingError";

		// private static final byte[] TYPE_TITLE =
		// "cg:type\0bib".getBytes(Charsets.UTF_8);

		private static final long WRITE_BUFFER = 1024 * 1024 * 16;

		private Reader reader;
		private ComplexPutWriter collector;
		private HTable htable;
		private byte[] format;
		private boolean storeRawData;
		private Context currentContext;

		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {
			super.setup(context);
			final Configuration conf = context.getConfiguration();

			storeRawData = conf.getBoolean(ConfigConst.STORE_RAW_DATA, false);
			format = conf.get(ConfigConst.FORMAT).getBytes();
			reader = new MultiFormatReader(conf.get(ConfigConst.FORMAT));
			collector = new ComplexPutWriter();
			collector.setIdPrefix(conf.get(ConfigConst.INGEST_PREFIX, ""));
			final Metamorph metamorph = new Metamorph(conf.get(ConfigConst.MORPH_DEF));
			metamorph.setErrorHandler(this);
			reader.setReceiver(metamorph);

	
			metamorph.setReceiver(collector);


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
			currentContext = context;

			final String record = value.toString();
			if (record.isEmpty()) {
				context.getCounter(INGEST, "empty input lines").increment(1);
				return;
			}

			try {
				collector.reset();
				reader.read(record);
				final Put put = collector.getCurrentPut();
				if (put == null) {
					return;
				}

				if (storeRawData) {
					put.add(Column.Family.RAW, format, record.getBytes());
				}

				if (put.isEmpty()) {
					context.getCounter(INGEST, "records without content").increment(1);
				} else {
					htable.put(put);
					context.getCounter(INGEST, "records ingested").increment(1);
				}
			} catch (RuntimeException e) {
				error(e);
			}

		}

		@Override
		public void error(final Exception exception) {
			final String row;
			if (collector.getCurrentPut() == null) {
				row = "";
			} else {
				collector.literal(EXCEPTION, exception.getClass().getSimpleName() + ":" + exception.getMessage());
				row = new String(collector.getCurrentPut().getRow(), Charsets.UTF_8);
			}

			LOG.warn(String.format(ERROR_IN_ROW, row, exception.getClass().getSimpleName(), exception.getMessage()));
			currentContext.getCounter(INGEST, exception.getClass().getSimpleName()).increment(1);
			currentContext.setStatus("Last exception: " + exception.getClass().getSimpleName() + " "
					+ exception.getMessage());
		}
	}

//	private static final class PrefixAdder extends DefaultSender<StreamReceiver> implements StreamReceiver {
//
//		private final String prefix;
//
//		public PrefixAdder(final String prefix) {
//			super();
//			this.prefix = prefix;
//		}
//
//		@Override
//		public void startRecord(final String identifier) {
//			getReceiver().startRecord(prefix + identifier);
//		}
//
//		@Override
//		public void endRecord() {
//			getReceiver().endRecord();
//		}
//
//		@Override
//		public void startEntity(final String name) {
//			getReceiver().startEntity(name);
//		}
//
//		@Override
//		public void endEntity() {
//			getReceiver().endEntity();
//		}
//
//		@Override
//		public void literal(final String name, final String value) {
//			getReceiver().literal(name, value);
//
//		}
//		
//		
//	}
}
