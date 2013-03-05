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
 * Configuration keys used in culturegraph
 * 
 * @author Markus Michael Geipel
 * 
 */
public final class ConfigConst {

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	public static final String OUTPUT_PATH = "cg.output.path";
	public static final String INPUT_PATH = "cg.input.path";
	public static final String OUTPUT_TABLE = "cg.output.table";
	public static final String INPUT_TABLE = "cg.input.table";

	public static final String ALGORITHM_NAME = "cg.match.algorithm";

	public static final String INGEST_PREFIX = "cg.ingest.prefix";

	public static final String FORMAT = "cg.format";
	public static final String DO_MORPH = "cg.domorph";
	public static final String STORE_RAW_DATA = "cg.store_rawdata";
	public static final String MORPH_DEF = "cg.morphdef";
	public static final String REDUCE_MORPH_DEF = "cg.reduce.morphdef";
	public static final String PROPERTY_NAME = "cg.propertyname";

	public static final String CACHED_ROWS = "cg.htable.cached_rows";
	public static final String KEY_NAME = "cg.key_name";

	public static final int DEFAULT_CACHED_ROWS = 500;

	private ConfigConst() {/* no instances exist */
	}
}
