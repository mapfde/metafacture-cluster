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

package org.culturegraph.mf.cluster.sink;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.framework.DefaultStreamReceiver;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;

@Description("writes a stream to hbase. In brackets state TABLE@SERVER.")
@In(StreamReceiver.class)
public final class HBaseIngester extends DefaultStreamReceiver {

	private final ComplexPutWriter writer = new ComplexPutWriter();
	private final HTable hTable;
	
	public HBaseIngester(final String address) {
		super();
		final String[] parts = address.split("@");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Adress '" + address + "' must follow the pattern TABLE@SERVER");
		}
		
		final Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", parts[1]);
		conf.set("hbase.regionserver.lease.period", "180000");
		try {
			hTable = new HTable(conf, parts[0]);
		} catch (IOException e) {
			throw new CulturegraphClusterException("error opening " + address, e);
		}
	}

	@Override
	public void resetStream() {
		//TODO should throw exception
	}

	@Override
	public void closeStream() {
		try {
			hTable.close();
		} catch (IOException e) {
			throw new CulturegraphClusterException("error closing table.", e);
		}
		writer.closeStream();
	}

	@Override
	public void startRecord(final String identifier) {
		writer.startRecord(identifier);
	}

	@Override
	public void startEntity(final String name) {
		writer.startEntity(name);
	}

	@Override
	public void endEntity() {
		writer.endEntity();
	}

	@Override
	public void endRecord() {
		writer.endRecord();
		try {
			hTable.put(writer.getCurrentPut());
		} catch (IOException e) {
			throw new CulturegraphClusterException("error submitting put.", e);
		}
	}

	@Override
	public void literal(final String name, final String value) {
		writer.literal(name, value);
	}
}
