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

package org.culturegraph.mf.cluster.inputformat;

/**
 * This class is based on org.apache.hadoop.hbase.mapreduce.TableInputFormatBase by the Apache Software Foundation.
 * The original TableInputFormatBase is licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableRecordReader;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public final class MultiTableInputFormat extends InputFormat<ImmutableBytesWritable, Result> implements Configurable {
	public static final String SCAN = "hbase.mapreduce.scan";
	public static final String INPUT_TABLE = "hbase.mapreduce.inputtable";

	private static final Logger LOG = LoggerFactory.getLogger(MultiTableInputFormat.class);

	private final Map<String, TableScanPair> tableScanPairs = new HashMap<String, MultiTableInputFormat.TableScanPair>();

	/** The configuration. */
	private Configuration conf;

	/**
	 * Returns the current configuration.
	 * 
	 * @return The current configuration.
	 * @see org.apache.hadoop.conf.Configurable#getConf()
	 */
	@Override
	public Configuration getConf() {
		return conf;
	}

	/**
	 * Sets the configuration. This is used to set the details for the table to
	 * be scanned.
	 * 
	 * @param configuration
	 *            The configuration to set.
	 * @see org.apache.hadoop.conf.Configurable#setConf(org.apache.hadoop.conf.Configuration)
	 */
	@Override
	public void setConf(final Configuration configuration) {
		this.conf = configuration;

		int tableCount = 0;
		String tableName;
		while ((tableName = conf.get(INPUT_TABLE + tableCount)) != null) {
			LOG.info("adding table '" + tableName + "'");
			final HTable table;

			try {
				table = new HTable(new Configuration(conf), tableName);
			} catch (IOException e) {
				LOG.error(StringUtils.stringifyException(e));
				continue;
			}

			final String scanString = conf.get(SCAN + tableCount);
			final Scan scan;
			if (scanString == null) {
				scan = new Scan();
				scan.setCacheBlocks(false);
			} else {
				try {
					scan = convertStringToScan(scanString);
				} catch (IOException e) {
					LOG.error("An error occurred.", e);
					continue;
				}
			}
			tableScanPairs.put(tableName, new TableScanPair(table, scan));

			++tableCount;
		}
	}

	/**
	 * Builds a TableRecordReader. If no TableRecordReader was provided, uses
	 * the default.
	 * 
	 * @param split
	 *            The split to work with.
	 * @param context
	 *            The current context.
	 * @return The newly created record reader.
	 * @throws IOException
	 *             When creating the reader fails.
	 * @see org.apache.hadoop.mapreduce.InputFormat#createRecordReader(org.apache.hadoop.mapreduce.InputSplit,
	 *      org.apache.hadoop.mapreduce.TaskAttemptContext)
	 */
	@Override
	public RecordReader<ImmutableBytesWritable, Result> createRecordReader(final InputSplit split,
			final TaskAttemptContext context) throws IOException {
		if (tableScanPairs.isEmpty()) {
			throw new IOException("Cannot create a record reader because of a"
					+ " previous error. Please look at the previous logs lines from"
					+ " the task's full log for more details.");
		}
		final TableSplit tSplit = (TableSplit) split;
		final String tableName = new String(tSplit.getTableName(), Charsets.UTF_8);
		final TableScanPair pair = tableScanPairs.get(tableName);
		final TableRecordReader trr = new TableRecordReader();
		final Scan scan = new Scan(pair.getScan());
		scan.setStartRow(tSplit.getStartRow());
		scan.setStopRow(tSplit.getEndRow());
		trr.setScan(scan);

		trr.setHTable(pair.getTable());
		trr.init();
		return trr;
	}

	/**
	 * Calculates the splits that will serve as input for the map tasks. The
	 * number of splits matches the number of regions in a table.
	 * 
	 * @param context
	 *            The current job context.
	 * @return The list of input splits.
	 * @throws IOException
	 *             When creating the list of splits fails.
	 * @see org.apache.hadoop.mapreduce.InputFormat#getSplits(org.apache.hadoop.mapreduce.JobContext)
	 */
	@Override
	public List<InputSplit> getSplits(final JobContext context) throws IOException {
		if (tableScanPairs.isEmpty()) {
			throw new IOException("No table was provided.");
		}

		final List<InputSplit> splits = new ArrayList<InputSplit>();

		for (TableScanPair pair : tableScanPairs.values()) {
			addSplits(pair.getTable(), pair.getScan(), splits);
		}

		return splits;
	}

	private void addSplits(final HTable table, final Scan scan, final List<InputSplit> splits) throws IOException {
		final Pair<byte[][], byte[][]> keys = table.getStartEndKeys();
		if (keys == null || keys.getFirst() == null || keys.getFirst().length == 0) {
			throw new IOException("Expecting at least one region.");
		}

		for (int i = 0; i < keys.getFirst().length; ++i) {
			if (!includeRegionInSplit(keys.getFirst()[i], keys.getSecond()[i])) {
				continue;
			}
			final String regionLocation = table.getRegionLocation(keys.getFirst()[i]).getServerAddress().getHostname();
			final byte[] startRow = scan.getStartRow();
			final byte[] stopRow = scan.getStopRow();

			if (isInRegion(startRow, stopRow, keys.getFirst()[i], keys.getSecond()[i])) {
				final byte[] splitStart = calcSplitStart(startRow, keys.getFirst()[i]);
				final byte[] splitStop = calcSplitStop(stopRow, keys.getSecond()[i]);
				final InputSplit split = new TableSplit(table.getTableName(), splitStart, splitStop, regionLocation);
				// LOG.info("splitting from table '" + new
				// String(table.getTableName()) + "': " + new String(splitStart)
				// + " to " + new String(splitStop));
				splits.add(split);
			}
		}
	}

	private byte[] calcSplitStop(final byte[] stopRow, final byte[] keySecond) {
		if ((stopRow.length == 0 || Bytes.compareTo(keySecond, stopRow) <= 0) && keySecond.length > 0) {
			return keySecond;
		}
		return stopRow;
	}

	private byte[] calcSplitStart(final byte[] startRow, final byte[] keyFirst) {
		if (startRow.length == 0 || Bytes.compareTo(keyFirst, startRow) >= 0) {
			return keyFirst;
		}
		return startRow;
	}

	private boolean isInRegion(final byte[] startRow, final byte[] stopRow, final byte[] keyFirst,
			final byte[] keySecond) {
		final boolean startInRegion = startRow.length == 0 || keySecond.length == 0
				|| Bytes.compareTo(startRow, keySecond) < 0;
		final boolean stopInRegion = stopRow.length == 0 || Bytes.compareTo(stopRow, keyFirst) > 0;

		return startInRegion && stopInRegion;
	}

	/**
	 * 
	 * 
	 * Test if the given region is to be included in the InputSplit while
	 * splitting the regions of a table.
	 * <p>
	 * This optimization is effective when there is a specific reasoning to
	 * exclude an entire region from the M-R job, (and hence, not contributing
	 * to the InputSplit), given the start and end keys of the same. <br>
	 * Useful when we need to remember the last-processed top record and revisit
	 * the [last, current) interval for M-R processing, continuously. In
	 * addition to reducing InputSplits, reduces the load on the region server
	 * as well, due to the ordering of the keys. <br>
	 * <br>
	 * Note: It is possible that <code>endKey.length() == 0 </code> , for the
	 * last (recent) region. <br>
	 * Override this method, if you want to bulk exclude regions altogether from
	 * M-R. By default, no region is excluded( i.e. all regions are included).
	 * 
	 * 
	 * @param startKey
	 *            Start key of the region
	 * @param endKey
	 *            End key of the region
	 * @return true, if this region needs to be included as part of the input
	 *         (default).
	 * 
	 */
	protected boolean includeRegionInSplit(final byte[] startKey, final byte[] endKey) {
		return true;
	}

	/**
	 * Converts the given Base64 string back into a Scan instance.
	 * 
	 * @param base64
	 *            The scan details.
	 * @return The newly created Scan instance.
	 * @throws IOException
	 *             When reading the scan instance fails.
	 */
	private static Scan convertStringToScan(final String base64) throws IOException {
		final ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(base64));
		final DataInputStream dis = new DataInputStream(bis);
		final Scan scan = new Scan();
		scan.readFields(dis);
		return scan;
	}

	/**
	 * Writes the given scan into a Base64 encoded string.
	 * 
	 * @param scan
	 *            The scan to write out.
	 * @return The scan saved in a Base64 encoded string.
	 * @throws IOException
	 *             When writing the scan fails.
	 */
	private static String convertScanToString(final Scan scan) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(out);
		scan.write(dos);
		return Base64.encodeBytes(out.toByteArray());
	}

	@SuppressWarnings("rawtypes")
	public static void initTableMapperJob(final Class<? extends TableMapper> mapper,
			final Class<? extends WritableComparable> outputKeyClass, final Class<? extends Writable> outputValueClass,
			final Job job) {
		job.setInputFormatClass(MultiTableInputFormat.class);
		if (outputValueClass != null) {
			job.setMapOutputValueClass(outputValueClass);
		}
		if (outputKeyClass != null) {
			job.setMapOutputKeyClass(outputKeyClass);
		}
		job.setMapperClass(mapper);
		HBaseConfiguration.addHbaseResources(job.getConfiguration());
	}

	public static void setTablesInJob(final Job job, final String... tables) {
		for (int i = 0; i < tables.length; ++i) {
			job.getConfiguration().set(INPUT_TABLE + i, tables[i]);
		}
	}

	public static void setTablesInJob(final Job job, final String tables, final Scan scan) throws IOException {
		setTablesInJob(job, tables.split("\\s*,\\s*"), scan);
	}

	public static void setTablesInJob(final Job job, final String[] tables, final Scan scan) throws IOException {
		setTablesInJob(job, tables);
		for (int i = 0; i < tables.length; ++i) {
			job.getConfiguration().set(SCAN, convertScanToString(scan));
		}
	}

	public static void setScansInJob(final Job job, final Scan... scans) throws IOException {
		for (int i = 0; i < scans.length; ++i) {
			job.getConfiguration().set(SCAN, convertScanToString(scans[i]));
		}
	}

	private static final class TableScanPair {
		private final HTable table;
		private final Scan scan;

		public TableScanPair(final HTable table, final Scan scan) {
			this.table = table;
			this.scan = scan;
		}

		public HTable getTable() {
			return table;
		}

		public Scan getScan() {
			return scan;
		}
	}

}