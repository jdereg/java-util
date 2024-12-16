package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A case-insensitive Map implementation that uses a compact internal representation
 * for small maps.
 *
 * @deprecated As of Cedar Software java-util 2.19.0, replaced by {@link CompactMap#newMap} with case-insensitive configuration.
 * Use {@code CompactMap.newMap(80, false, 16, CompactMap.UNORDERED)} instead of this class.
 * <p>
 * Example replacement:<br>
 * Instead of: {@code Map<String, String> map = new CompactCIHashMap<>();}<br>
 * Use: {@code Map<String, String> map = CompactMap.newMap(80, false, 16, CompactMap.UNORDERED);}
 * </p>
 * <p>
 * This creates a CompactMap with:
 * <ul>
 *   <li>compactSize = 80 (same as CompactCIHashMap)</li>
 *   <li>caseSensitive = false (case-insensitive behavior)</li>
 *   <li>capacity = 16 (default initial capacity)</li>
 *   <li>ordering = UNORDERED (standard hash map behavior)</li>
 * </ul>
 * </p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@Deprecated
public class CompactCIHashMap<K, V> extends CompactMap<K, V>
{
    public CompactCIHashMap() { }
    public CompactCIHashMap(Map<K ,V> other) { super(other); }
    protected Map<K, V> getNewMap() { return new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>(compactSize() + 1)); }
    protected boolean isCaseInsensitive() { return true; }
    protected boolean useCopyIterator() { return false; }
}
