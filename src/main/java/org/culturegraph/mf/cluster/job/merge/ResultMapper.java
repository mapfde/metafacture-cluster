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
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.cluster.util.TextArrayWritable;

import com.google.common.base.Charsets;

final class ResultMapper<K, V> extends Mapper<Text, TextArrayWritable, K, V> {

	private static final byte[] REDIRECT = "cg:redirect".getBytes(Charsets.UTF_8);

	private static final long WRITE_BUFFER = 1024 * 1024 * 16;

	private final NavigableSet<Text> memberSet = new TreeSet<Text>();
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
	public void map(final Text tag, final TextArrayWritable members, final Context context) throws IOException,
			InterruptedException {

		if (tag.equals(Union.OPEN)) {
			return;
		}
		
		memberSet.clear();
		members.copyTo(memberSet);
		final Text representative = memberSet.pollFirst();
		for (Text member : memberSet) {
			final Put put = new Put(member.getBytes());
			put.add(Column.Family.PROPERTY, REDIRECT, representative.getBytes());
			htable.put(put);
		}
		context.getCounter(Union.UNION_FIND, "redirects written").increment(memberSet.size());
	}
}