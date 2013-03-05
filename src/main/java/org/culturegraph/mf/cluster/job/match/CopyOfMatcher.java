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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.culturegraph.mf.cluster.inputformat.MultiTableInputFormat;
import org.culturegraph.mf.cluster.mapreduce.AbstractTableMorphMapper;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.sink.ValueSet;
import org.culturegraph.mf.types.CGEntity;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

/**
 * Matches records in an {@link HTable} based on a {@link Metamorph} based match
 * definition. The result goes to another {@link HTable}
 * 
 * @author Markus Michael Geipel
 */
public final class CopyOfMatcher extends AbstractJobLauncher {

	public static final String JOB_NAME = "Match";

	public static final String CONTAINS_PROPERTY = "cg:contains";
	public static final char ALG_PREFIX_DELIMITER = '$';

	public static final String EQUAL_PROPERTY = "cg:eq";
	public static final String EQUAL_DEL = EQUAL_PROPERTY + CGEntity.SUB_DELIMITER;
	public static final byte[] EQUAL_DEL_BYTES = EQUAL_DEL.getBytes(Charsets.UTF_8);

	public static final String CONTAINS_DEL = CONTAINS_PROPERTY + CGEntity.SUB_DELIMITER;
	public static final byte[] CONTAINS_DEL_BYTES = CONTAINS_DEL.getBytes(Charsets.UTF_8);

	public static void main(final String[] args) {
		launch(new CopyOfMatcher(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName(JOB_NAME + " '" + getConf().get(ConfigConst.INPUT_TABLE) + "' with '"
				+ getConf().get(ConfigConst.MORPH_DEF) + "'");

		addOptionalArguments(ConfigConst.ALGORITHM_NAME);
		addRequiredArguments(ConfigConst.MORPH_DEF, ConfigConst.INPUT_TABLE, ConfigConst.OUTPUT_TABLE);

		final String metamorphDef = conf.get(ConfigConst.MORPH_DEF);
		if (null != metamorphDef) {
			final String name = new Metamorph(metamorphDef).getValue(Metamorph.METADATA, "name");
			conf.setIfUnset(ConfigConst.ALGORITHM_NAME, name);
		}

		return HBaseConfiguration.create(conf);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		// configurePropertyTableMapper(job, conf, UniquePropertiesMapper.class,
		// Text.class, Text.class);

		final Scan scan = new Scan();
		scan.addFamily(Column.Family.PROPERTY);
		scan.setCaching(getConf().getInt(ConfigConst.CACHED_ROWS, ConfigConst.DEFAULT_CACHED_ROWS));
		scan.setCacheBlocks(false);
		MultiTableInputFormat.initTableMapperJob(UniquePropertiesMapper.class, Text.class, Text.class, job);
		MultiTableInputFormat.setTablesInJob(job, conf.get(ConfigConst.INPUT_TABLE), scan);

		TableMapReduceUtil.initTableReducerJob(conf.get(ConfigConst.OUTPUT_TABLE), UniquePropertiesReducer.class, job);
		job.setNumReduceTasks(2);
	}

	/**
	 * extracts identifying properties from the raw records
	 */
	static final class UniquePropertiesMapper extends AbstractTableMorphMapper<ValueSet, Text, Text> {

		public static final String COUNTER_EQU = "equivalence notices";

		private final Text currentKey = new Text();
		private final Text currentRow = new Text();
		private final Text currentEquivalent = new Text();
		private byte[] algorithm = ("unknown" + ALG_PREFIX_DELIMITER).getBytes(Charsets.UTF_8);

		@Override
		protected void init(final Configuration configuration) {
			algorithm = (configuration.get(ConfigConst.ALGORITHM_NAME) + ALG_PREFIX_DELIMITER).getBytes(Charsets.UTF_8);
		}

		@Override
		protected ValueSet createStreamReceiver() {
			return new ValueSet();
		}

		@Override
		protected void map(final ImmutableBytesWritable row, final ValueSet data, final Result raw,
				final Context context) throws InterruptedException, IOException {
			// currentRow.set(row.get(), row.getOffset(), row.getLength());
			currentRow.set(raw.getRow());

			context.getCounter(JOB_NAME, "unique properties").increment(data.size());

			if (data.isEmpty()) {
				context.getCounter(JOB_NAME, "without match key").increment(1);
				return;
			}

			// get preferred bundle
			final String preferredBundle = Collections.min(data);
			data.remove(preferredBundle);
			final byte[] preferredBundleBytes = preferredBundle.getBytes(Charsets.UTF_8);
			setText(currentKey, algorithm, preferredBundleBytes);
			context.write(currentKey, currentRow);

			context.getCounter(JOB_NAME, COUNTER_EQU).increment(data.size());
			// create equivalence notes
			for (String value : data) {
				setText(currentEquivalent, EQUAL_DEL_BYTES, algorithm, value.getBytes(Charsets.UTF_8));
				context.write(currentKey, currentEquivalent);
			}
		}

		private void setText(final Text text, final byte[]... byteArrays) {
			text.set(Bytes.concat(byteArrays));
		}

		@Override
		protected String getCounterName() {
			return JOB_NAME;
		}
	}

	/**
	 * reduces identifying properties to one equivalence statement
	 */
	static final class UniquePropertiesReducer extends TableReducer<Text, Text, Text> {

		private static final Text EMPTY_TEXT = new Text();

		private final Set<Text> movetoNotices = new HashSet<Text>();
		private final Set<Text> containsIdList = new HashSet<Text>();
		private final ComplexPutWriter putWriter = new ComplexPutWriter();

		@Override
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException,
				InterruptedException {
			// clean up
			movetoNotices.clear();
			containsIdList.clear();

			// fill movetoNotices and ids
			for (Text val : values) {
				if (val.find(EQUAL_DEL) == 0) {
					// Text creation necessary as val is reused by hadoop
					final Text target = new Text();
					target.append(val.getBytes(), EQUAL_DEL_BYTES.length, val.getLength() - EQUAL_DEL_BYTES.length);
					movetoNotices.add(target);
				} else {
					final Text identifier = new Text(CONTAINS_DEL);
					identifier.append(val.getBytes(), 0, val.getLength());
					containsIdList.add(identifier);
				}
			}

			// do we have a bundle?
			if (containsIdList.size() < 2) {
				context.getCounter(JOB_NAME, "singles").increment(1);
			}
			movetoNotices.remove(key);
			writeBundle(key, context);
			context.getCounter(JOB_NAME, "groups").increment(1);
		}

		private void writeBundle(final Text bundleId, final Context context) throws IOException, InterruptedException {

			putWriter.startRecord(bundleId.toString());

			for (Text containsId : containsIdList) {
				putWriter.literal(containsId.getBytes());
			}
			for (Text moved : movetoNotices) {
				putWriter.literal(Bytes.concat(EQUAL_DEL_BYTES, moved.toString().getBytes()));
			}
			putWriter.endRecord();
			context.write(EMPTY_TEXT, putWriter.getCurrentPut());
		}

		private void processValues(final Iterable<Text> values) {
			for (Text val : values) {
				if (val.find(EQUAL_DEL) == 0) {
					// Text creation necessary as val is reused by hadoop
					final Text target = new Text();
					target.append(val.getBytes(), EQUAL_DEL_BYTES.length, val.getLength() - EQUAL_DEL_BYTES.length);
					movetoNotices.add(target);
				} else {
					final Text identifier = new Text(CONTAINS_DEL);
					identifier.append(val.getBytes(), 0, val.getLength());
					containsIdList.add(identifier);
				}
			}
			// test
			// /final Text identifier = new Text("cucu");
			// /containsIdList.add(identifier);
		}
	}
}
