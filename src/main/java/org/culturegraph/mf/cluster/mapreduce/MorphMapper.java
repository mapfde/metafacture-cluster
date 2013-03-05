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

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.sink.StringListMap;
import org.culturegraph.mf.util.StreamConstants;

import com.google.common.base.Charsets;

/**
 * Applies a {@link Metamorph} transformation to the data and interprets a
 * {to:ID}NAME pattern in names as a redirect to ID.
 * 
 * @author Markus Michael Geipel
 * 
 */

public final class MorphMapper extends AbstractTableMorphMapper<StringListMap, Text, NamedValueWritable> {

	public static final String PROPERTIES_WRITTEN = "properties written";
	public static final String LITERAL_REDIRECTS = "literal redirects";
	public static final String RECORD_REDIRECTS = "record redirects";
	public static final String NAME = MorphMapper.class.getSimpleName();

	private static final Pattern REDIRECT_PATTERN = Pattern.compile("^\\{to:(.+)}(.+)$");
	private final NamedValueWritable currentNamedValue = new NamedValueWritable();
	private final Text currentRow = new Text();

	@Override
	protected StringListMap createStreamReceiver() {
		return new StringListMap();
	}

	@Override
	protected String getCounterName() {
		return NAME;
	}

	@Override
	public void map(final ImmutableBytesWritable row, final StringListMap data, final Result raw, final Context context)
			throws InterruptedException, IOException {

		final String newId = data.getFirst(StreamConstants.ID);
		if (newId == null) {
			currentRow.set(row.get());
		} else {
			currentRow.set(newId);
			context.getCounter(NAME, RECORD_REDIRECTS).increment(1);
		}

		for (Entry<String, List<String>> entry : data.entrySet()) {

			final String name = entry.getKey();
			final Matcher matcher = REDIRECT_PATTERN.matcher(name);
			final Text destinationRow;
			final String decodedName;

			if (matcher.find()) {
				destinationRow = new Text(matcher.group(1).getBytes(Charsets.UTF_8));
				decodedName = matcher.group(2);
				context.getCounter(NAME, LITERAL_REDIRECTS).increment(1);
			} else {
				destinationRow = currentRow;
				decodedName = name;
			}
			for (String value : entry.getValue()) {
				currentNamedValue.setNameValue(decodedName, value);
				context.write(destinationRow, currentNamedValue);
			}
			context.getCounter(NAME, PROPERTIES_WRITTEN).increment(entry.getValue().size());
		}
	}

	public static void configureJob(final Job job) {
		job.setMapperClass(MorphMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(NamedValueWritable.class);
	}
}