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

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.framework.DefaultObjectReceiver;
import org.culturegraph.mf.stream.converter.CGEntityEncoder;

import com.google.common.base.Charsets;

/**
 * simply prints the key followed by a tab separated list of the reduced values.
 * 
 * @author Markus Michael Geipel
 * 
 */

public final class Printer {

	private Printer() {
		// no instances
	}

	public static Class<ForText> forText() {
		return ForText.class;
	}

	public static Class<ForNamedValue> forNamedValue() {
		return ForNamedValue.class;
	}

	public static Class<NamedValueAsCge> namedValueAsCge() {
		return NamedValueAsCge.class;
	}

	public static final class ForText extends Reducer<Writable, Text, Writable, Text> {
		private static final byte[] SEPARATOR = "\t".getBytes(Charsets.UTF_8);
		private final Text currentValue = new Text();

		@Override
		public void reduce(final Writable key, final Iterable<Text> values, final Context context) throws IOException,
				InterruptedException {
			currentValue.set(SEPARATOR);
			for (Text value : values) {
				currentValue.append(value.getBytes(), 0, value.getLength());
				currentValue.append(SEPARATOR, 0, SEPARATOR.length);
			}
			context.write(key, currentValue);
		}
	}

	public static final class ForNamedValue extends Reducer<Writable, NamedValueWritable, Writable, Text> {
		private final Text currentValue = new Text();
		private final StringBuilder builder = new StringBuilder();

		@Override
		public void reduce(final Writable key, final Iterable<NamedValueWritable> namedValues, final Context context)
				throws IOException, InterruptedException {

			builder.delete(0, builder.length());

			for (NamedValueWritable namedValue : namedValues) {
				builder.append('\t');
				final String value = namedValue.getValue();
				final String name = namedValue.getName();

				if (!name.isEmpty()) {
					builder.append(name);
					builder.append('=');
				}
				builder.append(value);
			}
			currentValue.set(builder.toString());
			context.write(key, currentValue);
		}
	}

	public static final class NamedValueAsCge extends Reducer<Writable, NamedValueWritable, Writable, Text> {
		private static final Writable NULL = NullWritable.get();

		private final CGEntityEncoder encoder = new CGEntityEncoder();
		private final StringReceiver stringReceiver = new StringReceiver();

		public NamedValueAsCge() {
			encoder.setReceiver(stringReceiver);
		}

		@Override
		public void reduce(final Writable key, final Iterable<NamedValueWritable> namedValues, final Context context)
				throws IOException, InterruptedException {

			encoder.startRecord(key.toString());
			for (NamedValueWritable namedValue : namedValues) {
				encoder.literal(namedValue.getName(), namedValue.getValue());
			}
			encoder.endRecord();
			context.write(NULL, new Text(stringReceiver.getString()));
		}

		public static final class StringReceiver extends DefaultObjectReceiver<String> {

			private String string;

			@Override
			public void process(String obj) {
				string = obj;
			}

			public String getString() {
				return string;
			}
		}
	}
}