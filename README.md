<p align="center">
  <img src="infographic.svg?v=3" alt="java-util infographic - Essential Java Toolkit" width="100%" />
</p>
<div align="center">
  <p>
    <a href="https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware">
      <img src="https://badgen.net/maven/v/maven-central/com.cedarsoftware/java-util" alt="Maven Central" height="20" />
    </a>
    <a href="http://www.javadoc.io/doc/com.cedarsoftware/java-util">
      <img src="https://javadoc.io/badge/com.cedarsoftware/java-util.svg" alt="Javadoc" height="20" />
    </a>
    <a href="https://github.com/jdereg/java-util/blob/master/LICENSE">
      <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" height="20" />
    </a>
    <img src="https://img.shields.io/badge/JDK-8%20to%2024-orange" alt="JDK 8–24" height="20" />
  </p>

  <p>
    <a href="https://github.com/jdereg/java-util">
      <img src="https://img.shields.io/github/stars/jdereg/java-util?style=social" alt="GitHub stars" />
    </a>
    <a href="https://github.com/jdereg/java-util/fork">
      <img src="https://img.shields.io/github/forks/jdereg/java-util?style=social" alt="GitHub forks" />
    </a>
  </p>
</div>

A collection of high-performance Java utilities designed to enhance standard Java functionality. These utilities focus on:
- Memory efficiency and performance optimization
- Thread-safety and concurrent operations
- Enhanced collection implementations
- Simplified common programming tasks
- Deep object graph operations

Available on [Maven Central](https://central.sonatype.com/search?q=java-util&namespace=com.cedarsoftware).
This library has <b>no dependencies</b> on other libraries for runtime.
The`.jar`file is `~700K` and works with `JDK 1.8` through `JDK 24`.
The `.jar` file classes are version 52 `(JDK 1.8)`

As of version 3.6.0 the library is built with the `-parameters`
compiler flag. Parameter names are now retained for tasks such as
constructor discovery (increased the jar size by about 10K.)

---

## Table of Contents

- [Cloud Native & Container Ready](#cloud-native--container-ready)
- [JDK Module Requirements](#jdk-module-requirements)
- [Featured Utilities](#featured-utilities)
  - [DeepEquals](#-deepequals---complete-object-comparison)
  - [Converter](#-converter---universal-type-conversion)
  - [TTLCache](#-ttlcache---self-cleaning-time-based-cache)
  - [CompactMap](#-compactmap---self-optimizing-storage)
  - [MultiKeyMap](#-multikeymap---composite-key-mapping)
- [How java-util Compares](#how-java-util-compares)
- [Enterprise Security Features](#-enterprise-security-features)
  - [Input Validation & DOS Protection](#️-input-validation--dos-protection)
  - [Dangerous Class Protection](#-dangerous-class-protection)
  - [Cryptographic Security](#-cryptographic-security)
  - [Network Security Controls](#-network-security-controls)
  - [Security Audit & Monitoring](#-security-audit--monitoring)
  - [Zero-Downtime Security Hardening](#-zero-downtime-security-hardening)
  - [Security Compliance](#-security-compliance)
- [Core Components](#core-components)
- [Integration and Module Support](#integration-and-module-support)
  - [JPMS](#jpms-java-platform-module-system)
  - [OSGi](#osgi)
  - [Maven and Gradle Integration](#maven-and-gradle-integration)
  - [Framework Integration Examples](#-framework-integration-examples)
- [Feature Options](features.md)
- [Logging](#logging)
- [User Guide](#user-guide)

---

## Cloud Native & Container Ready

Optimized for modern cloud deployments and container environments:

**Technical Characteristics:**
- **Minimal Footprint**: ~700KB JAR
- **Zero Runtime Dependencies**: No transitive dependencies to manage, reducing classpath conflicts and container image complexity
- **Fast Startup**: JDK 8 bytecode (class file format 52) with instant classloading, optimized for serverless cold starts
- **Thread-Safe**: All concurrent collections designed for horizontal scaling in Kubernetes and containerized environments
- **JPMS/OSGi Support**: Works with jlink for custom JRE builds (<50MB runtime), compatible with OSGi frameworks (Karaf, Felix, Equinox)

**Deployment Environments:**
- Container platforms (Docker, Kubernetes, OpenShift)
- Serverless functions (AWS Lambda, Azure Functions, Google Cloud Functions, Cloud Run)
- Cloud infrastructure (ECS, EKS, AKS, GKE, Fargate, App Engine)
- Edge computing and microservices architectures

**Security Benefits:**
- Minimal attack surface with single artifact dependency tracking
- Uses `java.util.logging` only (no Log4Shell exposure)
- Air-gap and compliance-friendly for restricted environments

## JDK Module Requirements

### Required Modules
**java.sql** - Required for date/time conversions (`java.sql.Timestamp` and `java.sql.Date`)
- **Impact**: ~500KB footprint, includes JDBC API interfaces (no drivers required)
- **Why**: Core `Converter` uses `java.sql.Timestamp` extensively for date/time type conversions
- **Headless containers**: This adds ~500KB to your deployment but does NOT require database connectivity or JDBC drivers

### Optional Modules (static dependencies)
**java.xml** - Optional for `IOUtilities` XML stream operations (`javax.xml.stream.*`)
- Only needed for XML-specific methods in `IOUtilities`
- Most library functionality works without it
- See `IOUtilities` javadoc for details

### JPMS Module Descriptor
When using java-util as a JPMS module, add to your `module-info.java`:
```java
requires com.cedarsoftware.util;  // Automatically brings in java.sql
// java.xml is marked as 'static' - not required at runtime
```

### OSGi
The OSGi manifest automatically imports all required packages. Optional packages (`javax.xml.stream`) are marked as optional imports.

## Featured Utilities

### 🚀 DeepEquals - Complete Object Comparison

**What**: Compare any two Java objects for complete equality, handling all data types including cyclic references.

**Why use it**:
- ✅ Works with any objects - no equals() method needed
- ✅ Handles circular references and complex nested structures
- ✅ Perfect for testing, debugging, and data validation
- ✅ Secure error messages with automatic sensitive data redaction
- ✅ Detailed difference reporting with path to mismatch

**Quick example**:
```java
boolean same = DeepEquals.deepEquals(complexObject1, complexObject2);

// With difference reporting
Map<String, Object> options = new HashMap<>();
boolean same = DeepEquals.deepEquals(obj1, obj2, options);
if (!same) {
    String diff = (String) options.get(DeepEquals.DIFF);
    System.out.println("Difference: " + diff);
}
```

📖 [Full documentation and options →](userguide.md#deepequals)

---

### 🎯 Converter - Universal Type Conversion

**What**: Convert between many Java types with a single API - no more scattered conversion logic.

**Why use it**:
- ✅ ~1800 type conversions out of the box (use `.allAllSupportedConversions()` to list them out)
- ✅ Extensible - add your own custom conversions
- ✅ Handles complex types including temporal, arrays, and collections

**Quick example**:
```java
Date date = Converter.convert("2024-01-15", Date.class);
Long number = Converter.convert("42.7", Long.class);  // Returns 43
```

📖 [Full documentation and conversion matrix →](userguide.md#converter)

---

### ⏰ TTLCache - Self-Cleaning Time-Based Cache

**What**: A thread-safe cache that automatically expires entries after a time-to-live period, plus includes full LRU capability.

**Why use it**:
- ✅ Automatic memory management - no manual cleanup needed
- ✅ Prevents memory leaks from forgotten cache entries
- ✅ Perfect for session data, API responses, and temporary results

**Quick example**:
```java
TTLCache<String, User> userCache = new TTLCache<>(5, TimeUnit.MINUTES);
userCache.put("user123", user);  // Auto-expires in 5 minutes
User cached = userCache.get("user123");  // Returns user or null if expired
```

📖 [Full documentation and configuration →](userguide.md#ttlcache)

---

### 🔄 CompactMap - Self-Optimizing Storage

**What**: A Map implementation that automatically switches between compact and traditional storage based on size.

**Why use it**:
- ✅ Significant memory reduction for small maps (under ~60 elements)
- ✅ Automatically scales up for larger datasets
- ✅ Drop-in replacement for HashMap - no code changes needed

**Quick example**:
```java
Map<String, Object> map = new CompactMap<>();  // Starts compact
map.put("key", "value");  // Uses minimal memory
// Automatically expands when needed - completely transparent
```

📖 [Full documentation and benchmarks →](userguide.md#compactmap)

---

### 🔑 MultiKeyMap - Composite Key Mapping

**What**: Index objects with unlimited keys (decision variables). Useful for pricing tables, configuration trees, decision tables, arrays and collections as keys, matrix as key. 

**Why use it**:
- ✅ Composite keys without ceremony – Stop gluing keys into strings or writing boilerplate Pair/wrapper classes.
- ✅ Real-world key shapes – Use arrays, collections, jagged multi-dimensional arrays, matrices/tensors, etc., as key components; deep equality & hashing mean “same contents” truly equals “same key.”
- ✅ Cleaner, safer code – No more hand-rolled equals()/hashCode() on ad-hoc key objects. Fewer collision bugs, fewer “why doesn’t this look up?” moments.
- ✅ Beats nested maps – One structure instead of Map<A, Map<B, V>>. Simpler reads/writes, simpler iteration, simpler mental model.
- ✅ Follows same concurrency semantics as ConcurrentHashMap.
- ✅ Map-like ergonomics – Familiar put/get/contains/remove semantics; drop-in friendly alongside the rest of java.util collections.
- ✅ Fewer allocations – Avoid creating short-lived wrapper objects just to act as a key; reduce GC pressure versus “make-a-key-object-per-call.”
- ✅ Better iteration & analytics – Iterate entries once; no nested loops to walk inner maps when you just need all (k1,k2,…,v) tuples.
- ✅ Easier indexing patterns – Natural fit for multi-attribute lookups (e.g., (tenantId, userId), (type, region), (dateBucket, symbol)).
- ✅ Configurable case-insensitivity (case-retaining) – opt in to case-insensitive matching where you want it, keep exact matching where you don’t—all while preserving original casing for display/logging.

**Quick examples**:

Example 1 — Composite key that includes a small jagged array
```java
// Composite key: [[1, 2], "some key"]  -> value
MultiKeyMap<String> map = new MultiKeyMap<>();

Object[] compositeKey = new Object[] { new int[]{1, 2}, "some key" };
map.put(compositeKey, "payload-123");

// Retrieve using a *new* array with the same contents (deep equality)
String v1 = map.get(compositeKey);  // v1 = "payload-123"

// Standard Map operations work with the composite array key
boolean present = map.containsKey(compositeKey);   // true
map.remove(compositeKey);
map.containsKey(compositeKey); // false
```
Example 2 — Var-args style (no ambiguity with Map.put/get)
```java
// Var-args API: putMultiKey(value, k1, k2, k3) and getMultiKey(k1, k2, k3)
// Use this when you already have the distinct keys in hand.
MultiKeyMap<String> map = new MultiKeyMap<>();

String tenantId = "acme";
long userId = 42L;
String scope = "read:invoices";

// Value first by design (var-args must be last)
map.putMultiKey("granted", tenantId, userId, scope);

String perm = map.getMultiKey(tenantId, userId, scope);
System.out.println(perm); // prints: granted

boolean ok = map.containsMultiKey(tenantId, userId, scope);
System.out.println(ok); // true

map.removeMultiKey(tenantId, userId, scope);
System.out.println(map.containsMultiKey(tenantId, userId, scope)); // false
```

📖 [Full documentation and use cases →](userguide.md#multikeymap)

---

### 🎁 Plus Many More Utilities

From reflection helpers to graph traversal, concurrent collections to date utilities - java-util has you covered. [Browse all utilities →](#core-components)

**Why developers love these utilities:**
- **Zero dependencies** - No classpath conflicts
- **Null-safe** - Handle edge cases gracefully  
- **High performance** - Optimized for real-world usage
- **JDK 8+ compatible** - Works everywhere
- **Production proven** - Used in high-scale applications

## How java-util Compares

| Feature | JDK Collections | Google Guava | Eclipse Collections | Apache Commons | **java-util**                 |
|---------|----------------|--------------|---------------------|----------------|-------------------------------|
| **Dependencies** | None | 3+ libraries | 2+ libraries | Multiple | None                          |
| **Jar Size** | N/A | ~2.7MB | ~2.8MB | ~500KB each | ~700KB total                  |
| **JDK Compatibility** | 8+ | 11+ (latest) | 11+ | 8+ | 8+                            |
| **Null-Safe Concurrent** | ❌ | ❌ | ❌ | ❌ | ✅ ConcurrentMapNullSafe       |
| **Memory-Adaptive Collections** | ❌ | ❌ | ✅ | ❌ | ✅ CompactMap/Set              |
| **Case-Preserving Maps** | ❌ | ❌ | ❌ | Limited | ✅ Retains original case       |
| **Universal Type Conversion** | ❌ | Limited | ❌ | Limited | ✅ ~1800 conversions           |
| **N-Dimensional Mapping** | ❌ | ⚠️ Table (2D only) | ❌ | ⚠️ Limited | ✅ MultiKeyMap (unlimited N-D) |
| **Deep Object Comparison** | ❌ | Limited | ❌ | ❌ | ✅ Handles cycles              |
| **Runtime Configuration** | ❌ | ❌ | ❌ | ❌ | ✅ 70+ feature options         |
| **TTL Caching** | ❌ | ✅ | ❌ | ❌ | ✅ + LRU combo                 |
| **Thread-Safe with Nulls** | ❌ | ❌ | ❌ | ❌ | ✅ All concurrent types        |
| **JPMS/OSGi Ready** | ✅ | ⚠️ | ✅ | ⚠️ | ✅ Pre-configured              |
| **Security Controls** | ❌ | ❌ | ❌ | ❌ | ✅ Input validation            |

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
<td><a href="userguide.md#identityset">IdentitySet</a></td>
<td>High-performance Set using object identity (<code>==</code>) instead of <code>equals()</code>. Faster than <code>Collections.newSetFromMap(new IdentityHashMap<>())</code>.</td>
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
<td><a href="userguide.md#datageneratorinputstream">DataGeneratorInputStream</a></td>
<td>Memory-efficient <code>InputStream</code> for generating test data on-the-fly with multiple generation modes (random, sequential, patterns, custom).</td>
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
<td><a href="userguide.md#regexutilities">RegexUtilities</a></td>
<td>Safe regex operations with ReDoS protection, pattern caching, and timeout enforcement.</td>
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
<plugin id="com.cedarsoftware.java-util" version="LATEST_VERSION"/>
```

Both of these features ensure that our library can be seamlessly integrated into modular Java applications, providing robust dependency management and encapsulation.

### Maven and Gradle Integration

To include in your project:
> Replace `LATEST_VERSION` with the version shown here:
[![Maven Central](https://img.shields.io/maven-central/v/com.cedarsoftware/java-util)](https://central.sonatype.com/artifact/com.cedarsoftware/java-util)

##### Gradle
```groovy
implementation 'com.cedarsoftware:java-util:LATEST_VERSION'
```

##### Maven
```xml
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

### 🚀 Framework Integration Examples

For comprehensive framework integration examples including Spring, Jakarta EE, Spring Boot Auto-Configuration, Microservices, Testing, and Performance Monitoring.

Key integrations include:
- **[Spring Framework](frameworks.md#spring-framework-integration)** - Configuration beans and case-insensitive property handling
- **[Jakarta EE/JEE](frameworks.md#jakarta-ee--jee-integration)** - CDI producers and validation services
- **[Spring Boot](frameworks.md#spring-boot-auto-configuration)** - Auto-configuration with corrected cache constructors
- **[Microservices](frameworks.md#microservices--cloud-native)** - Service discovery and cloud-native configuration
- **[Testing](frameworks.md#testing-integration)** - Enhanced test comparisons with DeepEquals
- **[Monitoring](frameworks.md#performance-monitoring-integration)** - Micrometer metrics integration

## Feature Options

java-util provides **70+ runtime configuration options** via system properties, enabling:

- **Zero-downtime security hardening** - Enable security features without code changes
- **Environment-specific tuning** - Different limits for development vs. production
- **Gradual rollout strategies** - Test new security features with feature flags
- **Compliance flexibility** - Meet varying regulatory requirements across deployments

All security features are **disabled by default** for backward compatibility.

📖 **[View complete feature options reference →](features.md)**

### Logging

Because `java-util` has no dependencies on other libraries, `java-util` uses the Java built-in `java.util.logging` for all output. See the
[user guide](userguide.md#redirecting-javautillogging) for ways to route
these logs to SLF4J or Log4j&nbsp;2.

### User Guide
[View detailed documentation on all utilities.](userguide.md)

See [changelog.md](/changelog.md) for revision history.
