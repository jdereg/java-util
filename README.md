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
The`.jar`file is `485K` and works with `JDK 1.8` through `JDK 24`.
The `.jar` file classes are version 52 `(JDK 1.8)`
## Compatibility

### JPMS (Java Platform Module System)

This library is fully compatible with JPMS, commonly known as Java Modules. It includes a `module-info.class` file that 
specifies module dependencies and exports. 

### OSGi

This library also supports OSGi environments. It comes with pre-configured OSGi metadata in the `MANIFEST.MF` file, ensuring easy integration into any OSGi-based application.

### Using in an OSGi Runtime

The jar already ships with all necessary OSGi headers and a `module-info.class`. No `Import-Package` entries for `java.*` packages are required when consuming the bundle.

To add the bundle to an Eclipse feature or any OSGi runtime simply reference it:

```xml
<plugin id="com.cedarsoftware.java-util" version="3.5.0"/>
```

Both of these features ensure that our library can be seamlessly integrated into modular Java applications, providing robust dependency management and encapsulation.

---
To include in your project:
##### Gradle
```groovy
implementation 'com.cedarsoftware:java-util:3.5.0'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>3.5.0</version>
</dependency>
```
---
# java-util

### Sets
- **[CompactSet](userguide.md#compactset)** - Memory-efficient Set that dynamically adapts its storage structure based on size
- **[CaseInsensitiveSet](userguide.md#caseinsensitiveset)** - Set implementation with case-insensitive String handling
- **[ConcurrentSet](userguide.md#concurrentset)** - Thread-safe Set supporting null elements
- **[ConcurrentNavigableSetNullSafe](userguide.md#concurrentnavigablesetnullsafe)** - Thread-safe NavigableSet supporting null elements
- **[ClassValueSet](userguide.md#classvalueset)** - High-performance Set optimized for fast Class membership testing using JVM-optimized ClassValue

### Maps
- **[CompactMap](userguide.md#compactmap)** - Memory-efficient Map that dynamically adapts its storage structure based on size
- **[CaseInsensitiveMap](userguide.md#caseinsensitivemap)** - Map implementation with case-insensitive String keys
- **[LRUCache](userguide.md#lrucache)** - Thread-safe Least Recently Used cache with configurable eviction strategies
- **[TTLCache](userguide.md#ttlcache)** - Thread-safe Time-To-Live cache with optional size limits
- **[TrackingMap](userguide.md#trackingmap)** - Map that monitors key access patterns for optimization
- **[ConcurrentHashMapNullSafe](userguide.md#concurrenthashmapnullsafe)** - Thread-safe HashMap supporting null keys and values
- **[ConcurrentNavigableMapNullSafe](userguide.md#concurrentnavigablemapnullsafe)** - Thread-safe NavigableMap supporting null keys and values
- **[ClassValueMap](userguide.md#classvaluemap)** - High-performance Map optimized for fast Class key lookups using JVM-optimized ClassValue

### Lists
- **[ConcurrentList](userguide.md#concurrentlist)** - Thread-safe List implementation with flexible wrapping options

### Utilities
- **[ArrayUtilities](userguide.md#arrayutilities)** - Comprehensive array manipulation operations
- **[ByteUtilities](userguide.md#byteutilities)** - Byte array and hexadecimal conversion utilities
- **[ClassUtilities](userguide.md#classutilities)** - Class relationship and reflection helper methods
- **[Converter](userguide.md#converter)** - Robust type conversion system
- **[DateUtilities](userguide.md#dateutilities)** - Advanced date parsing and manipulation
- **[DeepEquals](userguide.md#deepequals)** - Recursive object graph comparison
- **[EncryptionUtilities](userguide.md#encryptionutilities)** - Simplified encryption and checksum operations
- **[Executor](userguide.md#executor)** - Streamlined system command execution
- **[GraphComparator](userguide.md#graphcomparator)** - Object graph difference detection and synchronization
- **[IOUtilities](userguide.md#ioutilities)** - Enhanced I/O operations and streaming utilities
- **[MathUtilities](userguide.md#mathutilities)** - Extended mathematical operations
- **[ReflectionUtils](userguide.md#reflectionutils)** - Optimized reflection operations
- **[StringUtilities](userguide.md#stringutilities)** - Extended String manipulation operations
- **[SystemUtilities](userguide.md#systemutilities)** - System and environment interaction utilities
- **[Traverser](userguide.md#traverser)** - Configurable object graph traversal
- **[TypeUtilities](userguide.md#typeutilities)** - Advanced Java type introspection and generic resolution utilities
- **[UniqueIdGenerator](userguide.md#uniqueidgenerator)** - Distributed-safe unique identifier generation

### Logging

Because `java-util` has no dependencies on other libraries, `java-util` uses the Java built-in `java.util.logging` for all output. See the
[user guide](userguide.md#redirecting-javautillogging) for ways to route
these logs to SLF4J or Log4j&nbsp;2.

[View detailed documentation](userguide.md)

See [changelog.md](/changelog.md) for revision history.

---

By: John DeRegnaucourt and Kenny Partlow
