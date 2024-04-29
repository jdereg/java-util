java-util
=========
<!--[![Build Status](https://travis-ci.org/jdereg/java-util.svg?branch=master)](https://travis-ci.org/jdereg/java-util) -->
[![Maven Central](https://badgen.net/maven/v/maven-central/com.cedarsoftware/java-util)](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware)
[![Javadoc](https://javadoc.io/badge/com.cedarsoftware/java-util.svg)](http://www.javadoc.io/doc/com.cedarsoftware/java-util)

Helpful Java utilities that are thoroughly tested and available on [Maven Central](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware). 
This library has <b>no dependencies</b> on other libraries for runtime.
The`.jar`file is `260K` and works with`JDK 1.8`through`JDK 21`.
The '.jar' file classes are version 52 (`JDK 1.8`).
## Compatibility

### JPMS (Java Platform Module System)

This library is fully compatible with JPMS, commonly known as Java Modules. It includes a `module-info.class` file that 
specifies module dependencies and exports. 

### OSGi

This library also supports OSGi environments. It comes with pre-configured OSGi metadata in the `MANIFEST.MF` file, ensuring easy integration into any OSGi-based application. 

Both of these features ensure that our library can be seamlessly integrated into modular Java applications, providing robust dependency management and encapsulation.

---
To include in your project:
##### Gradle
```
implementation 'com.cedarsoftware:java-util:2.9.0'
```

##### Maven
```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>2.9.0</version>
</dependency>
```
---

Included in java-util:
* **ArrayUtilities** - Useful utilities for working with Java's arrays `[]`
* **ByteUtilities** - Useful routines for converting `byte[]` to HEX character `[]` and visa-versa.
* **ClassUtilities** - Useful utilities for Class work. For example, call `computeInheritanceDistance(source, destination)` to get the inheritance distance (number of super class steps to make it from source to destination. It will return the distance as an integer.  If there is no inheritance relationship between the two,
then -1 is returned.  The primitives and primitive wrappers return 0 distance as if they are the
same class.
* **Sets**
  * **CompactSet** - Small memory footprint `Set` that expands to a `HashSet` when `size() > compactSize()`.
  * **CompactLinkedSet** - Small memory footprint `Set` that expands to a `LinkedHashSet` when `size() > compactSize()`.
  * **CompactCILinkedSet** - Small memory footprint `Set` that expands to a case-insensitive `LinkedHashSet` when `size() > compactSize()`.
  * **CompactCIHashSet** - Small memory footprint `Set` that expands to a case-insensitive `HashSet` when `size() > compactSize()`.
  * **CaseInsensitiveSet** - `Set` that ignores case for `Strings` contained within.
  * **ConcurrentSet** - A thread-safe `Set` which does not require each element to be `Comparable` like `ConcurrentSkipListSet` which is a `NavigableSet,` which is a `SortedSet.`
  * **SealableSet** - Provides a `Set` (or `Set` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the `Set` and ensures that all views on the `Set` respect the sealed-ness. One master supplier can control the immutability of many collections.
  * **SealableNavigableSet** - Provides a `NavigableSet` (or `NavigableSet` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the `NavigableSet` and ensures that all views on the `NavigableSet` respect the sealed-ness. One master supplier can control the immutability of many collections.
* **Maps**  
  * **CompactMap** - Small memory footprint `Map` that expands to a `HashMap` when `size() > compactSize()` entries.
  * **CompactLinkedMap** - Small memory footprint `Map` that expands to a `LinkedHashMap` when `size() > compactSize()` entries.
  * **CompactCILinkedMap** - Small memory footprint `Map` that expands to a case-insensitive `LinkedHashMap` when `size() > compactSize()` entries.
  * **CompactCIHashMap** - Small memory footprint `Map` that expands to a case-insensitive `HashMap` when `size() > compactSize()` entries.      
  * **CaseInsensitiveMap** - `Map` that ignores case when `Strings` are used as keys.
  * **LRUCache** - Thread safe LRUCache that implements the full Map API and supports a maximum capacity.  Once max capacity is reached, placing another item in the cache will cause the eviction of the item that was the least recently used (LRU).
  * **TrackingMap** - `Map` class that tracks when the keys are accessed via `.get()` or `.containsKey()`. Provided by @seankellner
  * **SealableMap** - Provides a `Map` (or `Map` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the `Map` and ensures that all views on the `Map` respect the sealed-ness.  One master supplier can control the immutability of many collections.
  * **SealableNavigbableMap** - Provides a `NavigableMap` (or `NavigableMap` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the `NavigableMap` and ensures that all views on the `NavigableMap` respect the sealed-ness.  One master supplier can control the immutability of many collections.
* **Lists**
  * **ConcurrentList** - Provides a thread-safe `List` (or `List` wrapper).  Use the no-arg constructor for a thread-safe `List,` use the constructor that takes a `List` to wrap another `List` instance and make it thread-safe (no elements are copied).
  * **SealableList** - Provides a `List` (or `List` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the `List` and ensures that all views on the `List` respect the sealed-ness.  One master supplier can control the immutability of many collections.

* **Converter** - Convert from one instance to another.  For example, `convert("45.3", BigDecimal.class)` will convert the `String` to a `BigDecimal`.  Works for all primitives, primitive wrappers, `Date`, `java.sql.Date`, `String`, `BigDecimal`, `BigInteger`, `AtomicBoolean`, `AtomicLong`, etc.  The method is very generous on what it allows to be converted.  For example, a `Calendar` instance can be input for a `Date` or `Long`. Call the method `Converter.getSupportedConversions()` or `Converter.allSupportedConversions()` to get a list of all source/target conversions.  Currently, there are 680+ conversions. 
* **DateUtilities** - Robust date String parser that handles date/time, date, time, time/date, string name months or numeric months, skips comma, etc. English month names only (plus common month name abbreviations), time with/without seconds or milliseconds, `y/m/d` and `m/d/y` ordering as well.
* **DeepEquals** - Compare two object graphs and return 'true' if they are equivalent, 'false' otherwise.  This will handle cycles in the graph, and will call an `equals()` method on an object if it has one, otherwise it will do a field-by-field equivalency check for non-transient fields.  Has options to turn on/off using `.equals()` methods that may exist on classes.
* **IO**
  * **FastReader** - Works like `BufferedReader` and `PushbackReader` without the synchronization.  Tracks `line` and `col` by watching for `0x0a,` which can be useful when reading text/json/xml files. You can `.pushback()` a character read, which is very useful in parsers.  
  * **FastWriter** - Works like `BufferedWriter` without the synchronization.  
  * **FastByteArrayInputStream** - Unlike the JDK `ByteArrayInputStream`, `FastByteArrayInputStream` is not `synchronized.`
  * **FastByteArrayOutputStream** - Unlike the JDK `ByteArrayOutputStream`, `FastByteArrayOutputStream` is not `synchronized.`
  * **IOUtilities** - Handy methods for simplifying I/O including such niceties as properly setting up the input stream for HttpUrlConnections based on their specified encoding.  Single line `.close()` method that handles exceptions for you.
* **EncryptionUtilities** - Makes it easy to compute MD5, SHA-1, SHA-256, SHA-512 checksums for `Strings`, `byte[]`, as well as making it easy to AES-128 encrypt `Strings` and `byte[]`'s.
* **Executor** - One line call to execute operating system commands.  `Executor executor = new Executor(); executor.exec('ls -l');`  Call `executor.getOut()` to fetch the output, `executor.getError()` retrieve error output.  If a -1 is returned, there was an error.
* **GraphComparator** - Compare any two Java object graphs. It generates a `List` of `Delta` objects which describe the difference between the two Object graphs.  This Delta list can be played back, such that `List deltas = GraphComparator.delta(source, target); GraphComparator.applyDelta(source, deltas)` will bring source up to match target.  See JUnit test cases for example usage.  This is a completely thorough graph difference (and apply delta), including support for `Array`, `Collection`, `Map`, and object field differences.
* **MathUtilities** - Handy mathematical algorithms to make your code smaller.  For example, minimum of array of values.
* **ReflectionUtils** - Simple one-liners for many common reflection tasks.  Speedy reflection calls due to Method caching.
* **StringUtilities** - Helpful methods that make simple work of common `String` related tasks.
* **SystemUtilities** - A Helpful utility methods for working with external entities like the OS, environment variables, and system properties.
* **Traverser** - Pass any Java object to this Utility class, it will call your passed in anonymous method for each object it encounters while traversing the complete graph.  It handles cycles within the graph. Permits you to perform generalized actions on all objects within an object graph.
* **UniqueIdGenerator** - Generates unique Java long value, that can be deterministically unique across up to 100 servers in a cluster (if configured with an environment variable), the ids are monotonically increasing, and can generate the ids at a rate of about 10 million per second.  Because the current time to the millisecond is embedded in the id, one can back-calculate when the id was generated.

See [changelog.md](/changelog.md) for revision history.

---
### Sponsors
[![Alt text](https://www.yourkit.com/images/yklogo.png "YourKit")](https://www.yourkit.com/.net/profiler/index.jsp)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.

<a href="https://www.jetbrains.com/idea/"><img alt="Intellij IDEA from JetBrains" src="https://s-media-cache-ak0.pinimg.com/236x/bd/f4/90/bdf49052dd79aa1e1fc2270a02ba783c.jpg" data-canonical-src="https://s-media-cache-ak0.pinimg.com/236x/bd/f4/90/bdf49052dd79aa1e1fc2270a02ba783c.jpg" width="100" height="100" /></a>
**Intellij IDEA**<hr>


By: John DeRegnaucourt and Kenny Partlow
