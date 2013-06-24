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

package org.culturegraph.mf.cluster.sink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.culturegraph.mf.cluster.pipe.HBaseResultDecoder;
import org.culturegraph.mf.cluster.util.Column;
import org.culturegraph.mf.framework.DefaultStreamReceiver;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.stream.converter.bib.MissingIdException;
import org.culturegraph.mf.stream.sink.Collector;
import org.culturegraph.mf.types.CGEntity;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

/**
 * Writes an event stream (see {@link StreamReceiver}) to an {@link HTable}
 * {@link Put}. Name value pairs and entities are written to the qualifier.
 * 
 * @see HBaseResultDecoder
 * 
 * @author Markus Michael Geipel
 * 
 */

public final class ComplexPutWriter extends DefaultStreamReceiver implements Collector<Put> {

	private static final byte[] SEPARATOR = Character.toString(CGEntity.SUB_DELIMITER).getBytes(Charsets.UTF_8);
	private static final byte[] NOTHING = new byte[0];
	private static final String ID_NAME = "_id";
	private Put currentPut;
	private final List<byte[]> buffer = new ArrayList<byte[]>();
	private String identifier;
	private Collection<Put> collection;

	private StringBuilder entityBuilder;
	private int entityDepth;
	private String idPrefix = "";

	public ComplexPutWriter(final Collection<Put> collection) {
		super();
		this.collection = collection;
	}

	public ComplexPutWriter() {
		super();
		// nothing to do
	}

	public Put getCurrentPut() {
		return currentPut;
	}

	@Override
	public void startRecord(final String identifier) {
		currentPut = null;
		entityBuilder = null;
		entityDepth = 0;
		this.identifier = null;
		if (identifier != null) {
			this.identifier = identifier;
		}
	}

	@Override
	public void startEntity(final String name) {
		if (entityBuilder == null) {
			entityBuilder = new StringBuilder();
			entityDepth = 0;
			entityBuilder.append(CGEntity.FIELD_DELIMITER);

		}
		entityBuilder.append(CGEntity.ENTITY_START_MARKER);
		entityBuilder.append(name);
		entityBuilder.append(CGEntity.FIELD_DELIMITER);

		++entityDepth;
	}

	@Override
	public void endEntity() {
		--entityDepth;
		entityBuilder.append(CGEntity.ENTITY_END_MARKER);
		entityBuilder.append(CGEntity.FIELD_DELIMITER);
		if (entityDepth == 0) {
			buffer.add(entityBuilder.toString().getBytes(Charsets.UTF_8));
			entityBuilder = null;
		}
	}

	@Override
	public void endRecord() {
		if (identifier == null) {
			throw new MissingIdException("No id found");
		}

		final Put put = new Put( (idPrefix + identifier).getBytes(Charsets.UTF_8));
		for (byte[] qualifier : buffer) {
			put.add(Column.Family.PROPERTY, qualifier, NOTHING);
		}
		buffer.clear();
		if (null != collection) {
			collection.add(put);
		}
		currentPut = put;

	}

	@Override
	public void literal(final String name, final String value) {
		if (name != null && value != null) {
			if (entityBuilder == null) {
				if (name.equals(ID_NAME)) {
					identifier = value;
				} else {
					buffer.add(buildQualifier(name, value));
				}
			} else {
				entityBuilder.append(CGEntity.LITERAL_MARKER);
				entityBuilder.append(name);
				entityBuilder.append(CGEntity.SUB_DELIMITER);
				entityBuilder.append(value);
				entityBuilder.append(CGEntity.FIELD_DELIMITER);
			}
		}
	}

	public static byte[] buildQualifier(final String name, final String value) {

		return Bytes.concat(name.getBytes(Charsets.UTF_8), SEPARATOR, value.getBytes(Charsets.UTF_8));
	}

	public void literal(final byte[] qualifier) {
		buffer.add(qualifier);
	}

	public static boolean putsAreEqual(final Put put1, final Put put2) {
		if (put1.compareTo(put2) != 0) {
			return false;
		}
		return isSubSet(put1, put2) && isSubSet(put2, put1);
	}

	public static boolean isSubSet(final Put subSetPut, final Put put) {
		for (KeyValue keyValue : subSetPut.getFamilyMap().get(Column.Family.PROPERTY)) {
			if (!put.has(Column.Family.PROPERTY, keyValue.getQualifier())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Collection<Put> getCollection() {
		return collection;
	}

	@Override
	public void setCollection(final Collection<Put> collection) {
		this.collection = collection;

	}

	public void reset() {
		buffer.clear();
		currentPut = null;
		if (collection != null) {
			collection.clear();
		}
	}

	public static void write(final Put put, final String name, final String value) {
		put.add(Column.Family.PROPERTY, buildQualifier(name, value), NOTHING);

	}

	public void setIdPrefix(final String string) {
		idPrefix  = string;
		
	}
}
