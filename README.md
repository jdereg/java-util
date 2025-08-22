java-util
=========

<!-- Badge Section -->
<div align="center">
<svg width="400" height="60" viewBox="0 0 400 60" xmlns="http://www.w3.org/2000/svg">
    <rect width="400" height="60" rx="6" fill="#1a1a1a"/>
    <rect x="0" y="0" width="140" height="60" rx="6" fill="#ff8800"/>
    <g transform="translate(15, 15)">
        <path d="M15 5 L25 10 L25 20 L15 25 L5 20 L5 10 Z" fill="none" stroke="#ffffff" stroke-width="1.5"/>
        <circle cx="15" cy="15" r="6" fill="none" stroke="#ffffff" stroke-width="2"/>
    </g>
    <text x="50" y="38" font-family="'JetBrains Mono', monospace" font-size="20" font-weight="700" fill="#ffffff">java-util</text>
    <text x="160" y="38" font-family="'Inter', sans-serif" font-size="18" fill="#ffffff">Zero Dependencies</text>
</svg>
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
catalog.putMultiKey("Electronics Department", "electronics");                               // 1D
catalog.putMultiKey("Science Fiction Books", "books", "fiction", "scifi");                  // 3D  
catalog.putMultiKey("Gaming Keyboards", "electronics", "computers", "keyboards", "gaming"); // 4D

// Flexible retrieval using matching dimensions
String dept = catalog.getMultiKey("electronics");                               // Electronics Department
String category = catalog.getMultiKey("books", "fiction", "scifi");            // Science Fiction Books
String product = catalog.getMultiKey("electronics", "computers", "laptops");   // Laptop Computers

// ConcurrentHashMap-level thread safety with lock-free reads
catalog.putMultiKey("Updated Value", "electronics", "computers", "laptops");   // Enterprise-grade concurrency
// Auto-tuned stripe locking: 8-32 stripes based on system cores for optimal write parallelism

// Advanced collection handling with CollectionKeyMode (affects Collections only, not Arrays)
MultiKeyMap<String> configMap = new MultiKeyMap<>(1024, CollectionKeyMode.COLLECTIONS_NOT_EXPANDED);
List<String> configPath = Arrays.asList("database", "connection", "pool");
configMap.put(configPath, "jdbc:mysql://localhost:3306/app");           // Collection tried as single key first
String dbUrl = configMap.get(configPath);                               // Retrieved as single key

// Arrays are ALWAYS expanded and elements are compared by equals(). This is a BIG feature for MultiKeyMap. 
// This is because you can't override equals/hashCode on arrays, as they compare with == (identity).
// Use another Map type if you want array to compare with identity. 
String[] arrayPath = {"database", "connection", "pool"};
configMap.put(arrayPath, "another-value");                                      // ALWAYS stored as 3D key
String arrayValue = configMap.getMultiKey("database", "connection", "pool");    // Retrieved as 3D key

// 🔥 N-DIMENSIONAL ARRAY EXPANSION - The Ultimate Power Feature
// Arrays and Collections expand with structure preservation - no limits on depth!

// ✅ EQUIVALENT (same flat structure, different containers - flattenDimensions = true):
String[] flatArray = {"a", "b", "c"};                    // → ["a", "b", "c"]
List<String> flatList = List.of("a", "b", "c");          // → ["a", "b", "c"]  
Object[] flatObject = {"a", "b", "c"};                   // → ["a", "b", "c"]
configMap.put(flatArray, "value");
// ALL of these work - cross-container equivalence:
String result1 = configMap.get(flatArray);               // ✅ Original String[]
String result2 = configMap.get(flatList);                // ✅ Equivalent List  
String result3 = configMap.get(flatObject);              // ✅ Equivalent Object[]
String result4 = configMap.getMultiKey("a", "b", "c");   // ✅ Individual elements

// ❌ NOT EQUIVALENT (different structures - flattenDimensions = false):
String[][] nested2D = {{"a", "b"}, {"c", "d"}};         // → [["a", "b"],["c", "d"]]
String[] flat1D = {"a", "b", "c", "d"};                 // → ["a", "b", "c", "d"]
// These create SEPARATE entries - different structures preserved!
configMap.put(nested2D, "2D_value");                    // Stored retaining structural info
configMap.put(flat1D, "flat_value");                    // Stored retaining structural info

// Perfect for complex lookups: user permissions, configuration trees, caches
MultiKeyMap<Permission> permissions = new MultiKeyMap<>();
permissions.putMultiKey(Permission.ADMIN, "user123", "project456", "resource789");
Permission userPerm = permissions.getMultiKey("user123", "project456", "resource789");
```

**🔄 Cross-Container Equivalence (Ultimate Flexibility!):**

MultiKeyMap treats **equivalent structures** as **identical keys**, regardless of container type:

```java
MultiKeyMap<String> map = new MultiKeyMap<>();

// 🎯 ALL of these are equivalent (same flat structure):
String[] stringArray = {"user", "profile", "settings"};
Object[] objectArray = {"user", "profile", "settings"};  
int[] intArray = {1, 2, 3};                              // Different elements, same structure
List<String> stringList = List.of("user", "profile", "settings");
List<Object> objectList = List.of("user", "profile", "settings");
ArrayList<String> arrayList = new ArrayList<>(List.of("user", "profile", "settings"));
Set<String> set = Set.of("user", "profile", "settings"); // Even Sets work!

// Store with ANY container type:
map.put(stringArray, "stored-value");

// Retrieve with ANY equivalent container:
map.get(stringArray);      // ✅ "stored-value"
map.get(objectArray);      // ✅ "stored-value" 
map.get(stringList);       // ✅ "stored-value"
map.get(objectList);       // ✅ "stored-value"
map.get(arrayList);        // ✅ "stored-value"
// Only ONE entry exists in the map - they're all the same key!

// 🎯 Nested structures are also equivalent across containers:
List<List<String>> nestedList = List.of(List.of("a", "b"), List.of("c"));
Object[] nestedArray = {new String[]{"a", "b"}, new String[]{"c"}};
map.put(nestedList, "nested-value");
map.get(nestedArray);      // ✅ "nested-value" - same nested structure!
```

**Advanced Configuration Options:**

```java
// Case-sensitive mode for String keys (default is case-insensitive)
MultiKeyMap<String> caseMap = new MultiKeyMap<>(true);  // Case-sensitive mode
caseMap.putMultiKey("Value1", "ABC", "def");
caseMap.putMultiKey("Value2", "abc", "def");  // Different key from "ABC"
String val1 = caseMap.getMultiKey("ABC", "def");  // "Value1"
String val2 = caseMap.getMultiKey("abc", "def");  // "Value2" 

// Value-based equality mode for cross-type numeric comparisons
MultiKeyMap<String> valueMap = new MultiKeyMap<>(1024, true);  // Value-based equality
valueMap.putMultiKey("NumericValue", 42L, 3.14);
// All these retrieve the same entry (cross-type numeric equality):
String result1 = valueMap.getMultiKey(42, 3.14);     // Integer and Double
String result2 = valueMap.getMultiKey(42L, 3.14f);   // Long and Float  
String result3 = valueMap.getMultiKey(42.0, 3.14);   // Double and Double
```

**Why MultiKeyMap is the industry-leading solution:**

| Feature | Guava Table | Apache Commons MultiKeyMap | DIY Record+HashMap | **java-util MultiKeyMap**                                 |
|---------|-------------|----------------------------|-------------------|-----------------------------------------------------------|
| **Performance** | ⚠️ Good (map-of-maps overhead) | ⚠️ Good (single-threaded) | ❌ Poor (key object creation) | ✅ **Excellent** (Thread-safe + competitive performance) |
| **Key Dimensions** | ❌ Limited to 2D only | ✅ Unlimited N-D | ✅ Unlimited N-D | ✅ **Unlimited N-D**                                       |
| **Thread Safety** | ❌ None built-in | ❌ Not thread-safe | ❌ None (manual synchronization) | ✅ **Full ConcurrentMap** (nulls allowed)                  |
| **Type Safety** | ✅ Built-in compile-time | ❌ Untyped Object keys | ✅ Built-in compile-time | ✅ **Façade-ready** (flexible core + typed wrapper)        |
| **Case Handling** | ❌ No built-in support | ❌ No built-in support | ❌ Manual implementation | ✅ **Configurable** (case-sensitive/insensitive)           |
| **Numeric Equality** | ❌ Type-strict only | ❌ Type-strict only | ❌ Type-strict only | ✅ **Value-based option** (42 == 42L == 42.0)             |

### ⏰ TTLCache - Time-Based Caching with LRU
Automatic expiration with optional size limits - supports null keys and values:

```java
// Cache with 30-second TTL
TTLCache<String, User> userCache = new TTLCache<>(TimeUnit.SECONDS.toMillis(30));

// Add items - they auto-expire after TTL
userCache.put("user123", loadUser("123"));
userCache.put(null, defaultUser);  // Null keys supported

// Optional: Add LRU eviction with max size
TTLCache<String, String> sessionCache = new TTLCache<>(
    TimeUnit.MINUTES.toMillis(15),  // 15-minute TTL
    1000                            // Max 1000 items (LRU eviction)
);

// Use like any Map - items auto-expire
sessionCache.put("session-abc", "user-data");
String userData = sessionCache.get("session-abc");  // null if expired

// Perfect for caching expensive operations
TTLCache<String, Result> resultCache = new TTLCache<>(TimeUnit.MINUTES.toMillis(5));
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
| **Jar Size** | N/A | ~2.7MB | ~2.8MB | ~500KB each | ~600KB total     |
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

<table>
<tr style="background-color: #334155;">
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Component</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Description</th>
</tr>
<tr style="background-color: #f0f4f8;">
<td style="color: #334155; font-weight: bold;"><strong>Sets</strong></td>
<td></td>
</tr>
<tr>
<td><a href="userguide.md#compactset">CompactSet</a></td>
<td>Memory-efficient Set that dynamically adapts its storage structure based on size.</td>
</tr>
<tr>
<td><a href="userguide.md#caseinsensitiveset">CaseInsensitiveSet</a></td>
<td>Set implementation with case-insensitive <code>String</code> handling.</td>
</tr>
<tr>
<td><a href="userguide.md#concurrentset">ConcurrentSet</a></td>
<td>Thread-safe Set supporting null elements.</td>
</tr>
<tr>
<td><a href="userguide.md#concurrentnavigablesetnullsafe">ConcurrentNavigableSetNullSafe</a></td>
<td>Thread-safe <code>NavigableSet</code> supporting null elements.</td>
</tr>
<tr>
<td><a href="userguide.md#classvalueset">ClassValueSet</a></td>
<td>High-performance Set optimized for fast <code>Class</code> membership testing using JVM-optimized <code>ClassValue</code>.</td>
</tr>
<tr>
<td><a href="userguide.md#intervalset">IntervalSet</a></td>
<td>Thread-safe interval set with O(log n) performance, automatically merges intervals, smart boundary handling for 20+ types, and you can add your own.</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td style="color: #334155; font-weight: bold;"><strong>Maps</strong></td>
<td></td>
</tr>
<tr>
<td><a href="userguide.md#compactmap">CompactMap</a></td>
<td>Memory-efficient Map that dynamically adapts its storage structure based on size.</td>
</tr>
<tr>
<td><a href="userguide.md#caseinsensitivemap">CaseInsensitiveMap</a></td>
<td>A <code>Map</code> wrapper that provides case-insensitive, case-retentive keys and inherits the features of the wrapped map (e.g., thread-safety from <code>ConcurrentMap</code> types, multi-key support from <code>MultiKeyMap</code>, sorted, thread-safe, allow nulls from <code>ConcurrentNavigableMapNullSafe</code>).</td>
</tr>
<tr>
<td><a href="userguide.md#lrucache">LRUCache</a></td>
<td>Thread-safe Least Recently Used cache with configurable eviction strategies.</td>
</tr>
<tr>
<td><a href="userguide.md#ttlcache">TTLCache</a></td>
<td>Thread-safe Time-To-Live cache with optional size limits.</td>
</tr>
<tr>
<td><a href="userguide.md#trackingmap">TrackingMap</a></td>
<td>A <code>Map</code> wrapper that tracks key access. Inherits features from wrapped <code>Map</code>, including thread-safety (<code>ConcurrentMap</code> types), sorted, thread-safe, with null support (<code>ConcurrentNavigableMapNullSafe</code>)</td>
</tr>
<tr>
<td><a href="userguide.md#concurrenthashmapnullsafe">ConcurrentHashMapNullSafe</a></td>
<td>Thread-safe <code>HashMap</code> supporting null keys and values.</td>
</tr>
<tr>
<td><a href="userguide.md#concurrentnavigablemapnullsafe">ConcurrentNavigableMapNullSafe</a></td>
<td>Thread-safe <code>NavigableMap</code> supporting null keys and values.</td>
</tr>
<tr>
<td><a href="userguide.md#classvaluemap">ClassValueMap</a></td>
<td>High-performance Map optimized for fast <code>Class</code> key lookups using JVM-optimized <code>ClassValue</code>.</td>
</tr>
<tr>
<td><a href="userguide.md#multikeymap">MultiKeyMap</a></td>
<td>Concurrent map supporting multiple keys.</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td style="color: #334155; font-weight: bold;"><strong>Lists</strong></td>
<td></td>
</tr>
<tr>
<td><a href="userguide.md#concurrentlist">ConcurrentList</a></td>
<td>High-performance bucket-based concurrent <code>List</code> and <code>Deque</code> with lock-free operations.</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td style="color: #334155; font-weight: bold;"><strong>Utilities</strong></td>
<td></td>
</tr>
<tr>
<td><a href="userguide.md#arrayutilities">ArrayUtilities</a></td>
<td>Comprehensive array manipulation operations.</td>
</tr>
<tr>
<td><a href="userguide.md#byteutilities">ByteUtilities</a></td>
<td>Byte array and hexadecimal conversion utilities.</td>
</tr>
<tr>
<td><a href="userguide.md#classutilities">ClassUtilities</a></td>
<td>Class relationship and reflection helper methods.</td>
</tr>
<tr>
<td><a href="userguide.md#converter">Converter</a></td>
<td>An extensive and extensible conversion utility with thousands of built-in transformations between common JDK types (Dates, Collections, Primitives, EnumSets, etc.).</td>
</tr>
<tr>
<td><a href="userguide.md#dateutilities">DateUtilities</a></td>
<td>Advanced date parsing and manipulation.</td>
</tr>
<tr>
<td><a href="userguide.md#deepequals">DeepEquals</a></td>
<td>Recursive object graph comparison.</td>
</tr>
<tr>
<td><a href="userguide.md#encryptionutilities">EncryptionUtilities</a></td>
<td>Simplified encryption and checksum operations.</td>
</tr>
<tr>
<td><a href="userguide.md#executor">Executor</a></td>
<td>Streamlined system command execution.</td>
</tr>
<tr>
<td><a href="userguide.md#graphcomparator">GraphComparator</a></td>
<td>Object graph difference detection and synchronization.</td>
</tr>
<tr>
<td><a href="userguide.md#ioutilities">IOUtilities</a></td>
<td>Enhanced I/O operations and streaming utilities.</td>
</tr>
<tr>
<td><a href="userguide.md#mathutilities">MathUtilities</a></td>
<td>Extended mathematical operations.</td>
</tr>
<tr>
<td><a href="userguide.md#reflectionutils">ReflectionUtils</a></td>
<td>Optimized reflection operations.</td>
</tr>
<tr>
<td><a href="userguide.md#stringutilities">StringUtilities</a></td>
<td>Extended <code>String</code> manipulation operations.</td>
</tr>
<tr>
<td><a href="userguide.md#systemutilities">SystemUtilities</a></td>
<td>System and environment interaction utilities.</td>
</tr>
<tr>
<td><a href="userguide.md#traverser">Traverser</a></td>
<td>Configurable object graph traversal.</td>
</tr>
<tr>
<td><a href="userguide.md#typeutilities">TypeUtilities</a></td>
<td>Advanced Java type introspection and generic resolution utilities.</td>
</tr>
<tr>
<td><a href="userguide.md#uniqueidgenerator">UniqueIdGenerator</a></td>
<td>Distributed-safe unique identifier generation.</td>
</tr>
</table>
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
<plugin id="com.cedarsoftware.java-util" version="4.0.0"/>
```

Both of these features ensure that our library can be seamlessly integrated into modular Java applications, providing robust dependency management and encapsulation.

### Maven and Gradle Integration

To include in your project:

##### Gradle
```groovy
implementation 'com.cedarsoftware:java-util:4.0.0'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>4.0.0</version>
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

<table>
<tr style="background-color: #334155;">
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Fully Qualified Property Name</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Allowed Values</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Default Value</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Description</th>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>ArrayUtilities</strong></td>
</tr>
<tr>
<td><code>arrayutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ArrayUtilities security features</td>
</tr>
<tr>
<td><code>arrayutilities.component.type.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block dangerous system classes in array operations</td>
</tr>
<tr>
<td><code>arrayutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">2147483639</span></td>
<td>Maximum array size (Integer.MAX_VALUE-8)</td>
</tr>
<tr>
<td><code>arrayutilities.dangerous.class.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">java.lang.Runtime,<br>java.lang.ProcessBuilder,<br>java.lang.System,<br>java.security.,javax.script.,<br>sun.,com.sun.,java.lang.Class</span></td>
<td>Dangerous class patterns to block</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>ByteUtilities</strong></td>
</tr>
<tr>
<td><code>byteutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ByteUtilities security features</td>
</tr>
<tr>
<td><code>byteutilities.max.hex.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Hex string length limit for decode operations</td>
</tr>
<tr>
<td><code>byteutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Byte array size limit for encode operations</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>DateUtilities</strong></td>
</tr>
<tr>
<td><code>dateutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all DateUtilities security features</td>
</tr>
<tr>
<td><code>dateutilities.input.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable input length and content validation</td>
</tr>
<tr>
<td><code>dateutilities.regex.timeout.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable regex timeout protection</td>
</tr>
<tr>
<td><code>dateutilities.malformed.string.protection.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable malformed input protection</td>
</tr>
<tr>
<td><code>dateutilities.max.input.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1000</span></td>
<td>Maximum input string length</td>
</tr>
<tr>
<td><code>dateutilities.max.epoch.digits</code></td>
<td>Integer</td>
<td><span style="color: #007acc">19</span></td>
<td>Maximum digits for epoch milliseconds</td>
</tr>
<tr>
<td><code>dateutilities.regex.timeout.milliseconds</code></td>
<td>Long</td>
<td><span style="color: #007acc">1000</span></td>
<td>Timeout for regex operations in milliseconds</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>DeepEquals</strong></td>
</tr>
<tr>
<td><code>deepequals.secure.errors</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable error message sanitization</td>
</tr>
<tr>
<td><code>deepequals.max.collection.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Collection size limit</td>
</tr>
<tr>
<td><code>deepequals.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Array size limit</td>
</tr>
<tr>
<td><code>deepequals.max.map.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Map size limit</td>
</tr>
<tr>
<td><code>deepequals.max.object.fields</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Object field count limit</td>
</tr>
<tr>
<td><code>deepequals.max.recursion.depth</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Recursion depth limit</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>EncryptionUtilities</strong></td>
</tr>
<tr>
<td><code>encryptionutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all EncryptionUtilities security features</td>
</tr>
<tr>
<td><code>encryptionutilities.file.size.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable file size limits for hashing operations</td>
</tr>
<tr>
<td><code>encryptionutilities.buffer.size.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable buffer size validation</td>
</tr>
<tr>
<td><code>encryptionutilities.crypto.parameters.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable cryptographic parameter validation</td>
</tr>
<tr>
<td><code>encryptionutilities.max.file.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Maximum file size for hashing operations (2GB)</td>
</tr>
<tr>
<td><code>encryptionutilities.max.buffer.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1048576</span></td>
<td>Maximum buffer size (1MB)</td>
</tr>
<tr>
<td><code>encryptionutilities.min.pbkdf2.iterations</code></td>
<td>Integer</td>
<td><span style="color: #007acc">10000</span></td>
<td>Minimum PBKDF2 iterations</td>
</tr>
<tr>
<td><code>encryptionutilities.max.pbkdf2.iterations</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1000000</span></td>
<td>Maximum PBKDF2 iterations</td>
</tr>
<tr>
<td><code>encryptionutilities.min.salt.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">8</span></td>
<td>Minimum salt size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.max.salt.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">64</span></td>
<td>Maximum salt size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.min.iv.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">8</span></td>
<td>Minimum IV size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.max.iv.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">32</span></td>
<td>Maximum IV size in bytes</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>IOUtilities</strong></td>
</tr>
<tr>
<td><code>io.debug</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable debug logging</td>
</tr>
<tr>
<td><code>io.connect.timeout</code></td>
<td>Integer (1000-300000)</td>
<td><span style="color: #007acc">5000</span></td>
<td>Connection timeout (1s-5min)</td>
</tr>
<tr>
<td><code>io.read.timeout</code></td>
<td>Integer (1000-300000)</td>
<td><span style="color: #007acc">30000</span></td>
<td>Read timeout (1s-5min)</td>
</tr>
<tr>
<td><code>io.max.stream.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Stream size limit (2GB)</td>
</tr>
<tr>
<td><code>io.max.decompression.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Decompression size limit (2GB)</td>
</tr>
<tr>
<td><code>io.path.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Path security validation enabled</td>
</tr>
<tr>
<td><code>io.url.protocol.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>URL protocol validation enabled</td>
</tr>
<tr>
<td><code>io.allowed.protocols</code></td>
<td>Comma-separated</td>
<td><span style="color: #007acc">http,https,file,jar</span></td>
<td>Allowed URL protocols</td>
</tr>
<tr>
<td><code>io.file.protocol.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>File protocol validation enabled</td>
</tr>
<tr>
<td><code>io.debug.detailed.urls</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Detailed URL logging disabled</td>
</tr>
<tr>
<td><code>io.debug.detailed.paths</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Detailed path logging disabled</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>MathUtilities</strong></td>
</tr>
<tr>
<td><code>mathutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all MathUtilities security features</td>
</tr>
<tr>
<td><code>mathutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Array size limit for min/max operations</td>
</tr>
<tr>
<td><code>mathutilities.max.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>String length limit for parsing</td>
</tr>
<tr>
<td><code>mathutilities.max.permutation.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>List size limit for permutations</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>ReflectionUtils</strong></td>
</tr>
<tr>
<td><code>reflectionutils.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ReflectionUtils security features</td>
</tr>
<tr>
<td><code>reflectionutils.dangerous.class.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block dangerous class access</td>
</tr>
<tr>
<td><code>reflectionutils.sensitive.field.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block sensitive field access</td>
</tr>
<tr>
<td><code>reflectionutils.max.cache.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">50000</span></td>
<td>Maximum cache size per cache type</td>
</tr>
<tr>
<td><code>reflectionutils.dangerous.class.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">java.lang.Runtime,java.lang.Process,<br>java.lang.ProcessBuilder,sun.misc.Unsafe,<br>jdk.internal.misc.Unsafe,<br>javax.script.ScriptEngine,<br>javax.script.ScriptEngineManager</span></td>
<td>Dangerous class patterns</td>
</tr>
<tr>
<td><code>reflectionutils.sensitive.field.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">password,passwd,secret,secretkey,<br>apikey,api_key,authtoken,accesstoken,<br>credential,confidential,adminkey,private</span></td>
<td>Sensitive field patterns</td>
</tr>
<tr>
<td><code>reflection.utils.cache.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1500</span></td>
<td>Reflection cache size</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>StringUtilities</strong></td>
</tr>
<tr>
<td><code>stringutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all StringUtilities security features</td>
</tr>
<tr>
<td><code>stringutilities.max.hex.decode.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max hex string size for decode()</td>
</tr>
<tr>
<td><code>stringutilities.max.wildcard.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max wildcard pattern length</td>
</tr>
<tr>
<td><code>stringutilities.max.wildcard.count</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max wildcard characters in pattern</td>
</tr>
<tr>
<td><code>stringutilities.max.levenshtein.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max string length for Levenshtein distance</td>
</tr>
<tr>
<td><code>stringutilities.max.damerau.levenshtein.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max string length for Damerau-Levenshtein</td>
</tr>
<tr>
<td><code>stringutilities.max.repeat.count</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max repeat count for repeat() method</td>
</tr>
<tr>
<td><code>stringutilities.max.repeat.total.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max total size for repeat() result</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>SystemUtilities</strong></td>
</tr>
<tr>
<td><code>systemutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all SystemUtilities security features</td>
</tr>
<tr>
<td><code>systemutilities.environment.variable.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block sensitive environment variable access</td>
</tr>
<tr>
<td><code>systemutilities.file.system.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Validate file system operations</td>
</tr>
<tr>
<td><code>systemutilities.resource.limits.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enforce resource usage limits</td>
</tr>
<tr>
<td><code>systemutilities.max.shutdown.hooks</code></td>
<td>Integer</td>
<td><span style="color: #007acc">100</span></td>
<td>Maximum number of shutdown hooks</td>
</tr>
<tr>
<td><code>systemutilities.max.temp.prefix.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">100</span></td>
<td>Maximum temporary directory prefix length</td>
</tr>
<tr>
<td><code>systemutilities.sensitive.variable.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">PASSWORD,PASSWD,PASS,SECRET,KEY,<br>TOKEN,CREDENTIAL,AUTH,APIKEY,API_KEY,<br>PRIVATE,CERT,CERTIFICATE,DATABASE_URL,<br>DB_URL,CONNECTION_STRING,DSN,<br>AWS_SECRET,AZURE_CLIENT_SECRET,<br>GCP_SERVICE_ACCOUNT</span></td>
<td>Sensitive variable patterns</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>Traverser</strong></td>
</tr>
<tr>
<td><code>traverser.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all Traverser security features</td>
</tr>
<tr>
<td><code>traverser.max.stack.depth</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum stack depth</td>
</tr>
<tr>
<td><code>traverser.max.objects.visited</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum objects visited</td>
</tr>
<tr>
<td><code>traverser.max.collection.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum collection size to process</td>
</tr>
<tr>
<td><code>traverser.max.array.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum array length to process</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>UrlUtilities</strong></td>
</tr>
<tr>
<td><code>urlutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all UrlUtilities security features</td>
</tr>
<tr>
<td><code>urlutilities.max.download.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max download size in bytes</td>
</tr>
<tr>
<td><code>urlutilities.max.content.length</code></td>
<td>Long</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max Content-Length header value</td>
</tr>
<tr>
<td><code>urlutilities.allow.internal.hosts</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">true</span></td>
<td>Allow access to internal/local hosts</td>
</tr>
<tr>
<td><code>urlutilities.allowed.protocols</code></td>
<td>Comma-separated</td>
<td><span style="color: #007acc">http,https,ftp</span></td>
<td>Allowed protocols</td>
</tr>
<tr>
<td><code>urlutilities.strict.cookie.domain</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable strict cookie domain validation</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>Converter</strong></td>
</tr>
<tr>
<td><code>converter.modern.time.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for Instant, ZonedDateTime, OffsetDateTime conversions</td>
</tr>
<tr>
<td><code>converter.duration.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for Duration conversions</td>
</tr>
<tr>
<td><code>converter.localtime.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for LocalTime conversions</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;"><strong>Other</strong></td>
</tr>
<tr>
<td><code>java.util.force.jre</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Force JRE simulation (testing only)</td>
</tr>
</table>

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
