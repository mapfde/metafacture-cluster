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
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;

/**
 * Utility for checking and logging configuration entries.
 * 
 * @author Markus Michael Geipel
 *
 */
public final class ConfigChecker {
	private final List<String> required = new LinkedList<String>();
	private final List<String> optional = new LinkedList<String>();
	
	public ConfigChecker(final String... required) {
		addRequired(required);
	}
	
	public void addRequired(final Collection<String> requiredArguments){
		this.required.addAll(requiredArguments);
	}
	
	public void addRequired(final String... requiredArguments){
		Collections.addAll(this.required, requiredArguments);
	}
	
	public void logConfig(final Logger log, final Configuration conf){
		for (String key : required) {
			final String value = conf.get(key);
			log.info(key + "==" + value);
		}
		for (String key : optional) {
			final String value = conf.get(key);
			log.info(key + "=" + value);
		}
	}
		
	public boolean verifyRequiredConfig(final Logger log, final Configuration conf){
		boolean isOk = true;
		for (String key : required) {
			final String value = conf.get(key);
			if(value==null){
				log.error("Configuration '" + key + "' is not set.");
				isOk=false;
			}
		}
		return isOk;
	}
	
	public boolean logAndVerify(final Logger log, final Configuration conf){
		logConfig(log, conf);
		return verifyRequiredConfig(log,conf);
	}

	public void addOptional(final String... optionalArguments) {
		Collections.addAll(this.optional, optionalArguments);
		
	}
}
