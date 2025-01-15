package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Map implementation that maintains insertion order and uses a compact internal representation
 * for small maps.
 *
 * @deprecated As of Cedar Software java-util 2.19.0, replaced by CompactMap with builder pattern configuration.
 * <p>
 * Example replacement:<br>
 * Instead of: {@code Map<String, String> map = new CompactLinkedMap<>();}<br>
 * Use: {@code Map<String, String> map = CompactMap.<String, Object>builder().insertionOrder().build();}
 * </p>
 * <p>
 * This creates a CompactMap with:
 * <ul>
 *   <li>compactSize = 70</li>
 *   <li>caseSensitive = true (default behavior)</li>
 *   <li>ordering = INSERTION (maintains insertion order)</li>
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
public class CompactLinkedMap<K, V> extends CompactMap<K, V>
{
    public CompactLinkedMap() { }
    public CompactLinkedMap(Map<K ,V> other) { super(other); }
    protected Map<K, V> getNewMap() { return new LinkedHashMap<>(compactSize() + 1); }
    protected boolean useCopyIterator() { return false; }
}
