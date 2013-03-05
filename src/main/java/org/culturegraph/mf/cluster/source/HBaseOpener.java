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

package org.culturegraph.mf.cluster.source;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.culturegraph.mf.cluster.pipe.HBaseResultDecoder;
import org.culturegraph.mf.cluster.pipe.HBaseScannerDecoder;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.DefaultSender;
import org.culturegraph.mf.framework.ObjectReceiver;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;

import com.google.common.base.Charsets;

/**
 * Opens an {@link HTable} and reads from column family <code>prop</code>
 * 
 * @author Markus Michael Geipel
 * 
 */

@Description("reads from an HBase table. Use TABLE/scanner to scan and TABLE/row/ROW_ID to access a single row.")
@In(String.class)
@Out(StreamReceiver.class)
public final class HBaseOpener extends DefaultSender<StreamReceiver> implements ObjectReceiver<String> {

	private static final int CACHED_ROWS = 500;
	private static final Pattern SCANNER_PATTERN = Pattern
			.compile("scanner(\\?(startRow=([^&]*))?&?(stopRow=([^&]*))?)?");

	private static final String ROW = "row/";

	private int cachedRows;
	private final Configuration conf = HBaseConfiguration.create();

	public HBaseOpener(final String server) {
		super();
		conf.set("hbase.zookeeper.quorum", server);
		conf.set("hbase.regionserver.lease.period", "640000");
		cachedRows = CACHED_ROWS;
	}

	public void setCachedRows(final int cachedRows) {
		this.cachedRows = cachedRows;
	}

	@Override
	public void process(final String input) {
		final int index = input.indexOf('/');
		final String table = input.substring(0, index);
		final String rest = input.substring(index + 1);
		final Matcher matcher = SCANNER_PATTERN.matcher(rest);

		if (matcher.find()) {
			final String start = matcher.group(3);
			final String end = matcher.group(5);

			processScanner(table, start, end);

		} else if (rest.startsWith(ROW)) {
			processRow(table, rest.substring(ROW.length()));
		} else {
			throw new IllegalArgumentException("input '" + input + "' is not a valid query");
		}

	}

	private void processRow(final String table, final String row) {
		try {
			final HBaseResultDecoder decoder = new HBaseResultDecoder();
			decoder.setReceiver(getReceiver());

			final HTable htable = new HTable(conf, table);
			final Get get = new Get(row.getBytes(Charsets.UTF_8));
			get.addFamily(Column.Family.PROPERTY);

			final Result result = htable.get(get);
			if (!result.isEmpty()) {
				decoder.process(result);
			}

			htable.close();
		} catch (IOException e) {
			throw new MetafactureException("reading row '" + row + "' form table '" + table + "' failed", e);
		}

	}

	private void processScanner(final String table, final String start, final String end) {
		try {

			final HTable htable = new HTable(conf, table);

			final Scan scan = new Scan();
			scan.setCacheBlocks(false);
			scan.setCaching(cachedRows);

			if (start != null) {
				scan.setStartRow(start.getBytes(Charsets.UTF_8));
			}
			if (end != null) {
				scan.setStopRow(end.getBytes(Charsets.UTF_8));
			}

			scan.addFamily(Column.Family.PROPERTY);
			final ResultScanner resultScanner = htable.getScanner(scan);
			final HBaseScannerDecoder hBaseScannerDecoder = new HBaseScannerDecoder();
			hBaseScannerDecoder.setReceiver(new HBaseResultDecoder()).setReceiver(getReceiver());

			hBaseScannerDecoder.process(resultScanner);

			htable.close();
		} catch (IOException e) {
			throw new MetafactureException("scanning table '" + table + "' failed", e);
		}

	}
}
