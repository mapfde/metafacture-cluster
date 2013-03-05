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

package org.culturegraph.mf.cluster.job.text;

import java.io.IOException;
import java.io.StringReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.util.reflection.ObjectFactory;

/**
 * Computes term counts for text data in the HDFS. EXPERIMENTAL!
 * 
 * @author Markus Michael Geipel
 */
public final class TokenCounter extends AbstractJobLauncher {

	private static final String ANALYZER = "cg.text.analyzer";

	public static void main(final String[] args) {
		launch(new TokenCounter(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName("Count tokens in '" + getConf().get(ConfigConst.INPUT_PATH) + "'");
		addRequiredArguments(ConfigConst.OUTPUT_PATH, ConfigConst.INPUT_PATH, ANALYZER);
		return getConf();
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configureFileInputMapper(job, conf, AnalyserMapper.class, Text.class, IntWritable.class);
		job.setCombinerClass(IntSumReducer.class);
		configureTextOutputReducer(job, conf, IntSumReducer.class);
	}

	static final class AnalyserMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		private static final String NAME = AnalyserMapper.class.getSimpleName();
		private static final String SEPARATOR = "\t";
		private static final IntWritable ONE = new IntWritable(1);
		private Analyzer analyzer;
		private final Text tokenText = new Text();

		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {
			final Class<? extends Analyzer> clazz = ObjectFactory.loadClass(context.getConfiguration().get(ANALYZER),
					Analyzer.class);
			analyzer = ObjectFactory.newInstance(clazz);
		}

		@Override
		protected void map(final LongWritable key, final Text value, final Context context) throws IOException,
				InterruptedException {

			int cut = 0;
			int nextCut;
			nextCut = value.find(SEPARATOR);
			cut = nextCut;
			nextCut = value.find(SEPARATOR, cut + 1);
			final String type = Text.decode(value.getBytes(), cut, nextCut - cut);
			cut = nextCut;
			final String text = Text.decode(value.getBytes(), cut, value.getLength() - cut);
			final TokenStream stream = analyzer.tokenStream(null, new StringReader(text));

			final CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);

			while (stream.incrementToken()) {
				tokenText.set(term.toString());
				context.write(tokenText, ONE);
				context.getCounter(NAME, "tokens counted").increment(1);
			}
			context.getCounter(NAME, type).increment(1);
		}
	}
}
