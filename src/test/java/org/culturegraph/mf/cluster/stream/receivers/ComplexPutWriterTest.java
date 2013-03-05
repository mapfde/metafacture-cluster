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

package org.culturegraph.mf.cluster.stream.receivers;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.KeyValue;
import org.culturegraph.mf.cluster.sink.ComplexPutWriter;
import org.junit.Ignore;
import org.junit.Test;

/**
 * tests {@link ComplexPutWriter}
 * 
 * @author Markus Michael Geipel
 *
 */
public final class ComplexPutWriterTest {
	
	@Test
	@Ignore
	//TODO write proper test!
	public void testWrite() throws UnsupportedEncodingException{
		final ComplexPutWriter putWriter = new ComplexPutWriter();
		putWriter.startRecord("id");
		putWriter.literal("literal", "a");
		putWriter.startEntity("e");
		putWriter.literal("literal", "b");
		putWriter.literal("literal", "c");
		putWriter.endEntity();
		putWriter.endRecord();
		
		final Map<byte[], List<KeyValue>> map = putWriter.getCurrentPut().getFamilyMap();
		//final StreamWriter consoleWriter = new StreamWriter(new BufferedWriter(new OutputStreamWriter(System.out, "UTF8")));
		//HBaseResultDecoder.read(map.get(Column.Family.PROPERTY), consoleWriter);
		
		//consoleWriter.closeStream();
	}
	
	
}
