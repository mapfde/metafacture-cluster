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

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.cluster.util.TextArrayWritable;

public final class RecollectMapper extends Mapper<Text, TextArrayWritable, Text, TextArrayWritable> {

	@Override
	public void map(final Text representative, final TextArrayWritable members, final Context context)
			throws IOException, InterruptedException {

		final Text[] memberArray = members.get();
		if (memberArray.length == 1) {
			context.write(memberArray[0], new TextArrayWritable(representative));
		} else {
			context.write(representative, members);
		}
	}
}