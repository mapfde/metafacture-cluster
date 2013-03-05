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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implements a simple Histogramm based on a {@link HashMap}.
 * 
 * @author Markus Michael Geipel
 *
 * @param <T>
 */
public final class Histogram<T> {
	private final Map<T, Integer> map = new HashMap<T, Integer>();
	
	
	public void increment(final T key){
		if (map.containsKey(key)) {
			map.put(key, Integer.valueOf(map.get(key).intValue() + 1));
		} else {
			map.put(key, Integer.valueOf(1));
		}
	}
	
	public void incrementAll(final Collection<T> keys){
		for (T key : keys) {
			increment(key);
		}
	}
	
	public int get(final T key){
		if(map.containsKey(key)){
			return map.get(key).intValue();
		}
		return 0;
	}
	
	public Set<T> keySet(){
		return Collections.unmodifiableSet(map.keySet());
	}
	
	public Set<Entry<T, Integer>> entrySet(){
		return Collections.unmodifiableSet(map.entrySet());
	}
	
	public void clear(){
		map.clear();
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

}
