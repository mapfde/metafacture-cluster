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

package org.culturegraph.mf.cluster.job.ingest;

import java.io.IOException;
import java.io.StringReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.culturegraph.mf.cluster.inputformat.XmlInputFormat;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.cluster.util.ConfigConst;
import org.culturegraph.mf.mediawiki.analyzer.MultiAnalyzer;
import org.culturegraph.mf.mediawiki.converter.xml.WikiXmlHandler;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;

/**
 * Reads a Wikipedia dump into an {@link HTable}. Links and PND references are
 * extracted. WORK IN PROGRESS.
 * 
 * @author Markus Michael Geipel
 */
public final class WikipediaIngest extends AbstractJobLauncher {



	private static final String START_TAG = "<page>";
	private static final String END_TAG = "</page>";


	public static void main(final String[] args) {
		final WikipediaIngest wikipediaIngest = new WikipediaIngest();
		wikipediaIngest.addRequiredArguments(ConfigConst.OUTPUT_TABLE, ConfigConst.INPUT_PATH);
		wikipediaIngest.addOptionalArguments(ConfigConst.MORPH_DEF);
		launch(wikipediaIngest, args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		conf.set(XmlInputFormat.START_TAG_KEY, START_TAG);
		conf.set(XmlInputFormat.END_TAG_KEY, END_TAG);
		conf.setIfUnset("mapred.map.tasks.speculative.execution", "false");
		return HBaseConfiguration.create(conf);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		final Path path = new Path(conf.get(ConfigConst.INPUT_PATH));
		FileInputFormat.addInputPath(job, path);
		job.setInputFormatClass(XmlInputFormat.class);
		job.setMapperClass(IngestMapper.class);

		job.setNumReduceTasks(0);
		job.setOutputFormatClass(NullOutputFormat.class);
	}

	/**
	 * writes raw records and properties to htable
	 */
	static final class IngestMapper<K, V> extends Mapper<LongWritable, Text, K, V> {

		private static final String WIKIPEDIA = "Wikipedia";
		private static final String ANALYZER_CONFIG = "mediawiki/analyzer.conf";
		//private MultiAnalyzer wikiAnalyzer;
		//private Metamorph metamorph;
		private final ComplexPutWriter putWriter = new ComplexPutWriter();
		private boolean storeRawData;
		private final XmlDecoder xmlDecoder = new XmlDecoder();
		private HTable htable;



		@Override
		protected void setup(final Context context) throws IOException, InterruptedException {
			super.setup(context);
			
			
			
			final Configuration conf = context.getConfiguration();
			final MultiAnalyzer wikiAnalyzer = new MultiAnalyzer(ANALYZER_CONFIG);
			storeRawData = conf.getBoolean(ConfigConst.STORE_RAW_DATA, false);

			if(conf.get(ConfigConst.MORPH_DEF)==null){
				wikiAnalyzer.setReceiver(putWriter);
			}else{
				wikiAnalyzer.setReceiver(new Metamorph(conf.get(ConfigConst.MORPH_DEF))).setReceiver(putWriter);
			}
			xmlDecoder.setReceiver(new WikiXmlHandler()).setReceiver(wikiAnalyzer);
			
			htable = new HTable(conf, conf.get(ConfigConst.OUTPUT_TABLE));
			htable.setAutoFlush(false);
		}
		
		

		@Override
		protected void cleanup(final Context context) throws IOException, InterruptedException {
			super.cleanup(context);
			htable.flushCommits();
			htable.close();
		}

		@Override
		public void map(final LongWritable row, final Text value, final Context context) throws IOException {
			context.getCounter(WIKIPEDIA, "articles processed").increment(1);
		
			
			xmlDecoder.process(new StringReader(value.toString()));
						
			final Put put = putWriter.getCurrentPut();
			if(put==null){
				context.getCounter(WIKIPEDIA, "empty put").increment(1);
				return;
			}
			
			if (storeRawData) {
				put.add(Column.Family.RAW, Column.Name.WIKI, value.getBytes());
			}


			if (!put.isEmpty()) {
				htable.put(put);
			}
			context.getCounter(WIKIPEDIA, "articles containing text").increment(1);
		}
	}
}
