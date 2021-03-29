package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Useful Map that does not care about the case-sensitivity of keys
 * when the key value is a String.  Other key types can be used.
 * String keys will be treated case insensitively, yet key case will
 * be retained.  Non-string keys will work as they normally would.
 * <p>
 * This Map uses very little memory (See CompactMap).  When the Map
 * has more than 'compactSize()' elements in it, the 'delegate' Map
 * is a HashMap.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CompactCIHashMap<K, V> extends CompactMap<K, V>
{
    public CompactCIHashMap() { }
    public CompactCIHashMap(Map<K ,V> other) { super(other); }
    protected Map<K, V> getNewMap() { return new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>(compactSize() + 1)); }
    protected boolean isCaseInsensitive() { return true; }
    protected boolean useCopyIterator() { return false; }
}
