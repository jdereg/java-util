package com.cedarsoftware.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A case-insensitive Map implementation that uses a compact internal representation
 * for small maps.  This Map exists to simplify JSON serialization. No custom reader nor
 * writer is needed to serialize this map.  It is a drop-in replacement for LinkedHashMap and
 * if you want case-insensitive behavior for String keys and compactness.
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
public class CompactCILinkedMap<K, V> extends CompactMap<K, V>
{
    public CompactCILinkedMap() { }
    public CompactCILinkedMap(Map<K ,V> other) { super(other); }
    protected Map<K, V> getNewMap() { return new CaseInsensitiveMap<>(compactSize() + 1); }
    protected boolean isCaseInsensitive() { return true; }
    protected boolean useCopyIterator() { return false; }
}
