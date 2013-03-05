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

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public final class TextArrayWritable extends ArrayWritable {
	public TextArrayWritable() {
		super(Text.class);
	}

	public TextArrayWritable(final Text... elements) {
		super(Text.class, elements);
	}

	@Override
	public Text[] get() {
		final Writable[] writables = super.get();
		final Text[] texts = new Text[writables.length];
		for (int i = 0; i < writables.length; ++i) {
			texts[i] = (Text) writables[i];
		}
		return texts;
	}

	public void copyTo(final Collection<Text> collection) {
		final Writable[] writables = super.get();
		for (int i = 0; i < writables.length; ++i) {
			collection.add((Text) writables[i]);
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(get());
	}
}