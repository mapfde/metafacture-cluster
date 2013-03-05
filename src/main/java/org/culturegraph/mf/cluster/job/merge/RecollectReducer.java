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
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.util.Histogram;
import org.culturegraph.mf.cluster.util.TextArrayWritable;

public final class RecollectReducer extends Reducer<Text, TextArrayWritable, Text, TextArrayWritable> {
	private final NavigableSet<Text> memberSet = new TreeSet<Text>();
	private final Histogram<Text> histogram = new Histogram<Text>();

	@Override
	public void reduce(final Text representative, final Iterable<TextArrayWritable> membersIterable,
			final Context context) throws IOException, InterruptedException {
		memberSet.clear();
		histogram.clear();

		Text mark = Union.SEPARATED;
		histogram.increment(representative);
		for (TextArrayWritable members : membersIterable) {
			for (Text member : members.get()) {
				histogram.increment(member);
			}
			members.copyTo(memberSet);

		}
		memberSet.add(representative);
		for (Entry<Text, Integer> entry : histogram.entrySet()) {
			if (entry.getValue().intValue() != 2) {
				mark = Union.OPEN;
				context.getCounter(Union.UNION_FIND, Union.OPEN_CLASSES).increment(1);
				break;
			}
		}
		context.getCounter(Union.UNION_FIND, "classes").increment(1);
		context.write(mark, new TextArrayWritable(memberSet.toArray(new Text[memberSet.size()])));
	}
}