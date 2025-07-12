java-util
=========

<!-- Badge Section -->
<div align="center">

[![Maven Central](https://badgen.net/maven/v/maven-central/com.cedarsoftware/java-util)](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware)
[![Javadoc](https://javadoc.io/badge/com.cedarsoftware/java-util.svg)](http://www.javadoc.io/doc/com.cedarsoftware/java-util)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![JDK Compatibility](https://img.shields.io/badge/JDK-8%20to%2024-blue.svg)](https://adoptium.net/)

[![Security](https://img.shields.io/badge/Security-70+%20Controls-green.svg)](#feature-options)
[![Dependencies](https://img.shields.io/badge/Dependencies-None-brightgreen.svg)](#zero-dependencies)
[![Thread Safety](https://img.shields.io/badge/Thread%20Safety-Concurrent%20Collections-orange.svg)](#null-safe-concurrency)
[![Memory Efficiency](https://img.shields.io/badge/Memory-Adaptive%20Collections-purple.svg)](#smart-memory-management)

[![GitHub stars](https://img.shields.io/github/stars/jdereg/java-util.svg?style=social&label=Star)](https://github.com/jdereg/java-util)
[![GitHub forks](https://img.shields.io/github/forks/jdereg/java-util.svg?style=social&label=Fork)](https://github.com/jdereg/java-util/fork)

</div>

A collection of high-performance Java utilities designed to enhance standard Java functionality. These utilities focus on:
- Memory efficiency and performance optimization
- Thread-safety and concurrent operations
- Enhanced collection implementations
- Simplified common programming tasks
- Deep object graph operations
 
Available on [Maven Central](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware). 
This library has <b>no dependencies</b> on other libraries for runtime.
The`.jar`file is `~600K` and works with `JDK 1.8` through `JDK 24`.
The `.jar` file classes are version 52 `(JDK 1.8)`

As of version 3.6.0 the library is built with the `-parameters`
compiler flag. Parameter names are now retained for tasks such as
constructor discovery (increased the jar size by about 10K.)

## Quick Start

Experience the power of java-util with these popular utilities that solve common development challenges:

### 🔍 DeepEquals - Compare Complex Object Graphs
Perfect for testing and validation - handles cycles, collections, and deep nesting automatically:

```java
// Compare complex test outputs without worrying about object cycles
List<User> expected = loadExpectedUsers();
List<User> actual = processUsers();

// Standard equals() fails with nested objects and cycles
// DeepEquals handles everything automatically
if (DeepEquals.deepEquals(expected, actual)) {
    System.out.println("Test passed!");
} else {
    // Get detailed diff for debugging
    Map<String, Object> options = new HashMap<>();
    DeepEquals.deepEquals(expected, actual, options);
    System.out.println("Differences: " + options.get("diff"));
}
```

**Visual diff output makes debugging obvious:**

<span style="color: #007acc">

```
// Object field mismatch - pinpoints exactly what's different
[field value mismatch] ▶ Person {name: "Jim Bob", age: 27} ▶ .age
  Expected: 27
  Found: 34

// Collection element differences with precise indexing  
[collection element mismatch] ▶ Container {strings: List(0..2), numbers: List(0..2)} ▶ .strings(0)
  Expected: "a"
  Found: "x"

// Complex nested structures with visual navigation
[array element mismatch] ▶ University {name: "Test University", departmentsByCode: Map(0..1)} ▶ 
  .departmentsByCode 《"CS" ⇨ Department {code: "CS", programs: List(0..2)}》.programs(0).requiredCourses
  Expected length: 2
  Found length: 3

// Map differences show key-value relationships clearly
[map value mismatch] ▶ LinkedHashMap(0..0) ▶ 《"user.email" ⇨ "old@example.com"》
  Expected: "old@example.com"  
  Found: "new@example.com"
```

</span>

### 🔄 Converter - Universal Type Conversion
Convert between any meaningful types with mind-bending intelligence:

```java
// Multi-dimensional arrays ↔ nested collections (any depth, any size!)
String[][][] jagged = {
    {{"a", "b", "c"}, {"d"}},           // First sub-array: 3 elements, then 1 element  
    {{"e", "f"}, {"g", "h", "i", "j"}}, // Second sub-array: 2 elements, then 4 elements
    {{"k"}}                             // Third sub-array: just 1 element
};
List<List<List<String>>> nested = Converter.convert(jagged, List.class);  
// Result: [[[a, b, c], [d]], [[e, f], [g, h, i, j]], [[k]]]

char[][][] backToArray = Converter.convert(nested, char[][][].class);       // Preserves jagged structure perfectly!

// EnumSet magic - detects collections and creates EnumSet automatically  
String[] permissions = {"READ", "WRITE", "ADMIN"};  
EnumSet<Permission> perms = Converter.convert(permissions, Permission.class);  // Array → EnumSet<Permission>

List<String> statusList = Arrays.asList("ACTIVE", "PENDING", "COMPLETE");
EnumSet<Status> statuses = Converter.convert(statusList, Status.class);       // Collection → EnumSet<Status>

Map<String, Object> config = Map.of("DEBUG", true, "INFO", false, "WARN", true);
EnumSet<LogLevel> levels = Converter.convert(config, LogLevel.class);         // Map keySet() → EnumSet<LogLevel>

// UUID as 128-bit number - who even thinks of this?!
UUID uuid = UUID.randomUUID();
BigInteger bigInt = Converter.convert(uuid, BigInteger.class);
// Result: 340282366920938463463374607431768211456 (UUID as massive integer!)
UUID restored = Converter.convert(bigInt, UUID.class);                   // Back to UUID!

// Base64 string directly to ByteBuffer 
String base64Data = "SGVsbG8gV29ybGQ=";  // "Hello World" encoded
ByteBuffer buffer = Converter.convert(base64Data, ByteBuffer.class);
// Result: Ready-to-use ByteBuffer, no manual decoding!

// Map to Color - understands RGB semantics
Map<String, Object> colorMap = Map.of("red", 255, "green", 128, "blue", 0, "alpha", 200);
Color orange = Converter.convert(colorMap, Color.class);
// Result: java.awt.Color[r=255,g=128,b=0,a=200] - it even handles alpha!

// Calendar to atomic types - extracts time AND makes it thread-safe
Calendar cal = Calendar.getInstance(); 
AtomicLong atomicTime = Converter.convert(cal, AtomicLong.class);        // Thread-safe epoch millis
AtomicInteger atomicYear = Converter.convert(cal, AtomicInteger.class);  // Just the year, atomically

// Add your own exotic conversions
Converter converter = new Converter();
converter.addConversion(MyClass.class, String.class, obj -> obj.toJson());

// See ALL available conversions - the full power revealed!
Map<String, Set<String>> supported = Converter.getSupportedConversions();
// Result: {"String" -> ["Integer", "Long", "Date", "UUID", "BigInteger", ...], 
//          "UUID" -> ["String", "BigInteger", "Map", ...], ...}

Set<String> allConversions = Converter.allSupportedConversions();  
// Result: ["String -> Integer", "String -> Long", "UUID -> BigInteger", 
//          "Map -> Color", "Calendar -> AtomicLong", ...]
```

**Beyond these 1000+ direct type conversions, Converter also handles:**
- 📦 **Collection ↔ Collection** (List ↔ Set ↔ Queue, any combination)
- 📦 **Collection ↔ Array** (any collection to any array type, preserving elements)  
- 📦 **Collection → EnumSet** (any collection to EnumSet&lt;targetType&gt; one-dimensional)
- 🧩 **Array ↔ Array** (int[] ↔ String[] ↔ Long[], automatic element conversion)
- 🧩 **Array ↔ Collection** (jagged arrays to nested collections, preserving structure)
- 🧩 **Array → EnumSet** (jagged arrays to nested collections, preserving structure)
- 🗺️ **Map ↔ Map** (HashMap ↔ LinkedHashMap ↔ ConcurrentHashMap, comparator-aware)
- 🗺️ **Map → EnumSet** (keySet() of Map to EnumSet&lt;targetType&gt;)
- 🌟 **N-dimensional support** (jagged arrays, nested collections, any depth)

### 🗝️ CaseInsensitiveMap - Fast, Case-Preserving Maps
High-performance maps that ignore case but preserve original key formatting:

```java
// Default: acts like LinkedHashMap but case-insensitive
CaseInsensitiveMap<String, String> headers = new CaseInsensitiveMap<>();
headers.put("Content-Type", "application/json");
headers.put("User-Agent", "MyApp/1.0");

// Case-insensitive lookup, but keys retain original case
String contentType = headers.get("content-type");  // "application/json"
String userAgent = headers.get("USER-AGENT");      // "MyApp/1.0"

// Walking keySet() returns original case
for (String key : headers.keySet()) {
    System.out.println(key);  // "Content-Type", "User-Agent"
}

// Supports heterogeneous keys (String + non-String)
CaseInsensitiveMap<Object, String> mixed = new CaseInsensitiveMap<>();
mixed.put("StringKey", "value1");
mixed.put(42, "value2");           // Non-string keys work fine

// Use ConcurrentHashMap as backing implementation for thread-safety
Map<String, String> existingData = Map.of("Content-Type", "application/json");
Map<String, String> concurrent = new CaseInsensitiveMap<>(existingData, new ConcurrentHashMap<>());
```

### 🗝️ MultiKeyMap - N-Dimensional Key-Value Mapping
The definitive solution for multi-dimensional lookups - outperforms all alternatives:

```java
// Create a high-performance N-dimensional map
MultiKeyMap<String> productCatalog = new MultiKeyMap<>();

// Standard Map interface - single keys work perfectly
Map<Object, String> mapInterface = productCatalog;
mapInterface.put("electronics", "Electronics Department");
mapInterface.put(Arrays.asList("books", "fiction", "scifi"), "Sci-Fi Books");

// MultiKeyMap varargs API - requires MultiKeyMap variable type
MultiKeyMap<String> catalog = new MultiKeyMap<>();
catalog.put("Electronics Department", "electronics");                    // 1D
catalog.put("Science Fiction Books", "books", "fiction", "scifi");      // 3D  
catalog.put("Laptop Computers", "electronics", "computers", "laptops"); // 3D
catalog.put("Gaming Keyboards", "electronics", "computers", "keyboards", "gaming"); // 4D

// Flexible retrieval using matching dimensions
String dept = catalog.get("electronics");                               // Electronics Department
String category = catalog.get("books", "fiction", "scifi");            // Science Fiction Books
String product = catalog.get("electronics", "computers", "laptops");   // Laptop Computers

// ConcurrentHashMap-level thread safety with lock-free reads
catalog.put("Updated Value", "electronics", "computers", "laptops");   // Enterprise-grade concurrency
// Coming soon: 32-stripe lock striping for 32x write parallelism

// Advanced collection handling with CollectionKeyMode
MultiKeyMap<String> configMap = new MultiKeyMap<>(1024, CollectionKeyMode.COLLECTION_KEY_FIRST);
String[] configPath = {"database", "connection", "pool"};
configMap.put(configPath, "jdbc:mysql://localhost:3306/app");           // Array as single key
String dbUrl = configMap.get(configPath);                               // Retrieved as single key

// Perfect for complex lookups: user permissions, configuration trees, caches
MultiKeyMap<Permission> permissions = new MultiKeyMap<>();
permissions.put(Permission.ADMIN, "user123", "project456", "resource789");
Permission userPerm = permissions.get("user123", "project456", "resource789");
```

**Why MultiKeyMap is the industry-leading solution:**

| Feature | Guava Table | Apache Commons MultiKeyMap | DIY Record+HashMap | **java-util MultiKeyMap**                                 |
|---------|-------------|----------------------------|-------------------|-----------------------------------------------------------|
| **Performance** | ⚠️ Good (map-of-maps overhead) | ❌ Poor (no optimizations) | ❌ Poor (key object creation) | ✅ **Excellent** (Reads: lock-free and zero heap pressure) |
| **Key Dimensions** | ❌ Limited to 2D only | ✅ Unlimited N-D | ✅ Unlimited N-D | ✅ **Unlimited N-D**                                       |
| **Thread Safety** | ❌ None built-in | ❌ Not thread-safe | ❌ None (manual synchronization) | ✅ **Full ConcurrentMap** (nulls allowed)                  |
| **Type Safety** | ✅ Built-in compile-time | ❌ Untyped Object keys | ✅ Built-in compile-time | ✅ **Façade-ready** (flexible core + typed wrapper)        |

### ⏰ TTLCache - Time-Based Caching with LRU
Automatic expiration with optional size limits - supports null keys and values:

```java
// Cache with 30-second TTL
TTLCache<String, User> userCache = new TTLCache<>(Duration.ofSeconds(30));

// Add items - they auto-expire after TTL
userCache.put("user123", loadUser("123"));
userCache.put(null, defaultUser);  // Null keys supported

// Optional: Add LRU eviction with max size
TTLCache<String, String> sessionCache = new TTLCache<>(
    Duration.ofMinutes(15),  // 15-minute TTL
    1000                     // Max 1000 items (LRU eviction)
);

// Use like any Map - items auto-expire
sessionCache.put("session-abc", "user-data");
String userData = sessionCache.get("session-abc");  // null if expired

// Perfect for caching expensive operations
TTLCache<String, Result> resultCache = new TTLCache<>(Duration.ofMinutes(5));
public Result getExpensiveResult(String key) {
    return resultCache.computeIfAbsent(key, k -> performExpensiveOperation(k));
}
```

**Why developers love these utilities:**
- **Zero dependencies** - No classpath conflicts
- **Null-safe** - Handle edge cases gracefully  
- **High performance** - Optimized for real-world usage
- **JDK 8+ compatible** - Works everywhere
- **Production proven** - Used in high-scale applications

## Performance Benchmarks

java-util is engineered for performance-critical applications with optimizations that deliver measurable improvements:


### 📊 Memory Efficiency

**CompactMap Dynamic Adaptation (it has one field):**
- **map.size() == 0** → _Object field_ = null (Sentinel value)
- **map.size() == 1** → _Object field_ = Map.Entry<Key, Value>
- **map.size() == 2 ... compactSize()** → _Object field_ = Object[2*size] containing keys (even) values (odd)
- **map.size() > compactSize()**: → _Object field_ = map // delegates to wrapped map
- Great for applications with millions of small Maps


## How java-util Compares

| Feature | JDK Collections | Google Guava | Eclipse Collections | Apache Commons | **java-util**    |
|---------|----------------|--------------|---------------------|----------------|------------------|
| **Dependencies** | None | 3+ libraries | 2+ libraries | Multiple | None             |
| **Jar Size** | N/A | ~2.7MB | ~2.8MB | ~500KB each | ~500KB total     |
| **JDK Compatibility** | 8+ | 11+ (latest) | 11+ | 8+ | 8+               |
| **Null-Safe Concurrent** | ❌ | ❌ | ❌ | ❌ | ✅ ConcurrentMapNullSafe |
| **Memory-Adaptive Collections** | ❌ | ❌ | ✅ | ❌ | ✅ CompactMap/Set |
| **Case-Preserving Maps** | ❌ | ❌ | ❌ | Limited | ✅ Retains original case |
| **Universal Type Conversion** | ❌ | Limited | ❌ | Limited | ✅ 1000+ conversions |
| **N-Dimensional Mapping** | ❌ | ⚠️ Table (2D only) | ❌ | ⚠️ Limited | ✅ MultiKeyMap (unlimited N-D) |
| **Deep Object Comparison** | ❌ | Limited | ❌ | ❌ | ✅ Handles cycles |
| **Runtime Configuration** | ❌ | ❌ | ❌ | ❌ | ✅ 70+ feature options |
| **TTL Caching** | ❌ | ✅ | ❌ | ❌ | ✅ + LRU combo    |
| **Thread-Safe with Nulls** | ❌ | ❌ | ❌ | ❌ | ✅ All concurrent types |
| **JPMS/OSGi Ready** | ✅ | ⚠️ | ✅ | ⚠️ | ✅ Pre-configured |
| **Security Controls** | ❌ | ❌ | ❌ | ❌ | ✅ Input validation |

### Key Differentiators

**🎯 Zero Dependencies**: Unlike Guava (Checker Framework, Error Prone, J2ObjC) or Eclipse Collections (JUnit, SLF4J), java-util has zero runtime dependencies - no classpath conflicts ever.

**🔒 Null-Safe Concurrency**: java-util is the only library providing thread-safe collections that handle null keys and values safely (`ConcurrentHashMapNullSafe`, `ConcurrentSetNullSafe`).

**🧠 Smart Memory Management**: `CompactMap` and `CompactSet` automatically adapt from array-based storage (small size) to hash-based storage (large size) - optimal memory usage at every scale.

**🔄 Universal Conversion**: Convert between any meaningful Java types - primitives, collections, dates, enums, custom objects. Other libraries require multiple dependencies to achieve the same coverage.

**⚙️ Production Flexibility**: 70+ runtime configuration options allow zero-downtime security hardening and environment-specific tuning that enterprise applications demand.

## 🔒 Enterprise Security Features

java-util provides comprehensive security controls designed for enterprise environments where security compliance and threat mitigation are critical:

### 🛡️ Input Validation & DOS Protection

**Configurable Resource Limits:**
```java
// Prevent memory exhaustion attacks
System.setProperty("deepequals.max.collection.size", "1000000");
System.setProperty("stringutilities.max.repeat.total.size", "10485760");
System.setProperty("mathutilities.max.array.size", "1000000");

// Protect against ReDoS (Regular Expression Denial of Service)
System.setProperty("dateutilities.regex.timeout.enabled", "true");
System.setProperty("dateutilities.regex.timeout.milliseconds", "1000");
```

### 🚫 Dangerous Class Protection

**Block Access to Sensitive System Classes:**
```java
// Prevent reflection-based attacks
System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
// Blocks: Runtime, ProcessBuilder, System, Unsafe, ScriptEngine

// Prevent sensitive field access
System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");  
// Blocks: password, secret, apikey, credential fields
```

### 🔐 Cryptographic Security

**Enforce Strong Crypto Parameters:**
```java
// PBKDF2 iteration requirements
System.setProperty("encryptionutilities.min.pbkdf2.iterations", "100000");
System.setProperty("encryptionutilities.max.pbkdf2.iterations", "1000000");

// Salt and IV size validation
System.setProperty("encryptionutilities.min.salt.size", "16");
System.setProperty("encryptionutilities.min.iv.size", "12");
```

### 🌐 Network Security Controls

**Protocol and Host Validation:**
```java
// Restrict allowed protocols
System.setProperty("io.allowed.protocols", "https");
System.setProperty("urlutilities.allowed.protocols", "https");

// Prevent SSRF (Server-Side Request Forgery)
System.setProperty("urlutilities.allow.internal.hosts", "false");
System.setProperty("urlutilities.max.download.size", "104857600"); // 100MB limit
```

### 🔍 Security Audit & Monitoring

**Comprehensive Logging:**
```java
// Enable detailed security logging
System.setProperty("io.debug", "true");
System.setProperty("io.debug.detailed.urls", "true");
System.setProperty("io.debug.detailed.paths", "true");
```

### 🏢 Zero-Downtime Security Hardening

**Production-Safe Configuration:**
- **Feature flags**: Enable/disable security features without code changes
- **Gradual rollout**: Test security features in staging before production
- **Environment-specific**: Different limits for dev/staging/production
- **Compliance ready**: Meet OWASP, SOC 2, ISO 27001 requirements

**Example: Progressive Security Enablement**
```bash
# Development (permissive)
-Dreflectionutils.security.enabled=false

# Staging (warning mode)  
-Dreflectionutils.security.enabled=true
-Dreflectionutils.dangerous.class.validation.enabled=false

# Production (full security)
-Dreflectionutils.security.enabled=true
-Dreflectionutils.dangerous.class.validation.enabled=true
-Dreflectionutils.sensitive.field.validation.enabled=true
```

### 📋 Security Compliance

| Security Standard | java-util Coverage |
|-------------------|-------------------|
| **OWASP Top 10** | ✅ Injection prevention, DoS protection, Logging |
| **CWE Mitigation** | ✅ CWE-22 (Path traversal), CWE-502 (Unsafe deserialization) |
| **NIST Guidelines** | ✅ Input validation, Crypto parameter enforcement |
| **SOC 2 Type II** | ✅ Audit logging, Access controls, Data protection |

> **Default Secure**: All security features are disabled by default for backward compatibility, but can be enabled system-wide with zero code changes.

## Core Components

| Component | Description                                                                                                                                                                                                                                                                      |
| --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sets** |                                                                                                                                                                                                                                                                                  |
| [CompactSet](userguide.md#compactset) | Memory-efficient Set that dynamically adapts its storage structure based on size.                                                                                                                                                                                                |
| [CaseInsensitiveSet](userguide.md#caseinsensitiveset) | Set implementation with case-insensitive `String` handling.                                                                                                                                                                                                                      |
| [ConcurrentSet](userguide.md#concurrentset) | Thread-safe Set supporting null elements.                                                                                                                                                                                                                                        |
| [ConcurrentNavigableSetNullSafe](userguide.md#concurrentnavigablesetnullsafe) | Thread-safe `NavigableSet` supporting null elements.                                                                                                                                                                                                                             |
| [ClassValueSet](userguide.md#classvalueset) | High-performance Set optimized for fast `Class` membership testing using JVM-optimized `ClassValue`.                                                                                                                                                                             |
| **Maps** |                                                                                                                                                                                                                                                                                  |
| [CompactMap](userguide.md#compactmap) | Memory-efficient Map that dynamically adapts its storage structure based on size.                                                                                                                                                                                                |
| [CaseInsensitiveMap](userguide.md#caseinsensitivemap) | A `Map` wrapper that provides case-insensitive, case-retentive keys and inherits the features of the wrapped map (e.g., thread-safety from `ConcurrentMap` types, multi-key support from `MultiKeyMap`, sorted, thread-safe, allow nulls from `ConcurrentNavigableMapNullSafe`). |
| [LRUCache](userguide.md#lrucache) | Thread-safe Least Recently Used cache with configurable eviction strategies.                                                                                                                                                                                                     |
| [TTLCache](userguide.md#ttlcache) | Thread-safe Time-To-Live cache with optional size limits.                                                                                                                                                                                                                        |
| [TrackingMap](userguide.md#trackingmap) | A `Map` wrapper that tracks key access. Inherits features from wrapped `Map`, including thread-safety (`ConcurrentMap` types), Multiple-key support (`MultiKeyMap`), or sorted, thread-safey, with null support (`ConcurrentNavigableMapNullSafe`)     |
| [ConcurrentHashMapNullSafe](userguide.md#concurrenthashmapnullsafe) | Thread-safe `HashMap` supporting null keys and values.                                                                                                                                                                                                                           |
| [ConcurrentNavigableMapNullSafe](userguide.md#concurrentnavigablemapnullsafe) | Thread-safe `NavigableMap` supporting null keys and values.                                                                                                                                                                                                                      |
| [ClassValueMap](userguide.md#classvaluemap) | High-performance Map optimized for fast `Class` key lookups using JVM-optimized `ClassValue`.                                                                                                                                                                                    |
| [MultiKeyMap](userguide.md#multikeymap) | Concurrent map supporting multiple keys.                                                                                                                                                                                                                                         |
| **Lists** |                                                                                                                                                                                                                                                                                  |
| [ConcurrentList](userguide.md#concurrentlist) | Thread-safe `List` implementation with flexible wrapping options.                                                                                                                                                                                                                |
| **Utilities** |                                                                                                                                                                                                                                                                                  |
| [ArrayUtilities](userguide.md#arrayutilities) | Comprehensive array manipulation operations.                                                                                                                                                                                                                                     |
| [ByteUtilities](userguide.md#byteutilities) | Byte array and hexadecimal conversion utilities.                                                                                                                                                                                                                                 |
| [ClassUtilities](userguide.md#classutilities) | Class relationship and reflection helper methods.                                                                                                                                                                                                                                |
| [Converter](userguide.md#converter) | Robust type conversion system.                                                                                                                                                                                                                                                   |
| [DateUtilities](userguide.md#dateutilities) | Advanced date parsing and manipulation.                                                                                                                                                                                                                                          |
| [DeepEquals](userguide.md#deepequals) | Recursive object graph comparison.                                                                                                                                                                                                                                               |
| [EncryptionUtilities](userguide.md#encryptionutilities) | Simplified encryption and checksum operations.                                                                                                                                                                                                                                   |
| [Executor](userguide.md#executor) | Streamlined system command execution.                                                                                                                                                                                                                                            |
| [GraphComparator](userguide.md#graphcomparator) | Object graph difference detection and synchronization.                                                                                                                                                                                                                           |
| [IOUtilities](userguide.md#ioutilities) | Enhanced I/O operations and streaming utilities.                                                                                                                                                                                                                                 |
| [MathUtilities](userguide.md#mathutilities) | Extended mathematical operations.                                                                                                                                                                                                                                                |
| [ReflectionUtils](userguide.md#reflectionutils) | Optimized reflection operations.                                                                                                                                                                                                                                                 |
| [StringUtilities](userguide.md#stringutilities) | Extended `String` manipulation operations.                                                                                                                                                                                                                                       |
| [SystemUtilities](userguide.md#systemutilities) | System and environment interaction utilities.                                                                                                                                                                                                                                    |
| [Traverser](userguide.md#traverser) | Configurable object graph traversal.                                                                                                                                                                                                                                             |
| [TypeUtilities](userguide.md#typeutilities) | Advanced Java type introspection and generic resolution utilities.                                                                                                                                                                                                               |
| [UniqueIdGenerator](userguide.md#uniqueidgenerator) | Distributed-safe unique identifier generation.                                                                                                                                                                                                                                   |
## Integration and Module Support

### JPMS (Java Platform Module System)

This library is fully compatible with JPMS, commonly known as Java Modules. It includes a `module-info.class` file that 
specifies module dependencies and exports. 

### OSGi

This library also supports OSGi environments. It comes with pre-configured OSGi metadata in the `MANIFEST.MF` file, ensuring easy integration into any OSGi-based application.

### Using in an OSGi Runtime

The jar already ships with all necessary OSGi headers and a `module-info.class`. No `Import-Package` entries for `java.*` packages are required when consuming the bundle.

To add the bundle to an Eclipse feature or any OSGi runtime simply reference it:

```xml
<plugin id="com.cedarsoftware.java-util" version="3.6.0"/>
```

Both of these features ensure that our library can be seamlessly integrated into modular Java applications, providing robust dependency management and encapsulation.

### Maven and Gradle Integration

To include in your project:

##### Gradle
```groovy
implementation 'com.cedarsoftware:java-util:3.6.0'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>3.6.0</version>
</dependency>
```

### 🚀 Framework Integration Examples

For comprehensive framework integration examples including Spring, Jakarta EE, Spring Boot Auto-Configuration, Microservices, Testing, and Performance Monitoring, see **[frameworks.md](frameworks.md)**.

Key integrations include:
- **Spring Framework** - Configuration beans and case-insensitive property handling
- **Jakarta EE/JEE** - CDI producers and validation services  
- **Spring Boot** - Auto-configuration with corrected cache constructors
- **Microservices** - Service discovery and cloud-native configuration
- **Testing** - Enhanced test comparisons with DeepEquals
- **Monitoring** - Micrometer metrics integration

## Feature Options

Modern enterprise applications demand libraries that adapt to diverse security requirements, performance constraints, and operational environments. Following the architectural principles embraced by industry leaders like Google (with their extensive use of feature flags), Netflix (with their chaos engineering configurations), Amazon (with their service-specific tuning), and Meta (with their A/B testing infrastructure), java-util embraces a **flexible feature options approach** that puts control directly in the hands of developers and operations teams.

This approach aligns with current best practices in cloud-native development, including GitOps configurations, service mesh policies, and progressive delivery patterns that define the cutting edge of modern software architecture.

Rather than forcing a one-size-fits-all configuration, java-util provides granular control over every aspect of its behavior through system properties. This approach enables:

- **Zero-downtime security hardening** - Enable security features without code changes
- **Environment-specific tuning** - Different limits for development vs. production
- **Gradual rollout strategies** - Test new security features with feature flags
- **Compliance flexibility** - Meet varying regulatory requirements across deployments
- **Performance optimization** - Fine-tune resource limits based on actual usage patterns

All security features are **disabled by default** to ensure seamless upgrades, with the flexibility to enable and configure them per environment. This design philosophy allows java-util to serve both lightweight applications and enterprise-grade systems from the same codebase.

| Fully Qualified Property Name | Allowed Values | Default Value | Description |
|-------------------------------|----------------|---------------|-------------|
| **ArrayUtilities** | | | |
| `arrayutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all ArrayUtilities security features |
| `arrayutilities.component.type.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Block dangerous system classes in array operations |
| `arrayutilities.max.array.size` | Integer | <span style="color: #007acc">2147483639</span> | Maximum array size (Integer.MAX_VALUE-8) |
| `arrayutilities.dangerous.class.patterns` | Comma-separated patterns | <span style="color: #007acc; font-size: 7pt">java.lang.Runtime,<br>java.lang.ProcessBuilder,<br>java.lang.System,<br>java.security.,javax.script.,<br>sun.,com.sun.,java.lang.Class</span> | Dangerous class patterns to block |
| **ByteUtilities** | | | |
| `byteutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all ByteUtilities security features |
| `byteutilities.max.hex.string.length` | Integer | <span style="color: #007acc">0</span> (disabled) | Hex string length limit for decode operations |
| `byteutilities.max.array.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Byte array size limit for encode operations |
| **DateUtilities** | | | |
| `dateutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all DateUtilities security features |
| `dateutilities.input.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable input length and content validation |
| `dateutilities.regex.timeout.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable regex timeout protection |
| `dateutilities.malformed.string.protection.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable malformed input protection |
| `dateutilities.max.input.length` | Integer | <span style="color: #007acc">1000</span> | Maximum input string length |
| `dateutilities.max.epoch.digits` | Integer | <span style="color: #007acc">19</span> | Maximum digits for epoch milliseconds |
| `dateutilities.regex.timeout.milliseconds` | Long | <span style="color: #007acc">1000</span> | Timeout for regex operations in milliseconds |
| **DeepEquals** | | | |
| `deepequals.secure.errors` | `true`, `false` | <span style="color: #007acc">false</span> | Enable error message sanitization |
| `deepequals.max.collection.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Collection size limit |
| `deepequals.max.array.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Array size limit |
| `deepequals.max.map.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Map size limit |
| `deepequals.max.object.fields` | Integer | <span style="color: #007acc">0</span> (disabled) | Object field count limit |
| `deepequals.max.recursion.depth` | Integer | <span style="color: #007acc">0</span> (disabled) | Recursion depth limit |
| **EncryptionUtilities** | | | |
| `encryptionutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all EncryptionUtilities security features |
| `encryptionutilities.file.size.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable file size limits for hashing operations |
| `encryptionutilities.buffer.size.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable buffer size validation |
| `encryptionutilities.crypto.parameters.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enable cryptographic parameter validation |
| `encryptionutilities.max.file.size` | Long | <span style="color: #007acc">2147483647</span> | Maximum file size for hashing operations (2GB) |
| `encryptionutilities.max.buffer.size` | Integer | <span style="color: #007acc">1048576</span> | Maximum buffer size (1MB) |
| `encryptionutilities.min.pbkdf2.iterations` | Integer | <span style="color: #007acc">10000</span> | Minimum PBKDF2 iterations |
| `encryptionutilities.max.pbkdf2.iterations` | Integer | <span style="color: #007acc">1000000</span> | Maximum PBKDF2 iterations |
| `encryptionutilities.min.salt.size` | Integer | <span style="color: #007acc">8</span> | Minimum salt size in bytes |
| `encryptionutilities.max.salt.size` | Integer | <span style="color: #007acc">64</span> | Maximum salt size in bytes |
| `encryptionutilities.min.iv.size` | Integer | <span style="color: #007acc">8</span> | Minimum IV size in bytes |
| `encryptionutilities.max.iv.size` | Integer | <span style="color: #007acc">32</span> | Maximum IV size in bytes |
| **IOUtilities** | | | |
| `io.debug` | `true`, `false` | <span style="color: #007acc">false</span> | Enable debug logging |
| `io.connect.timeout` | Integer (1000-300000) | <span style="color: #007acc">5000</span> | Connection timeout (1s-5min) |
| `io.read.timeout` | Integer (1000-300000) | <span style="color: #007acc">30000</span> | Read timeout (1s-5min) |
| `io.max.stream.size` | Long | <span style="color: #007acc">2147483647</span> | Stream size limit (2GB) |
| `io.max.decompression.size` | Long | <span style="color: #007acc">2147483647</span> | Decompression size limit (2GB) |
| `io.path.validation.disabled` | `true`, `false` | <span style="color: #007acc">false</span> | Path security validation enabled |
| `io.url.protocol.validation.disabled` | `true`, `false` | <span style="color: #007acc">false</span> | URL protocol validation enabled |
| `io.allowed.protocols` | Comma-separated | <span style="color: #007acc">http,https,file,jar</span> | Allowed URL protocols |
| `io.file.protocol.validation.disabled` | `true`, `false` | <span style="color: #007acc">false</span> | File protocol validation enabled |
| `io.debug.detailed.urls` | `true`, `false` | <span style="color: #007acc">false</span> | Detailed URL logging disabled |
| `io.debug.detailed.paths` | `true`, `false` | <span style="color: #007acc">false</span> | Detailed path logging disabled |
| **MathUtilities** | | | |
| `mathutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all MathUtilities security features |
| `mathutilities.max.array.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Array size limit for min/max operations |
| `mathutilities.max.string.length` | Integer | <span style="color: #007acc">0</span> (disabled) | String length limit for parsing |
| `mathutilities.max.permutation.size` | Integer | <span style="color: #007acc">0</span> (disabled) | List size limit for permutations |
| **ReflectionUtils** | | | |
| `reflectionutils.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all ReflectionUtils security features |
| `reflectionutils.dangerous.class.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Block dangerous class access |
| `reflectionutils.sensitive.field.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Block sensitive field access |
| `reflectionutils.max.cache.size` | Integer | <span style="color: #007acc">50000</span> | Maximum cache size per cache type |
| `reflectionutils.dangerous.class.patterns` | Comma-separated patterns | <span style="color: #007acc; font-size: 7pt">java.lang.Runtime,java.lang.Process,<br>java.lang.ProcessBuilder,sun.misc.Unsafe,<br>jdk.internal.misc.Unsafe,<br>javax.script.ScriptEngine,<br>javax.script.ScriptEngineManager</span> | Dangerous class patterns |
| `reflectionutils.sensitive.field.patterns` | Comma-separated patterns | <span style="color: #007acc; font-size: 7pt">password,passwd,secret,secretkey,<br>apikey,api_key,authtoken,accesstoken,<br>credential,confidential,adminkey,private</span> | Sensitive field patterns |
| `reflection.utils.cache.size` | Integer | <span style="color: #007acc">1500</span> | Reflection cache size |
| **StringUtilities** | | | |
| `stringutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all StringUtilities security features |
| `stringutilities.max.hex.decode.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Max hex string size for decode() |
| `stringutilities.max.wildcard.length` | Integer | <span style="color: #007acc">0</span> (disabled) | Max wildcard pattern length |
| `stringutilities.max.wildcard.count` | Integer | <span style="color: #007acc">0</span> (disabled) | Max wildcard characters in pattern |
| `stringutilities.max.levenshtein.string.length` | Integer | <span style="color: #007acc">0</span> (disabled) | Max string length for Levenshtein distance |
| `stringutilities.max.damerau.levenshtein.string.length` | Integer | <span style="color: #007acc">0</span> (disabled) | Max string length for Damerau-Levenshtein |
| `stringutilities.max.repeat.count` | Integer | <span style="color: #007acc">0</span> (disabled) | Max repeat count for repeat() method |
| `stringutilities.max.repeat.total.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Max total size for repeat() result |
| **SystemUtilities** | | | |
| `systemutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all SystemUtilities security features |
| `systemutilities.environment.variable.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Block sensitive environment variable access |
| `systemutilities.file.system.validation.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Validate file system operations |
| `systemutilities.resource.limits.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Enforce resource usage limits |
| `systemutilities.max.shutdown.hooks` | Integer | <span style="color: #007acc">100</span> | Maximum number of shutdown hooks |
| `systemutilities.max.temp.prefix.length` | Integer | <span style="color: #007acc">100</span> | Maximum temporary directory prefix length |
| `systemutilities.sensitive.variable.patterns` | Comma-separated patterns | <span style="color: #007acc; font-size: 7pt">PASSWORD,PASSWD,PASS,SECRET,KEY,<br>TOKEN,CREDENTIAL,AUTH,APIKEY,API_KEY,<br>PRIVATE,CERT,CERTIFICATE,DATABASE_URL,<br>DB_URL,CONNECTION_STRING,DSN,<br>AWS_SECRET,AZURE_CLIENT_SECRET,<br>GCP_SERVICE_ACCOUNT</span> | Sensitive variable patterns |
| **Traverser** | | | |
| `traverser.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all Traverser security features |
| `traverser.max.stack.depth` | Integer | <span style="color: #007acc">0</span> (disabled) | Maximum stack depth |
| `traverser.max.objects.visited` | Integer | <span style="color: #007acc">0</span> (disabled) | Maximum objects visited |
| `traverser.max.collection.size` | Integer | <span style="color: #007acc">0</span> (disabled) | Maximum collection size to process |
| `traverser.max.array.length` | Integer | <span style="color: #007acc">0</span> (disabled) | Maximum array length to process |
| **UrlUtilities** | | | |
| `urlutilities.security.enabled` | `true`, `false` | <span style="color: #007acc">false</span> | Master switch for all UrlUtilities security features |
| `urlutilities.max.download.size` | Long | <span style="color: #007acc">0</span> (disabled) | Max download size in bytes |
| `urlutilities.max.content.length` | Long | <span style="color: #007acc">0</span> (disabled) | Max Content-Length header value |
| `urlutilities.allow.internal.hosts` | `true`, `false` | <span style="color: #007acc">true</span> | Allow access to internal/local hosts |
| `urlutilities.allowed.protocols` | Comma-separated | <span style="color: #007acc">http,https,ftp</span> | Allowed protocols |
| `urlutilities.strict.cookie.domain` | `true`, `false` | <span style="color: #007acc">false</span> | Enable strict cookie domain validation |
| **Converter** | | | |
| `converter.modern.time.long.precision` | `millis`, `nanos` | <span style="color: #007acc">millis</span> | Precision for Instant, ZonedDateTime, OffsetDateTime conversions |
| `converter.duration.long.precision` | `millis`, `nanos` | <span style="color: #007acc">millis</span> | Precision for Duration conversions |
| `converter.localtime.long.precision` | `millis`, `nanos` | <span style="color: #007acc">millis</span> | Precision for LocalTime conversions |
| **Other** | | | |
| `java.util.force.jre` | `true`, `false` | <span style="color: #007acc">false</span> | Force JRE simulation (testing only) |

> **Note:** All security features are disabled by default for backward compatibility. Most properties accepting `0` disable the feature entirely. Properties can be set via system properties (`-D` flags) or environment variables.

### Logging

Because `java-util` has no dependencies on other libraries, `java-util` uses the Java built-in `java.util.logging` for all output. See the
[user guide](userguide.md#redirecting-javautillogging) for ways to route
these logs to SLF4J or Log4j&nbsp;2.

### User Guide
[View detailed documentation on all utilities.](userguide.md)

See [changelog.md](/changelog.md) for revision history.

---

By: John DeRegnaucourt and Kenny Partlow
