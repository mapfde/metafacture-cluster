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
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.cluster.util.TextArrayWritable;

public final class ExplodeMapper extends Mapper<Text, TextArrayWritable, Text, TextArrayWritable> {

	private final NavigableSet<Text> memberSet = new TreeSet<Text>();

	@Override
	public void map(final Text tag, final TextArrayWritable members, final Context context) throws IOException,
			InterruptedException {

		if (tag.equals(Union.SEPARATED)) {
			context.getCounter(Union.UNION_FIND, Union.SEPARATED_CLASSES).increment(1);
			return;
		}
		
		context.getCounter(Union.UNION_FIND, Union.OPEN_CLASSES).increment(1);
		
		memberSet.clear();
		members.copyTo(memberSet);
		explode(memberSet, context);
	}

	public static void explode(final NavigableSet<Text> memberSet,
			final Mapper<?, ?, Text, TextArrayWritable>.Context context) throws IOException, InterruptedException {

		final Text representative = memberSet.first();
		context.write(representative, new TextArrayWritable(memberSet.toArray(new Text[memberSet.size()])));
		memberSet.pollFirst();
		for (Text member : memberSet) {
			context.write(member, new TextArrayWritable(representative));
		}
	}
}