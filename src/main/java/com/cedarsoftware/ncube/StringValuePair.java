/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.ncube;

import com.cedarsoftware.util.StringUtilities;

import java.util.Map;

/**
 * A Map.Entry implementation.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class StringValuePair<V> implements Map.Entry<String,V> {

    private String key;
    private V value;

    public StringValuePair(String key, V value) {
        this.key = key;
        this.value = value;
    }

    public boolean equals(Object that) {
        if (that instanceof StringValuePair) {
            return equals((StringValuePair) that);
        }
        return false;
    }

    public boolean equals(StringValuePair that) {
        return equals(that.key);
    }

    public boolean equals(String that) {
        return StringUtilities.equalsIgnoreCase(this.key, that);
    }

    public int hashCode() {
        return hash(key);
    }

    public String toString() {
        return key + ":" + value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        this.value = value;
        return value;
    }

    /**
     * Helper method to handle object hashes for possibly null values
     */
    protected int hash(Object object) {
        return (object == null) ? 0xbabe : object.hashCode();
    }

}
