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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.culturegraph.mf.cluster.mapreduce.AbstractTableMorphMapper;
import org.culturegraph.mf.cluster.mapreduce.HTableReducer;
import org.culturegraph.mf.cluster.mapreduce.MorphMapper;
import org.culturegraph.mf.cluster.mapreduce.Printer;
import org.culturegraph.mf.cluster.type.NamedValueWritable;
import org.culturegraph.mf.cluster.util.AbstractJobLauncher;
import org.culturegraph.mf.cluster.util.ConfigConst;

/**
 * @author Markus Michael Geipel
 */

public final class HTableMorph extends AbstractJobLauncher {

	public static final String NAME = HTableMorph.class.getSimpleName();

	public static void main(final String[] args) {
		launch(new HTableMorph(), args);
	}

	@Override
	protected Configuration prepareConf(final Configuration conf) {
		setJobName(NAME + " '" + getConf().get(ConfigConst.MORPH_DEF) + "' in '"
				+ getConf().get(ConfigConst.INPUT_TABLE) + "'");
		addRequiredArguments(AbstractTableMorphMapper.REQUIREMENTS);
		addOptionalArguments(ConfigConst.OUTPUT_PATH, ConfigConst.OUTPUT_TABLE, ConfigConst.REDUCE_MORPH_DEF);

		final String morphDef = conf.get(ConfigConst.MORPH_DEF);
		final String[] parts = morphDef.split("\\s*,\\s*");
		if (parts.length == 2) {
			conf.set(ConfigConst.MORPH_DEF, parts[0]);
			conf.set(ConfigConst.REDUCE_MORPH_DEF, parts[1]);
		}

		return HBaseConfiguration.create(conf);
	}

	@Override
	protected void configureJob(final Job job, final Configuration conf) throws IOException {
		configurePropertyTableMapper(job, conf, MorphMapper.class, Text.class, NamedValueWritable.class);

		if (conf.get(ConfigConst.OUTPUT_TABLE) == null) {
			configureTextOutputReducer(job, conf, Printer.forNamedValue());
		} else {
			TableMapReduceUtil.initTableReducerJob(conf.get(ConfigConst.OUTPUT_TABLE),
					HTableReducer.ForNamedValue.class, job);
		}
	}
}
