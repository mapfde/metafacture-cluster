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

package org.culturegraph.mf.cluster.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.culturegraph.mf.cluster.util.Column;

import com.google.common.base.Charsets;

/**
 * Mock utility for Hodoop and Hbase related objects specific to Culturegraph.
 * 
 * @author Markus Michael Geipel
 *
 */
public final class Mock {

	private Mock() {
		// no instances exist
	}
	
	public static MockResultIntermediate result() {
		return new MockResultIntermediate();
	}
	
	public static MockConfigurationIntermediate configuration() {
		return new MockConfigurationIntermediate();
	}
	
	public static final class MockConfigurationIntermediate {
		private final Configuration configuration = new Configuration();
		
		
		protected MockConfigurationIntermediate() {
			// nothing
		}
		
		public MockConfigurationIntermediate with(final String name, final String value){
			configuration.set(name, value);
			return this;
		}
		
		public Configuration create(){
			return configuration;
		}
	}

	public static final class MockResultIntermediate {
		private static final byte[] EMPTY = "".getBytes(Charsets.UTF_8);
		private final List<KeyValue> keyValues = new ArrayList<KeyValue>();
		private boolean closed;

		protected MockResultIntermediate() {
			// nothing
		}
		
		public MockResultIntermediate with(final byte[] row, final String key, final String value) {
			checkClosed();
			keyValues.add(
					new KeyValue(row, Column.Family.PROPERTY, 
							ComplexPutWriter.buildQualifier(key, value), 0L, EMPTY));
			return this;
		}

		private void checkClosed() {
			if (closed) {
				throw new IllegalStateException("Result already created!");
			}
		}

		public Result create() {
			checkClosed();
			closed = true;
			return new Result(keyValues);
		}
	}
}
