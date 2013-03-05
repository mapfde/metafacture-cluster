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

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.util.TextArrayWritable;

public final class ExplodeReducer extends Reducer<Text, TextArrayWritable, Text, TextArrayWritable> {
	private final NavigableSet<Text> memberSet = new TreeSet<Text>();

	@Override
	public void reduce(final Text representative, final Iterable<TextArrayWritable> membersIterable,
			final Context context) throws IOException, InterruptedException {
		memberSet.clear();
		final Counter counter = context.getCounter(Union.UNION_FIND, "merges");
		for (TextArrayWritable members : membersIterable) {
			members.copyTo(memberSet);
			counter.increment(1);
		}
				
		context.write(representative, new TextArrayWritable(memberSet.toArray(new Text[memberSet.size()])));
	}
}