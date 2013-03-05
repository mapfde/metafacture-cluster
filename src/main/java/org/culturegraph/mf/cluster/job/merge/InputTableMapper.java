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

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.Text;
import org.culturegraph.mf.cluster.mapreduce.AbstractTableMorphMapper;
import org.culturegraph.mf.cluster.util.TextArrayWritable;
import org.culturegraph.mf.stream.sink.ValueSet;

public final class InputTableMapper extends
		AbstractTableMorphMapper<ValueSet, Text, TextArrayWritable> {
	private final NavigableSet<Text> memberSet = new TreeSet<Text>();

	@Override
	protected ValueSet createStreamReceiver() {
		return new ValueSet();
	}

	@Override
	protected String getCounterName() {
		return "EquivalenceTableMapper";
	}

	@Override
	protected void map(final ImmutableBytesWritable row, final ValueSet idSet, final Result raw,
			final Context context) throws InterruptedException, IOException {
		if(idSet.size() < 2){
			return;
		}
		memberSet.clear();
		for (String id : idSet) {
			memberSet.add(new Text(id));
		}
		context.getCounter(Union.UNION_FIND, "bundles").increment(1);
		context.getCounter(Union.UNION_FIND, "equ. rels.").increment(memberSet.size()-1);
		ExplodeMapper.explode(memberSet, context);
	}
}