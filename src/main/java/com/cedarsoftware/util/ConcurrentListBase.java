package com.cedarsoftware.util;

/**
 * {@link ConcurrentList}'s superclass, empty here. Its JDK 21 version, in META-INF/versions/21 of the jar (source in
 * src/main/java21), defines reversed(): JDK 21 gave both List and Deque a reversed(), a class that is both must define
 * its own, and java-util, compiled for JDK 8, cannot name the JDK 21 types.
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
abstract class ConcurrentListBase<E> {
}
