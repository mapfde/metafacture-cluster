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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.culturegraph.mf.cluster.pipe.HBaseResultDecoder;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.MorphErrorHandler;

/**
 * @author Markus Michael Geipel
 *
 * @param <I> {@link StreamReceiver} to use with {@link Metamorph}
 * @param <K> key class
 * @param <V> value class
 */
public abstract class AbstractTableMorphMapper<I extends StreamReceiver, K, V> extends TableMapper<K, V> implements MorphErrorHandler{
	
	public static final List<String> REQUIREMENTS = Collections.unmodifiableList(Arrays.asList(ConfigConst.MORPH_DEF, ConfigConst.INPUT_TABLE));
	
	//private static final Logger LOG = LoggerFactory.getLogger(AbstractTableMorphMapper.class);

	private Metamorph metamorph;
	private Context currentContext;
	private I receiver;
		
	protected abstract I createStreamReceiver();
	protected abstract String getCounterName();	
	protected abstract void map(final ImmutableBytesWritable row, final I data, final Result raw, final Context context) throws InterruptedException, IOException;

	
	@Override
	protected final void setup(final Context context) throws IOException, InterruptedException {
		receiver = createStreamReceiver();
		if(null==receiver){
			throw new IllegalStateException("failed to create a StreamReceiver");
		}
		metamorph = new Metamorph(context.getConfiguration().get(ConfigConst.MORPH_DEF));
		metamorph.setErrorHandler(this);
		metamorph.setReceiver(receiver);
		init(context.getConfiguration());
	}

	protected void init(final Configuration configuration) {
		// do nothing
	}

	@Override
	public final void map(final ImmutableBytesWritable row, final Result result, final Context context) throws IOException, InterruptedException {
		currentContext = context;
		try{
		HBaseResultDecoder.read(result, metamorph);
		}catch (MetafactureException e) {
			error(e);
		}
		map(row, receiver, result, context);
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
