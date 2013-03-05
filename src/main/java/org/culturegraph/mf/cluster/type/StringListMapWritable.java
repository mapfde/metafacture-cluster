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

package org.culturegraph.mf.cluster.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.io.Writable;
import org.culturegraph.mf.types.ListMap;

/**
 * Wraps a {@link ListMap} to make it persistent. See {@link Writable}.
 * 
 * @author Markus Michael Geipel
 * 
 */
public final class StringListMapWritable implements Writable {

	private ListMap<String, String> listMap = new ListMap<String, String>();

	public StringListMapWritable() {
		// nothing
	}

	public StringListMapWritable(final ListMap<String, String> listMap1) {
		listMap = listMap1;
	}

	public ListMap<String, String> getListMap() {
		return listMap;
	}
	
	public ListMap<String, String> detachListMap() {
		final ListMap<String, String> returnListMap = listMap;
		listMap = new ListMap<String, String>();
		return returnListMap;
	}

	public void setListMap(final ListMap<String, String> listMap) {
		this.listMap = listMap;
	}

	@Override
	public void readFields(final DataInput dataInput) throws IOException {

		listMap.clearAllKeys();
		listMap.setId(dataInput.readUTF());
		final int numEntries = dataInput.readInt();
		for (int i = 0; i < numEntries; ++i) {
			final int numValues = dataInput.readInt();
			final String key = dataInput.readUTF();
			for (int j = 0; j < numValues; ++j) {
				listMap.add(key, dataInput.readUTF());
			}
		}
	}

	@Override
	public void write(final DataOutput dataOutput) throws IOException {
		dataOutput.writeUTF(listMap.getId());
		final Set<Entry<String, List<String>>> entrySet = listMap.entrySet();
		dataOutput.writeInt(entrySet.size());
		for (final Entry<String, List<String>> entry : entrySet) {
			final List<String> values = entry.getValue();

			dataOutput.writeInt(values.size());
			dataOutput.writeUTF(entry.getKey());
			for (String value : values) {
				dataOutput.writeUTF(value);
			}

		}
	}

	// /*
	// * Only checks reference equality of contained listMap!
	// *
	// * (non-Javadoc)
	// * @see java.lang.Object#equals(java.lang.Object)
	// *
	// */
	// @Override
	// public boolean equals(final Object obj) {
	// if (obj instanceof StringListMapWritable) {
	// final StringListMapWritable writable = (StringListMapWritable) obj;
	// return getListMap().equals(writable.getListMap());
	// }
	// return super.equals(obj);
	// }
	//
	// @Override
	// public int hashCode() {
	// return getListMap().hashCode();
	// }
}
