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

package org.culturegraph.mf.cluster.pipe;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.exceptions.FormatException;
import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.types.CGEntity;

import com.google.common.base.Charsets;

/**
 * Writes the name value pairs in a {@link HTable} {@link Result} to an event
 * stream. See {@link StreamReceiver}/{@link StreamSender}. The name value pairs
 * and entities are expected to be stored in the qualifier.
 * 
 * @see ComplexPutWriter
 * 
 * @author Markus Michael Geipel
 * 
 */
public final class HBaseResultDecoder extends DefaultObjectPipe<Result, StreamReceiver> {
	private static final Pattern FIELD_PATTERN = Pattern.compile(String.valueOf(CGEntity.FIELD_DELIMITER),
			Pattern.LITERAL);
	private static final Pattern SUBFIELD_PATTERN = Pattern.compile(String.valueOf(CGEntity.SUB_DELIMITER),
			Pattern.LITERAL);

	@Override
	public void process(final Result result) {
		read(result, getReceiver());
	}

	public static void read(final Result result, final StreamReceiver receiver) {
		final String identifier = new String(result.getRow(), Charsets.UTF_8);
		receiver.startRecord(identifier);
		read(result.list(), receiver);
		receiver.endRecord();
	}

	public static void read(final List<KeyValue> list, final StreamReceiver receiver) {
		for (KeyValue keyValue : list) {
			read(keyValue.getQualifier(), keyValue.getValue(), receiver);
		}
	}

	public static void read(final byte[] qualifier, final byte[] value, final StreamReceiver receiver) {
		final String string = new String(qualifier, Charsets.UTF_8);

		try {
			if (string.charAt(0) == CGEntity.FIELD_DELIMITER) {

				final String[] fields = FIELD_PATTERN.split(string);

				for (int i = 1; i < fields.length; ++i) {
					final char firstChar = fields[i].charAt(0);
					if (firstChar == CGEntity.LITERAL_MARKER) {
						final String[] parts = SUBFIELD_PATTERN.split(fields[i], -1);
						receiver.literal(parts[0].substring(1), parts[1]);
					} else if (firstChar == CGEntity.ENTITY_START_MARKER) {
						receiver.startEntity(fields[i].substring(1));
					} else if (firstChar == CGEntity.ENTITY_END_MARKER) {
						receiver.endEntity();
					} else {
						throw new FormatException(string);
					}
				}

			} else {
				final int split = string.indexOf(CGEntity.SUB_DELIMITER);
				if(-1 == split){
					receiver.literal(string, new String(value, Charsets.UTF_8));
				}else{
					receiver.literal(string.substring(0, split), string.substring(split + 1));
				}
			}
		} catch (IndexOutOfBoundsException exception) {
			throw new FormatException(string, exception);
		}

	}

	public static void read(final ResultScanner resultScanner, final StreamReceiver streamReceiver) {
		for (Result result : resultScanner) {
			read(result, streamReceiver);
		}
	}

	public void read(final ResultScanner resultScanner) {
		read(resultScanner, getReceiver());
	}

}
