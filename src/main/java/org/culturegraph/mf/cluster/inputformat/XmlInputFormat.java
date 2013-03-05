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

package org.culturegraph.mf.cluster.inputformat;

/**
 * This file is a modification of org.apache.mahout.classifier.bayes.XmlInputFormat.
 * Support for compressed xml was added by Markus Michael Geipel.
 * 
 * The original file is licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Reads records that are delimited by a specific begin/end tag.
 */
public final class XmlInputFormat extends TextInputFormat {

	public static final String START_TAG_KEY = "xmlinput.start";
	public static final String END_TAG_KEY = "xmlinput.end";

	private static final Logger LOG = LoggerFactory.getLogger(XmlInputFormat.class);

	@Override
	public RecordReader<LongWritable, Text> createRecordReader(final InputSplit split, final TaskAttemptContext context) {
		try {
			return new XmlRecordReader((FileSplit) split, context.getConfiguration());
		} catch (IOException ioe) {
			LOG.warn("Error while creating XmlRecordReader", ioe);
			return null;
		}
	}

	/**
	 * XMLRecordReader class to read through a given xml document to output xml
	 * blocks as records as specified by the start tag and end tag
	 * 
	 */
	public static final class XmlRecordReader extends RecordReader<LongWritable, Text> {

		private final byte[] startTag;
		private final byte[] endTag;
		private final long start;
		private long end;
		private long pos;

		private InputStream inputStream;

		private final DataOutputBuffer buffer = new DataOutputBuffer();

		private final CompressionCodecFactory compressionCodecs;
		private LongWritable currentKey;
		private Text currentValue;

		public XmlRecordReader(final FileSplit split, final Configuration conf) throws IOException {
			super();
			startTag = conf.get(START_TAG_KEY).getBytes(Charsets.UTF_8);
			endTag = conf.get(END_TAG_KEY).getBytes(Charsets.UTF_8);

			// open the file and seek to the start of the split
			start = split.getStart();
			end = start + split.getLength();
			pos = start;
			final Path file = split.getPath();

			compressionCodecs = new CompressionCodecFactory(conf);
			final CompressionCodec codec = compressionCodecs.getCodec(file);

			final FileSystem fileSystem = file.getFileSystem(conf);
			final FSDataInputStream fsInputStream = fileSystem.open(split.getPath());
			
			if (codec == null) {
				if (start != 0) {
					fsInputStream.seek(start);
				}
				inputStream = fsInputStream;
			} else {
				inputStream = codec.createInputStream(fsInputStream);
				end = Long.MAX_VALUE;
			}
		}

		private boolean next(final LongWritable key, final Text value) throws IOException {
			if (pos < end && readUntilMatch(startTag, false)) {
				try {
					buffer.write(startTag);
					if (readUntilMatch(endTag, true)) {
						key.set(pos);
						value.set(buffer.getData(), 0, buffer.getLength());
						return true;
					}
				} finally {
					buffer.reset();
				}
			}
			return false;
		}

		@Override
		public void close() throws IOException {
			inputStream.close();
		}

		@Override
		public float getProgress() throws IOException {
			if (start == end) {
				return 0.0f;
			}
			return Math.min(1.0f, (pos - start) / (float)(end - start));
		}

		private boolean readUntilMatch(final byte[] match, final boolean withinBlock) throws IOException {
			int counter = 0;
			while (true) {
				final int integer = inputStream.read();
				// end of file:
				if (integer == -1) {
					return false;
				}
				++pos;
				// save to buffer:
				if (withinBlock) {
					buffer.write(integer);
				}

				// check if we're matching:
				if (integer == match[counter]) {
					++counter;
					if (counter >= match.length) {
						return true;
					}
				} else {
					counter = 0;
				}
				// see if we've passed the stop point:
				if (!withinBlock && counter == 0 && pos >= end) {
					return false;
				}
			}
		}

		@Override
		public LongWritable getCurrentKey() throws IOException, InterruptedException {
			return currentKey;
		}

		@Override
		public Text getCurrentValue() throws IOException, InterruptedException {
			return currentValue;
		}

		@Override
		public void initialize(final InputSplit split, final TaskAttemptContext context) throws IOException,
				InterruptedException {
			// nothing to do
		}

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
			currentKey = new LongWritable();
			currentValue = new Text();
			return next(currentKey, currentValue);
		}
	}
}
