java-util
=========
<!--[![Build Status](https://travis-ci.org/jdereg/java-util.svg?branch=master)](https://travis-ci.org/jdereg/java-util) -->
[![Maven Central](https://badgen.net/maven/v/maven-central/com.cedarsoftware/java-util)](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware)
[![Javadoc](https://javadoc.io/badge/com.cedarsoftware/java-util.svg)](http://www.javadoc.io/doc/com.cedarsoftware/java-util)

A collection of high-performance Java utilities designed to enhance standard Java functionality. These utilities focus on:
- Memory efficiency and performance optimization
- Thread-safety and concurrent operations
- Enhanced collection implementations
- Simplified common programming tasks
- Deep object graph operations
 
Available on [Maven Central](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware). 
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
# java-util


### Sets
- **[CompactSet](/src/main/java/com/cedarsoftware/util/CompactSet.java)** - Memory-efficient Set that dynamically adapts its storage structure based on size <details><summary>Details</summary>

  A memory-efficient `Set` implementation that dynamically adapts its internal storage structure based on size:

  **Key Features:**
    - Dynamic Storage Transitions:
        - Empty state: Minimal memory footprint
        - Single element: Optimized single-reference storage
        - Small sets (2 to N elements): Efficient array-based storage
        - Large sets: Automatic transition to full Set implementation

  **Example Usage:**
    ```java
    // Basic usage
    CompactSet<String> set = new CompactSet<>();

    // With custom transition size
    CompactSet<String> set = new CompactSet<>(10);

    // Case-insensitive set
    CompactSet<String> caseInsensitive = CompactSet.createCaseInsensitiveSet();
    ```
</details>

- **[CaseInsensitiveSet](/src/main/java/com/cedarsoftware/util/CaseInsensitiveSet.java)** - Set implementation with case-insensitive String handling
- **[ConcurrentSet](/src/main/java/com/cedarsoftware/util/ConcurrentSet.java)** - Thread-safe Set supporting null elements
- **[ConcurrentNavigableSetNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableSetNullSafe.java)** - Thread-safe NavigableSet supporting null elements

### Maps
- **[CompactMap](/src/main/java/com/cedarsoftware/util/CompactMap.java)** - Memory-efficient Map that dynamically adapts its storage structure based on size
- **[CaseInsensitiveMap](/src/main/java/com/cedarsoftware/util/CaseInsensitiveMap.java)** - Map implementation with case-insensitive String keys
- **[LRUCache](/src/main/java/com/cedarsoftware/util/LRUCache.java)** - Thread-safe Least Recently Used cache with configurable eviction strategies
- **[TTLCache](/src/main/java/com/cedarsoftware/util/TTLCache.java)** - Thread-safe Time-To-Live cache with optional size limits
- **[TrackingMap](/src/main/java/com/cedarsoftware/util/TrackingMap.java)** - Map that monitors key access patterns for optimization
- **[ConcurrentHashMapNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentHashMapNullSafe.java)** - Thread-safe HashMap supporting null keys and values
- **[ConcurrentNavigableMapNullSafe](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableMapNullSafe.java)** - Thread-safe NavigableMap supporting null keys and values

### Lists
- **[ConcurrentList](/src/main/java/com/cedarsoftware/util/ConcurrentList.java)** - Thread-safe List implementation with flexible wrapping options

### Utilities
- **[ArrayUtilities](/src/main/java/com/cedarsoftware/util/ArrayUtilities.java)** - Comprehensive array manipulation operations
- **[ByteUtilities](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)** - Byte array and hexadecimal conversion utilities
- **[ClassUtilities](/src/main/java/com/cedarsoftware/util/ClassUtilities.java)** - Class relationship and reflection helper methods
- **[Converter](/src/main/java/com/cedarsoftware/util/Converter.java)** - Robust type conversion system
- **[DateUtilities](/src/main/java/com/cedarsoftware/util/DateUtilities.java)** - Advanced date parsing and manipulation
- **[DeepEquals](/src/main/java/com/cedarsoftware/util/DeepEquals.java)** - Recursive object graph comparison
- **[IOUtilities](/src/main/java/com/cedarsoftware/util/IOUtilities.java)** - Enhanced I/O operations and streaming utilities
- **[EncryptionUtilities](/src/main/java/com/cedarsoftware/util/EncryptionUtilities.java)** - Simplified encryption and checksum operations
- **[Executor](/src/main/java/com/cedarsoftware/util/Executor.java)** - Streamlined system command execution
- **[GraphComparator](/src/main/java/com/cedarsoftware/util/GraphComparator.java)** - Object graph difference detection and synchronization
- **[MathUtilities](/src/main/java/com/cedarsoftware/util/MathUtilities.java)** - Extended mathematical operations
- **[ReflectionUtils](/src/main/java/com/cedarsoftware/util/ReflectionUtils.java)** - Optimized reflection operations
- **[StringUtilities](/src/main/java/com/cedarsoftware/util/StringUtilities.java)** - Extended String manipulation operations
- **[SystemUtilities](/src/main/java/com/cedarsoftware/util/SystemUtilities.java)** - System and environment interaction utilities
- **[Traverser](/src/main/java/com/cedarsoftware/util/Traverser.java)** - Configurable object graph traversal
- **[UniqueIdGenerator](/src/main/java/com/cedarsoftware/util/UniqueIdGenerator.java)** - Distributed-safe unique identifier generation

[View detailed documentation](userguide.md)

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
