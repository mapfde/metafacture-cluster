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

package org.culturegraph.mf.cluster.job.beacon;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.exceptions.MorphException;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.MorphErrorHandler;
import org.culturegraph.mf.stream.converter.bib.PicaDecoder;
import org.culturegraph.mf.stream.sink.StringListMap;
import org.culturegraph.mf.types.ListMap;

/**
 * calculates a BEACON file based on a pica dump in the HDFS.
 * 
 * @author Markus Michael Geipel
 */
public final class FileBeacon extends AbstractJobLauncher {

	private static final String VALID_KEY = "valid";
	private static final String REF_TYPE = "_ref_type";
	private static final String REF_TARGET = "_ref_target";
	private static final String BEACON = "Beacon";

	public static void main(final String[] args) {
		launch(new FileBeacon(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		addRequiredArguments(ConfigConst.INPUT_PATH, ConfigConst.MORPH_DEF, ConfigConst.OUTPUT_PATH,
				ConfigConst.STORE_RAW_DATA);
		conf.setIfUnset(ConfigConst.STORE_RAW_DATA, "false");
		return HBaseConfiguration.create(getConf());
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configureFileInputMapper(job, conf, RefMapper.class, Text.class, Text.class);
		job.setNumReduceTasks(1);
		configureTextOutputReducer(job, conf, BeaconReducer.class);
	}

	/**
	 * extracts backrefs
	 */
	static final class RefMapper extends Mapper<LongWritable, Text, Text, Text> implements MorphErrorHandler {

		private static final String ERROR_IN_ROW = "Error in row %1$s: %2$s";
		private static final byte[] VALID_KEY_BYTES = VALID_KEY.getBytes();
		private static final Log LOG = LogFactory.getLog(RefMapper.class);

		private final StringListMap listMapWriter = new StringListMap();
		private final Text tempText = new Text();

		private final Text currentRowId = new Text();
		private final Text refTarget = new Text();
		private Context currentContext;
		private Metamorph metamorph;

		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {
			final Configuration conf = context.getConfiguration();
			metamorph = new Metamorph(conf.get(ConfigConst.MORPH_DEF));
			metamorph.setErrorHandler(this);
			metamorph.setReceiver(listMapWriter);
		}

		@Override
		public void map(final LongWritable row, final Text recordText, final Context context) throws IOException {
			currentContext = context;

			final String record = recordText.toString();
			if (record.isEmpty()) {
				context.getCounter(BEACON, "empty records").increment(1);
				return;
			}
			currentRowId.set("NO ID");
			try {

				currentRowId.set(PicaDecoder.extractIdFromRecord(record));
				PicaDecoder.process(record, metamorph);

				final String valid = listMapWriter.getFirst(VALID_KEY);
				if (valid != null) {

					tempText.set(VALID_KEY_BYTES);
					context.write(currentRowId, tempText);
					context.getCounter(BEACON, "valid ref targets").increment(1);
				}
				String value = listMapWriter.getFirst(REF_TYPE);

				if (value == null) {
					value = "undef";
					context.getCounter(BEACON, "undefined reference holders").increment(1);
				}
				tempText.set(value);

				for (String ref : listMapWriter.get(REF_TARGET)) {
					refTarget.set(ref);
					context.write(refTarget, tempText);
					context.getCounter(BEACON, "references").increment(1);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (MorphException exception) {
				error(exception);
			}

		}

		@Override
		public void error(final Exception exception) {
			LOG.warn(String.format(ERROR_IN_ROW, currentRowId.toString(), exception.getMessage()));
			currentContext.setStatus(currentRowId.toString() + ": " + exception.getClass().getSimpleName() + " "
					+ exception.getMessage());
			currentContext.getCounter(BEACON, exception.getClass().getSimpleName()).increment(1);
		}
	}

	/**
	 *
	 */
	static final class BeaconReducer extends Reducer<Text, Text, Text, Text> {

		private static final String ALL_DEL = "all\t";
		private final ListMap<String, String> inputListMap = new ListMap<String, String>();
		private final Text outText = new Text();

		@Override
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException,
				InterruptedException {

			inputListMap.clear();
			for (Text value : values) {
				inputListMap.add(value.toString(), "");
			}
			if (inputListMap.existsKey(VALID_KEY)) {
				int totalCount = 0;
				inputListMap.removeKey(VALID_KEY);
				for (Entry<String, List<String>> entry : inputListMap.entrySet()) {
					final int count = entry.getValue().size();
					outText.set(entry.getKey() + "\t" + count);
					context.write(key, outText);
					totalCount += count;
				}
				if (totalCount > 0) {
					context.getCounter(BEACON, "referenced entities").increment(1);
					outText.set(ALL_DEL + totalCount);
					context.write(key, outText);
				}
			}
		}
	}

}
