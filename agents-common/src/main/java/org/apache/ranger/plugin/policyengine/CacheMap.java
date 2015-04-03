/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.plugin.policyengine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ser.StdSerializers;

import java.util.LinkedHashMap;
import java.util.Map;

public class CacheMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(CacheMap.class);


    private static final float RANGER_CACHE_DEFAULT_LOAD_FACTOR = 0.75f;

    protected int initialCapacity;

    public CacheMap(int initialCapacity) {
        super(initialCapacity, CacheMap.RANGER_CACHE_DEFAULT_LOAD_FACTOR, true); // true for access-order

        this.initialCapacity = initialCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        boolean result = size() > initialCapacity;

        if (LOG.isDebugEnabled()) {
            LOG.debug("CacheMap.removeEldestEntry(), result:"+ result);
        }

        return result;
    }
}
