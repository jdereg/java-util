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
```groovy
implementation 'com.cedarsoftware:java-util:2.11.0'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>2.11.0</version>
</dependency>
```
---

Included in java-util:
## Included in java-util:
- **[ArrayUtilities](/src/main/java/com/cedarsoftware/util/ArrayUtilities.java)** - Provides utilities for working with Java arrays `[]`, enhancing array operations.
- **[ByteUtilities](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)** - Offers routines for converting `byte[]` to hexadecimal character arrays and vice versa, facilitating byte manipulation.
- **[ClassUtilities](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)** - Includes utilities for class-related operations. For example, the method `computeInheritanceDistance(source, destination)` calculates the number of superclass steps between two classes, returning it as an integer. If no inheritance relationship exists, it returns -1. Distances for primitives and their wrappers are considered as 0, indicating no separation.

### Sets
- **[CompactSet](/src/main/java/com/cedarsoftware/util/CompactSet.java)** - A memory-efficient `Set` that expands to a `HashSet` when `size() > compactSize()`.
- **[CompactLinkedSet](/src/main/java/com/cedarsoftware/util/CompactLinkedSet.java)** - A memory-efficient `Set` that transitions to a `LinkedHashSet` when `size() > compactSize()`.
- **[CompactCILinkedSet](/src/main/java/com/cedarsoftware/util/CompactCILinkedSet.java)** - A compact, case-insensitive `Set` that becomes a `LinkedHashSet` when expanded.
- **[CompactCIHashSet](/src/main/java/com/cedarsoftware/util/CompactCIHashSet.java)** - A small-footprint, case-insensitive `Set` that expands to a `HashSet`.
- **[CaseInsensitiveSet](/src/main/java/com/cedarsoftware/util/CaseInsensitiveSet.java)** - A `Set` that ignores case sensitivity for `Strings`.
- **[ConcurrentSet](/src/main/java/com/cedarsoftware/util/ConcurrentSet.java)** - A thread-safe `Set` not requiring elements to be comparable, unlike `ConcurrentSkipListSet`.
- **[SealableSet](/src/main/java/com/cedarsoftware/util/SealableSet.java)** - Allows toggling between read-only and writable states via a `Supplier<Boolean>`, managing immutability externally.
- **[SealableNavigableSet](/src/main/java/com/cedarsoftware/util/SealableNavigableSet.java)** - Similar to `SealableSet` but for `NavigableSet`, controlling immutability through an external supplier.

### Maps
- **[CompactMap](/src/main/java/com/cedarsoftware/util/CompactMap.java)** - A `Map` with a small memory footprint that scales to a `HashMap` as needed.
- **[CompactLinkedMap](/src/main/java/com/cedarsoftware/util/CompactLinkedMap.java)** - A compact `Map` that extends to a `LinkedHashMap` for larger sizes.
- **[CompactCILinkedMap](/src/main/java/com/cedarsoftware/util/CompactCILinkedMap.java)** - A small-footprint, case-insensitive `Map` that becomes a `LinkedHashMap`.
- **[CompactCIHashMap](/src/main/java/com/cedarsoftware/util/CompactCIHashMap.java)** - A compact, case-insensitive `Map` expanding to a `HashMap`.
- **[CaseInsensitiveMap](/src/main/java/com/cedarsoftware/util/CaseInsensitiveMap.java)** - Treats `String` keys in a case-insensitive manner.
- **[LRUCache](/src/main/java/com/cedarsoftware/util/LRUCache.java)** - A thread-safe LRU cache implementing the full Map API, managing items based on usage.
- **[TrackingMap](/src/main/java/com/cedarsoftware/util/TrackingMap.java)** - Tracks access patterns to its keys, aiding in performance optimizations.
- **[SealableMap](/src/main/java/com/cedarsoftware/util/SealableMap.java)** - Allows toggling between sealed (read-only) and unsealed (writable) states, managed externally.
- **[SealableNavigableMap](/src/main/java/com/cedarsoftware/util/SealableNavigableMap.java)** - Extends `SealableMap` features to `NavigableMap`, managing state externally.

### Lists
- **[ConcurrentList](/src/main/java/com/cedarsoftware/util/ConcurrentList.java)** - Provides a thread-safe `List` that can be either an independent or a wrapped instance.
- **[SealableList](/src/main/java/com/cedarsoftware/util/SealableList.java)** - Enables switching between sealed and unsealed states for a `List`, managed via an external `Supplier<Boolean>`.

### Additional Utilities
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
