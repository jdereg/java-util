java-util
=========
<!--[![Build Status](https://travis-ci.org/jdereg/java-util.svg?branch=master)](https://travis-ci.org/jdereg/java-util) -->
[![Maven Central](https://badgen.net/maven/v/maven-central/com.cedarsoftware/java-util)](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware)
[![Javadoc](https://javadoc.io/badge/com.cedarsoftware/java-util.svg)](http://www.javadoc.io/doc/com.cedarsoftware/java-util)

Helpful Java utilities that are thoroughly tested and available on [Maven Central](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware). 
This library has <b>no dependencies</b> on other libraries for runtime.
The`.jar`file is `336K` and works with `JDK 1.8` through `JDK 23`.
The `.jar` file classes are version 52 `(JDK 1.8)`
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
```groovy
implementation 'com.cedarsoftware:java-util:2.18.0'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>2.18.0</version>
</dependency>
```
---

## Included in java-util:

### Sets
- **[CompactSet](/src/main/java/com/cedarsoftware/util/CompactSet.java)** - A memory-efficient `Set` implementation that dynamically adapts its internal storage structure based on size:
  - Starts with minimal memory usage for small sets (0-1 elements)
  - Uses a compact array-based storage for medium-sized sets (2 to N elements, where N is configurable)
  - Automatically transitions to a full Set implementation of your choice (HashSet, TreeSet, etc.) for larger sizes
  - Features:
    - Configurable size thresholds for storage transitions
    - Support for ordered (sorted, reverse, insertion) and unordered sets
    - Optional case-insensitive string element comparisons
    - Custom comparator support
    - Memory optimization for single-element sets
    - Compatible with all standard Set operations
  - Ideal for:
    - Applications with many small sets
    - Sets that start small but may grow
    - Scenarios where memory efficiency is crucial
    - Systems needing dynamic set behavior based on size
- **[CaseInsensitiveSet](/src/main/java/com/cedarsoftware/util/CaseInsensitiveSet.java)** - A `Set` that ignores case sensitivity for `Strings`.
- **[ConcurrentSet](/src/main/java/com/cedarsoftware/util/ConcurrentSet.java)** - A thread-safe `Set` that allows `null` elements.
- **[ConcurrentNavigableSetNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableSetNullSafe.java)** - A thread-safe drop-in replacement for `ConcurrentSkipListSet` that allows `null` values. 

### Maps
- **[CompactMap](/src/main/java/com/cedarsoftware/util/CompactMap.java)** - A memory-efficient `Map` implementation that dynamically adapts its internal storage structure based on size:
  - Starts with minimal memory usage for small maps (0-1 entries)
  - Uses a compact array-based storage for medium-sized maps (2 to N entries, where N is configurable)
  - Automatically transitions to a full Map implementation of your choice (HashMap, TreeMap, etc.) for larger sizes
  - Features:
    - Configurable size thresholds for storage transitions
    - Support for ordered (sorted, reverse, insertion) and unordered maps
    - Optional case-insensitive string key comparisons
    - Custom comparator support
    - Memory optimization for single-entry maps
    - Compatible with all standard Map operations
  - Ideal for:
    - Applications with many small maps
    - Maps that start small but may grow
    - Scenarios where memory efficiency is crucial
    - Systems needing dynamic map behavior based on size
- **[CaseInsensitiveMap](/src/main/java/com/cedarsoftware/util/CaseInsensitiveMap.java)** - Treats `String` keys in a case-insensitive manner.
- **[LRUCache](/src/main/java/com/cedarsoftware/util/LRUCache.java)** - Thread-safe LRU cache which implements the Map API.  Supports "locking" or "threaded" strategy (selectable).
- **[TTLCache](/src/main/java/com/cedarsoftware/util/TTLCache.java)** - Thread-safe TTL cache which implements the Map API. Entries older than Time-To-Live will be evicted.  Also supports a `maxSize` (LRU capability).
- **[TrackingMap](/src/main/java/com/cedarsoftware/util/TrackingMap.java)** - Tracks access patterns to its keys, aiding in performance optimizations.
- **[ConcurrentHashMapNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentHashMapNullSafe.java)** - A thread-safe drop-in replacement for `ConcurrentHashMap` that allows `null` keys & values.
- **[ConcurrentNavigableMapNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableMapNullSafe.java)** - A thread-safe drop-in replacement for `ConcurrentSkipListMap` that allows `null` keys & values.

### Lists
- **[ConcurrentList](/src/main/java/com/cedarsoftware/util/ConcurrentList.java)** - Provides a thread-safe `List` that can be either an independent or a wrapped instance.

### Utilities
- **[ArrayUtilities](/src/main/java/com/cedarsoftware/util/ArrayUtilities.java)** - Provides utilities for working with Java arrays `[]`, enhancing array operations.
- **[ByteUtilities](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)** - Offers routines for converting `byte[]` to hexadecimal character arrays and vice versa, facilitating byte manipulation.
- **[ClassUtilities](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)** - Includes utilities for class-related operations. For example, the method `computeInheritanceDistance(source, destination)` calculates the number of superclass steps between two classes, returning it as an integer. If no inheritance relationship exists, it returns -1. Distances for primitives and their wrappers are considered as 0, indicating no separation.
- **[Converter](/src/main/java/com/cedarsoftware/util/Converter.java)** - Facilitates type conversions, e.g., converting `String` to `BigDecimal`. Supports a wide range of conversions.
- **[DateUtilities](/src/main/java/com/cedarsoftware/util/DateUtilities.java)** - Robustly parses date strings with support for various formats and idioms.
- **[DeepEquals](/src/main/java/com/cedarsoftware/util/DeepEquals.java)** - Deeply compares two object graphs for equivalence, handling cycles and using custom `equals()` methods where available.
- **[IOUtilities](/src/main/java/com/cedarsoftware/util/IOUtilities.java)** - Transfer APIs, close/flush APIs, compress/uncompress APIs.
  - **[FastReader](/src/main/java/com/cedarsoftware/util/FastReader.java)** and **[FastWriter](/src/main/java/com/cedarsoftware/util/FastWriter.java)** - Provide high-performance alternatives to standard IO classes without synchronization.
  - **[FastByteArrayInputStream](/src/main/java/com/cedarsoftware/util/FastByteArrayInputStream.java)** and **[FastByteArrayOutputStream](/src/main/java/com/cedarsoftware/util/FastByteArrayOutputStream.java)** - Non-synchronized versions of standard Java IO byte array streams.
- **[EncryptionUtilities](/src/main/java/com/cedarsoftware/util/EncryptionUtilities.java)** - Simplifies the computation of checksums and encryption using common algorithms.
- **[Executor](/src/main/java/com/cedarsoftware/util/Executor.java)** - Simplifies the execution of operating system commands with methods for output retrieval.
- **[GraphComparator](/src/main/java/com/cedarsoftware/util/GraphComparator.java)** - Compares two object graphs and provides deltas, which can be applied to synchronize the graphs.
- **[MathUtilities](/src/main/java/com/cedarsoftware/util/GraphComparator.java)** - Offers handy mathematical operations and algorithms.
- **[ReflectionUtils](/src/main/java/com/cedarsoftware/util/ReflectionUtils.java)** - Provides efficient and simplified reflection operations.
- **[StringUtilities](/src/main/java/com/cedarsoftware/util/StringUtilities.java)** - Contains helpful methods for common `String` manipulation tasks.
- **[SystemUtilities](/src/main/java/com/cedarsoftware/util/SystemUtilities.java)** - Offers utilities for interacting with the operating system and environment.
- **[Traverser](/src/main/java/com/cedarsoftware/util/Traverser.java)** - Allows generalized actions on all objects within an object graph through a user-defined method.
- **[UniqueIdGenerator](/src/main/java/com/cedarsoftware/util/UniqueIdGenerator.java)** - Generates unique identifiers with embedded timing information, suitable for use in clustered environments.

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
