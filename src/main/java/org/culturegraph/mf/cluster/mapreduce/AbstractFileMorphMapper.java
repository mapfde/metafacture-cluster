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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.MorphErrorHandler;
import org.culturegraph.mf.stream.reader.MultiFormatReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Markus Michael Geipel
 * 
 * @param <I>
 *            {@link StreamReceiver} to use with {@link Metamorph}
 * @param <K>
 *            key class
 * @param <V>
 *            value class
 */
public abstract class AbstractFileMorphMapper<I extends StreamReceiver, K, V> extends Mapper<LongWritable, Text, K, V>
		implements MorphErrorHandler {

	public static final List<String> REQUIREMENTS = Collections.unmodifiableList(Arrays.asList(ConfigConst.MORPH_DEF,
			ConfigConst.INPUT_PATH, ConfigConst.FORMAT));
	private static final Logger LOG = LoggerFactory.getLogger(AbstractFileMorphMapper.class);


	private Metamorph metamorph;
	private Context currentContext;
	private MultiFormatReader reader;
	private I receiver;

	protected abstract I createStreamReceiver();

	protected abstract String getCounterName();

	protected abstract void map(LongWritable row, I receiver, Text rawInput, Context context) throws IOException,
			InterruptedException;

	@Override
	protected final void setup(final Context context) throws IOException, InterruptedException {
		reader = new MultiFormatReader(context.getConfiguration().get(ConfigConst.FORMAT));
		
		receiver = createStreamReceiver();
		if (null == receiver) {
			throw new IllegalStateException("failed to create a StreamReceiver");
		}
		metamorph = new Metamorph(context.getConfiguration().get(ConfigConst.MORPH_DEF));
		metamorph.setErrorHandler(this);
		reader.setReceiver(metamorph).setReceiver(receiver);
		init();

	}

	protected void init() {
		// do nothing
	}

	@Override
	public final void map(final LongWritable row, final Text text, final Context context) throws IOException,
			InterruptedException {
		currentContext = context;
		try {
			final String record = text.toString();
			if (record.isEmpty()) {
				context.getCounter(getCounterName(), "empty records");
			} else {
				reader.read(record);
				map(row, receiver, text, context);
			}
		} catch (MetafactureException e) {
			LOG.warn("An Exception occured!", e);
			context.getCounter(getCounterName(), e.getClass().getSimpleName());
		}
	}

	@Override
	public final void error(final Exception exception) {
		currentContext.getCounter(getCounterName(), exception.getClass().getSimpleName()).increment(1);
		currentContext.setStatus("Last exception: " + exception.getClass().getSimpleName() + " "
				+ exception.getMessage());
	}

	protected final Metamorph getMetamorph() {
		return metamorph;
	}

}
