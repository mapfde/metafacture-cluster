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

package org.culturegraph.mf.cluster.util;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.culturegraph.mf.cluster.exception.CulturegraphClusterException;
import org.culturegraph.mf.cluster.inputformat.MultiTableInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates boilerplate code used to schedule a single job in hadoop. Naming
 * conventions for the {@link Configuration} are implicitly expected. See
 * contants in {@link ConfigConst}.
 * 
 * @author Markus Michael Geipel
 */
public abstract class AbstractJobLauncher extends Configured implements Tool {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractJobLauncher.class);

	private static final int CACHED_ROWS = 500;

	private final ConfigChecker configChecker = new ConfigChecker();

	private String jobName = getClass().getSimpleName();

	protected static void launch(final Tool tool, final String[] args) {
		try {
			System.exit(ToolRunner.run(tool, args));
		} catch (Exception e) {
			throw new CulturegraphClusterException("Error during job launch", e);
		}
	}

	public static <K extends WritableComparable<?>, V extends Writable> void configurePropertyTableMapper(
			final Job job, final String tableName, final Class<? extends TableMapper<K, V>> mapperClass,
			final Class<K> keyOutClass, final Class<V> valueOutClass) throws IOException {
		final Scan scan = new Scan();
		scan.addFamily(Column.Family.PROPERTY);
		scan.setCaching(CACHED_ROWS);
		scan.setCacheBlocks(false);

		MultiTableInputFormat.initTableMapperJob(mapperClass, keyOutClass, valueOutClass, job);
		MultiTableInputFormat.setTablesInJob(job, tableName, scan);

		// TableMapReduceUtil.initTableMapperJob(tableName, scan, mapperClass,
		// keyOutClass, valueOutClass, job);
	}

	public static <K extends WritableComparable<?>, V extends Writable> void configurePropertyTableMapper(
			final Job job, final Configuration conf, final Class<? extends TableMapper<K, V>> mapperClass,
			final Class<K> keyOutClass, final Class<V> valueOutClass) throws IOException {
		configurePropertyTableMapper(job, conf.get(ConfigConst.INPUT_TABLE), mapperClass, keyOutClass, valueOutClass);
	}

	@SuppressWarnings("rawtypes")
	// Reducer cannot be parameterized as it would become incompatible with
	// hadoop classes (strange indeed)
	protected static void configureTextOutputReducer(final Job job, final Configuration conf,
			final Class<? extends Reducer> reducerClass) throws IOException {
		job.setReducerClass(reducerClass);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path(conf.get(ConfigConst.OUTPUT_PATH)));
	}

	@Override
	public final int run(final String[] arg) {

		try {
			final Configuration conf = prepareConf(getConf());
			final Job job = new Job(conf, getJobName());
			job.setJarByClass(AbstractJobLauncher.class);

			if (configChecker.logAndVerify(LOG, conf)) {
				configureJob(job, conf);
				if (job.waitForCompletion(true)) {
					return 0;
				}
			}

		} catch (IOException exception) {
			throw new CulturegraphClusterException(exception);
		} catch (InterruptedException exception) {
			LOG.warn("Thread interrupted");
			Thread.currentThread().interrupt();
		} catch (ClassNotFoundException exception) {
			throw new CulturegraphClusterException(exception);
		}
		return 1;
	}

	protected final void addRequiredArguments(final String... required) {
		configChecker.addRequired(required);
	}

	protected final void addRequiredArguments(final Collection<String> required) {
		configChecker.addRequired(required);
	}

	protected final void addOptionalArguments(final String... optional) {
		configChecker.addOptional(optional);
	}

	protected final String getJobName() {
		return jobName;
	}

	protected final void setJobName(final String jobName) {
		this.jobName = jobName;
	}

	protected abstract Configuration prepareConf(final Configuration conf);

	protected abstract void configureJob(Job job, Configuration conf) throws IOException;

	public static <K, V> void configureFileInputMapper(final Job job, final Configuration conf,
			final Class<? extends Mapper<?, ?, K, V>> mapperClass, final Class<K> keyClass, final Class<V> valueClass)
			throws IOException {

		final Path path = new Path(conf.get(ConfigConst.INPUT_PATH));
		FileInputFormat.addInputPath(job, path);
		job.setMapperClass(mapperClass);
		job.setMapOutputKeyClass(keyClass);
		job.setMapOutputValueClass(valueClass);

	}

}
