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

package org.culturegraph.mf.cluster.mapreduce;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.cluster.type.StringListMapWritable;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.types.ListMap;

public final class KeyChangeMapper extends Mapper<Text, StringListMapWritable, Text, StringListMapWritable> {
	public static final String DEFAULT_KEY = "key";
	// private final StringListMapWritable writable = new
	// StringListMapWritable();
	private String keyName = DEFAULT_KEY;
	private final List<String> tempList = new ArrayList<String>();

	@Override
	protected void setup(final Context context) throws java.io.IOException, InterruptedException {
		keyName = context.getConfiguration().get(ConfigConst.KEY_NAME, DEFAULT_KEY);
	}

	@Override
	public void map(final Text key, final StringListMapWritable value, final Context context)
			throws java.io.IOException, InterruptedException {
		final ListMap<String, String> listMap = value.getListMap();

		tempList.clear();
		tempList.addAll(listMap.get(keyName));
		listMap.removeKey(keyName);
		
		//context.getCounter("KeyChange", "#keys=" + tempList.size()).increment(1);
		for (String newKey : tempList) {
			context.write(new Text(newKey), value);
		}
	}
}