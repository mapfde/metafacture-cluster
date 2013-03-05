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

package org.culturegraph.mf.cluster.job.stat;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.culturegraph.mf.cluster.mapreduce.AbstractFileMorphMapper;
import org.culturegraph.mf.cluster.mapreduce.Printer;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.sink.StringListMap;

import com.google.common.base.Charsets;

/**
 * Morphs data in the HDFS. Which pieces of data are counted is defined with a {@link Metamorph} definition.
 * 
 * @author Markus Michael Geipel
 */
public final class FileMorph extends AbstractJobLauncher {

	public static void main(final String[] args){
		launch(new FileMorph(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName("Metamorph '" + getConf().get(ConfigConst.MORPH_DEF) + "' in '" + getConf().get(ConfigConst.INPUT_PATH)
				+ "'");
					
		addRequiredArguments(ConfigConst.OUTPUT_PATH);
		addRequiredArguments(AbstractFileMorphMapper.REQUIREMENTS);
		return getConf();
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configureFileInputMapper(job, conf, MorphMapper.class, Text.class, Text.class);
		configureTextOutputReducer(job, conf, Printer.forText());
	}

	static final class MorphMapper extends AbstractFileMorphMapper<StringListMap, Text, Text>{
		private static final String NAME = MorphMapper.class.getSimpleName();
		
		private final Text currentValue = new Text();
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
		protected void map(final LongWritable row, final StringListMap data, final Text rawInput,
				final Context context) throws IOException, InterruptedException {
			currentRow.set(data.getId());
			for (Entry<String, List<String>> entry : data.entrySet()) {
				final byte[] keyBytes = entry.getKey().getBytes(Charsets.UTF_8);
				for (String value : entry.getValue()) {
					currentValue.set(keyBytes);
					final byte[] valueBytes = value.getBytes(Charsets.UTF_8);
					currentValue.append(valueBytes, 0, valueBytes.length);
					context.write(currentRow, currentValue);
				}
				context.getCounter(NAME, "properties written").increment(entry.getValue().size());
			}
		}
	}
}
