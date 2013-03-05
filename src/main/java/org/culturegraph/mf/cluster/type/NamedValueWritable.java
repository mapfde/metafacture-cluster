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

import org.apache.hadoop.io.Writable;
import org.culturegraph.mf.types.NamedValue;

/**
 * Wraps a {@link NamedValue} to make it persistent. See {@link Writable}.
 * 
 * @author Markus Michael Geipel
 * 
 */
public final class NamedValueWritable implements Writable, Comparable<NamedValueWritable> {

	private static final int MAGIC1 = 23;
	private static final int MAGIC2 = 31;
	private String name;
	private String value;
	private int preCompHashCode;

	public NamedValueWritable(final String name, final String value) {
		if (name == null) {
			throw new IllegalArgumentException("'name' must not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("'value' must not be null");
		}

		this.name = name;
		this.value = value;

		computeHash();
	}

	private void computeHash() {
		int result = MAGIC1;
		result = MAGIC2 * result + value.hashCode();
		result = MAGIC2 * result + name.hashCode();
		preCompHashCode = result;
	}

	public NamedValueWritable() {/* for serialization */
	}

	@Override
	public void readFields(final DataInput input) throws IOException {
		this.name = input.readUTF();
		this.value = input.readUTF();

	}

	public void setNameValue(final String name, final String value) {
		this.name = name;
		this.value = value;
		computeHash();
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	@Override
	public void write(final DataOutput out) throws IOException {
		out.writeUTF(name);
		out.writeUTF(value);
	}

	@Override
	public int hashCode() {
		return preCompHashCode;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof NamedValueWritable) {
			final NamedValueWritable namedValue = (NamedValueWritable) obj;
			return namedValue.preCompHashCode == preCompHashCode && namedValue.name.equals(name)
					&& namedValue.value.equals(value);
		}
		return false;
	}

	@Override
	public int compareTo(final NamedValueWritable namedValue) {
		final int first = name.compareTo(namedValue.name);
		if (first == 0) {
			return value.compareTo(namedValue.value);
		}
		return first;
	}

	@Override
	public String toString() {
		return name + ":" + value;
	}
}
