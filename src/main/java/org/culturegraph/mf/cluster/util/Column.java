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


/**
 * Keys for the hbase table columns used in culturegraph
 * 
 * @author Markus Michael Geipel
 * 
 */
public final class Column {

	public static final String SEPARATOR_STRING = ":";
	
	public static final class Family {
		//public static final byte[] BACK_REF = "bref".getBytes();
		public static final byte[] RAW = "raw".getBytes();
		public static final byte[] PROPERTY = "prop".getBytes();
		//public static final byte[] IDENTIFIER = "id".getBytes();

		private Family() {/*no instances exist*/
		}
	}

	public static final class Name {
		
		public static final byte[] MAB2 = "mab2".getBytes();
		public static final byte[] PICA = "pica".getBytes();
		public static final byte[] MARC21 = "marc21".getBytes();
		
		public static final byte[] TXT = "txt".getBytes();
		public static final byte[] WIKI = "wiki".getBytes();
		public static final byte[] INH = "inh".getBytes();
		public static final byte[] TITLE = "ttl".getBytes();
		
		private Name(){/*no instances exist*/}
	}

	private Column() {/*no instances exist*/
	}
}
