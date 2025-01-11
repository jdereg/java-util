# User Guide for java-util

## CompactSet

[View Source](/src/main/java/com/cedarsoftware/util/CompactSet.java)

A memory-efficient `Set` implementation that internally uses `CompactMap`. This implementation provides the same memory benefits as `CompactMap` while maintaining proper Set semantics.

### Key Features

- Configurable case sensitivity for String elements
- Flexible element ordering options:
    - Sorted order
    - Reverse order
    - Insertion order
    - No oOrder
- Customizable compact size threshold
- Memory-efficient internal storage

### Usage Examples

```java
// Create a case-insensitive, sorted CompactSet
CompactSet<String> set = CompactSet.<String>builder()
    .caseSensitive(false)
    .sortedOrder()
    .compactSize(70)
    .build();

// Create a CompactSet with insertion ordering
CompactSet<String> ordered = CompactSet.<String>builder()
    .insertionOrder()
    .build();
```

### Configuration Options

#### Case Sensitivity
- Control case sensitivity for String elements using `.caseSensitive(boolean)`
- Useful for scenarios where case-insensitive string comparison is needed

#### Element Ordering
Choose from three ordering strategies:
- `sortedOrder()`: Elements maintained in natural sorted order
- `reverseOrder()`: Elements maintained in reverse sorted order
- `insertionOrder()`: Elements maintained in the order they were added
- `noOrder()`: Elements maintained in an arbitrary order

#### Compact Size
- Set custom threshold for compact storage using `.compactSize(int)`
- Allows fine-tuning of memory usage vs performance tradeoff

### Implementation Notes

- Built on top of `CompactMap` for memory efficiency
- Maintains proper Set semantics while optimizing storage
- Thread-safe when properly synchronized externally
---
## CaseInsensitiveSet

[View Source](/src/main/java/com/cedarsoftware/util/CaseInsensitiveSet.java)

A specialized `Set` implementation that performs case-insensitive comparisons for String elements while preserving their original case. This collection can contain both String and non-String elements, making it versatile for mixed-type usage.

### Key Features

- **Case-Insensitive String Handling**
    - Performs case-insensitive comparisons for String elements
    - Preserves original case when iterating or retrieving elements
    - Treats non-String elements as a normal Set would

- **Flexible Collection Types**
    - Supports both homogeneous (all Strings) and heterogeneous (mixed types) collections
    - Maintains proper Set semantics for all element types

- **Customizable Backing Storage**
    - Supports various backing map implementations for different use cases
    - Automatically selects appropriate backing store based on input collection type

### Usage Examples

```java
// Create a basic case-insensitive set
CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
set.add("Hello");
set.add("HELLO");  // No effect, as "Hello" already exists
System.out.println(set);  // Outputs: [Hello]

// Mixed-type usage
CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
mixedSet.add("Apple");
mixedSet.add(123);
mixedSet.add("apple");  // No effect, as "Apple" already exists
System.out.println(mixedSet);  // Outputs: [Apple, 123]
```

### Construction Options

1. **Default Constructor**
   ```java
   CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
   ```
   Creates an empty set with default initial capacity and load factor.

2. **Initial Capacity**
   ```java
   CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(100);
   ```
   Creates an empty set with specified initial capacity.

3. **From Existing Collection**
   ```java
   Collection<String> source = List.of("A", "B", "C");
   CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
   ```
   The backing map is automatically selected based on the source collection type:
    - `ConcurrentNavigableSetNullSafe` → `ConcurrentNavigableMapNullSafe`
    - `ConcurrentSkipListSet` → `ConcurrentSkipListMap`
    - `ConcurrentSet` → `ConcurrentHashMapNullSafe`
    - `SortedSet` → `TreeMap`
    - Others → `LinkedHashMap`

### Implementation Notes

- Thread safety depends on the backing map implementation
- String comparisons are case-insensitive but preserve original case
- Set operations use the underlying `CaseInsensitiveMap` for consistent behavior
- Maintains proper `Set` contract while providing case-insensitive functionality for strings

---
## ConcurrentSet
[Source](/src/main/java/com/cedarsoftware/util/ConcurrentSet.java)

A thread-safe Set implementation that supports null elements while maintaining full concurrent operation safety.

### Key Features
- Full thread-safety for all operations
- Supports null elements (unlike ConcurrentHashMap's keySet)
- Implements complete Set interface
- Efficient concurrent operations
- Consistent iteration behavior
- No external synchronization needed

### Implementation Details
- Built on top of ConcurrentHashMap's keySet
- Uses a sentinel object (NULL_ITEM) to represent null values internally
- Maintains proper Set contract even with null elements
- Thread-safe iterator that reflects real-time state of the set

### Usage Examples

**Basic Usage:**
```java
// Create empty set
ConcurrentSet<String> set = new ConcurrentSet<>();

// Add elements (including null)
set.add("first");
set.add(null);
set.add("second");

// Check contents
boolean hasNull = set.contains(null);      // true
boolean hasFirst = set.contains("first");  // true
```

**Create from Existing Collection:**
```java
List<String> list = Arrays.asList("one", null, "two");
ConcurrentSet<String> set = new ConcurrentSet<>(list);
```

**Concurrent Operations:**
```java
ConcurrentSet<String> set = new ConcurrentSet<>();

// Safe for concurrent access
CompletableFuture.runAsync(() -> set.add("async1"));
CompletableFuture.runAsync(() -> set.add("async2"));

// Iterator is thread-safe
for (String item : set) {
    // Safe to modify set while iterating
    set.remove("async1");
}
```

**Bulk Operations:**
```java
ConcurrentSet<String> set = new ConcurrentSet<>();
set.addAll(Arrays.asList("one", "two", "three"));

// Remove multiple items
set.removeAll(Arrays.asList("one", "three"));

// Retain only specific items
set.retainAll(Collections.singleton("two"));
```

### Performance Characteristics
- Read operations: O(1)
- Write operations: O(1)
- Space complexity: O(n)
- Thread-safe without blocking
- Optimized for concurrent access

### Use Cases
- High-concurrency environments
- Multi-threaded data structures
- Thread-safe caching
- Concurrent set operations requiring null support
- Real-time data collection

### Thread Safety Notes
- All operations are thread-safe
- Iterator reflects real-time state of the set
- No external synchronization needed
- Safe to modify while iterating
- Atomic operation guarantees maintained

---
## ConcurrentNavigableSetNullSafe
[Source](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableSetNullSafe.java)

A thread-safe NavigableSet implementation that supports null elements while maintaining sorted order. This class provides all the functionality of ConcurrentSkipListSet with added null element support.

### Key Features
- Full thread-safety for all operations
- Supports null elements (unlike ConcurrentSkipListSet)
- Maintains sorted order
- Supports custom comparators
- Provides navigational operations (lower, higher, floor, ceiling)
- Range-view operations (subSet, headSet, tailSet)
- Bidirectional iteration

### Usage Examples

**Basic Usage:**
```java
// Create with natural ordering
NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
set.add("B");
set.add(null);
set.add("A");
set.add("C");

// Iteration order will be: A, B, C, null
for (String s : set) {
    System.out.println(s);
}
```

**Custom Comparator:**
```java
// Create with custom comparator (reverse order)
NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(
    Comparator.reverseOrder()
);
set.add("B");
set.add(null);
set.add("A");

// Iteration order will be: null, C, B, A
```

**Navigation Operations:**
```java
NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
set.add(1);
set.add(3);
set.add(5);
set.add(null);

Integer lower = set.lower(3);     // Returns 1
Integer higher = set.higher(3);    // Returns 5
Integer ceiling = set.ceiling(2);  // Returns 3
Integer floor = set.floor(4);      // Returns 3
```

**Range Views:**
```java
NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
set.addAll(Arrays.asList(1, 3, 5, 7, null));

// Get subset (exclusive end)
SortedSet<Integer> subset = set.subSet(2, 6);  // Contains 3, 5

// Get headSet (elements less than value)
SortedSet<Integer> head = set.headSet(4);      // Contains 1, 3

// Get tailSet (elements greater than or equal)
SortedSet<Integer> tail = set.tailSet(5);      // Contains 5, 7, null
```

**Descending Views:**
```java
NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
set.addAll(Arrays.asList("A", "B", "C", null));

// Get descending set
NavigableSet<String> reversed = set.descendingSet();
// Iteration order will be: null, C, B, A

// Use descending iterator
Iterator<String> it = set.descendingIterator();
```

### Implementation Details
- Built on ConcurrentSkipListSet
- Uses UUID-based sentinel value for null elements
- Maintains proper ordering with null elements
- Thread-safe iterator reflecting real-time state
- Supports both natural ordering and custom comparators

### Performance Characteristics
- Contains/Add/Remove: O(log n)
- Size: O(1)
- Iteration: O(n)
- Memory: O(n)
- Thread-safe without blocking

### Use Cases
- Concurrent ordered collections requiring null support
- Range-based queries in multi-threaded environment
- Priority queues with null values
- Sorted concurrent data structures
- Real-time data processing with ordering requirements

### Thread Safety Notes
- All operations are thread-safe
- Iterator reflects real-time state
- No external synchronization needed
- Safe for concurrent modifications
- Maintains consistency during range-view operations

---
## CompactMap
[Source](/src/main/java/com/cedarsoftware/util/CompactMap.java)

A memory-efficient Map implementation that dynamically adapts its internal storage structure to minimize memory usage while maintaining excellent performance.

### Key Features
- Dynamic storage optimization based on size
- Builder pattern for creation and configuration
- Support for case-sensitive/insensitive String keys
- Configurable ordering (sorted, reverse, insertion, unordered)
- Custom backing map implementations
- Thread-safe when wrapped with Collections.synchronizedMap()
- Full Map interface implementation

### Usage Examples

**Basic Usage:**
```java
// Simple creation
CompactMap<String, Object> map = new CompactMap<>();
map.put("key", "value");

// Create from existing map
Map<String, Object> source = new HashMap<>();
CompactMap<String, Object> copy = new CompactMap<>(source);
```

**Builder Pattern (Recommended):**
```java
// Case-insensitive, sorted map
CompactMap<String, Object> map = CompactMap.<String, Object>builder()
    .caseSensitive(false)
    .sortedOrder()
    .compactSize(65)
    .build();

// Insertion-ordered map
CompactMap<String, Object> ordered = CompactMap.<String, Object>builder()
    .insertionOrder()
    .mapType(LinkedHashMap.class)
    .build();
```

**Configuration Options:**
```java
// Comprehensive configuration
CompactMap<String, Object> configured = CompactMap.<String, Object>builder()
    .caseSensitive(false)          // Case-insensitive keys
    .compactSize(60)               // Custom transition threshold
    .mapType(TreeMap.class)        // Custom backing map
    .singleValueKey("uuid")        // Optimize single-entry storage
    .sourceMap(existingMap)        // Initialize with data
    .sortedOrder()                 // Or: .reverseOrder(), .insertionOrder()
    .build();
```

### Storage States
1. Empty: Minimal memory footprint
2. Single Entry: Optimized single key-value storage
3. Compact Array: Efficient storage for 2 to N entries
4. Backing Map: Full map implementation for larger sizes

### Configuration Options
- **Case Sensitivity:** Controls String key comparison
- **Compact Size:** Threshold for switching to backing map (default: 70)
- **Map Type:** Backing map implementation (HashMap, TreeMap, etc.)
- **Single Value Key:** Key for optimized single-entry storage
- **Ordering:** Unordered, sorted, reverse, or insertion order

### Performance Characteristics
- Get/Put/Remove: O(n) for maps < compactSize(), O(1) or O(log n) for sorted or reverse
- compactSize() of 60-70 from emperical testing, provides key memory savings with great performance 
- Memory Usage: Optimized based on size (Maps < compactSize() use minimal memory)
- Iteration: Maintains configured ordering
- Thread Safety: Safe when wrapped with Collections.synchronizedMap()

### Use Cases
- Applications with many small maps
- Memory-constrained environments
- Configuration storage
- Cache implementations
- Data structures requiring different ordering strategies
- Systems with varying map sizes

### Thread Safety Notes
- Not thread-safe by default
- Use Collections.synchronizedMap() for thread safety
- Iterator operations require external synchronization
- Atomic operations not guaranteed without synchronization

---
## CaseInsensitiveMap
[Source](/src/main/java/com/cedarsoftware/util/CaseInsensitiveMap.java)

A Map implementation that provides case-insensitive key comparison for String keys while preserving their original case. Non-String keys are handled normally.

### Key Features
- Case-insensitive String key comparison
- Original String case preservation
- Full Map interface implementation including Java 8+ methods
- Efficient caching of case-insensitive String representations
- Support for various backing map implementations
- Compatible with all standard Map operations
- Thread-safe when using appropriate backing map

### Usage Examples

**Basic Usage:**
```java
// Create empty map
CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
map.put("Key", "Value");
map.get("key");   // Returns "Value"
map.get("KEY");   // Returns "Value"

// Create from existing map
Map<String, Object> source = Map.of("Name", "John", "AGE", 30);
CaseInsensitiveMap<String, Object> copy = new CaseInsensitiveMap<>(source);
```

**Mixed Key Types:**
```java
CaseInsensitiveMap<Object, String> mixed = new CaseInsensitiveMap<>();
mixed.put("Name", "John");        // String key - case insensitive
mixed.put(123, "Number");         // Integer key - normal comparison
mixed.put("name", "Jane");        // Overwrites "Name" entry
```

**With Different Backing Maps:**
```java
// With TreeMap for sorted keys
Map<String, Object> treeMap = new TreeMap<>();
CaseInsensitiveMap<String, Object> sorted = 
    new CaseInsensitiveMap<>(treeMap);

// With ConcurrentHashMap for thread safety
Map<String, Object> concurrentMap = new ConcurrentHashMap<>();
CaseInsensitiveMap<String, Object> threadSafe = 
    new CaseInsensitiveMap<>(concurrentMap);
```

**Java 8+ Operations:**
```java
CaseInsensitiveMap<String, Integer> scores = new CaseInsensitiveMap<>();

// computeIfAbsent
scores.computeIfAbsent("Player", k -> 0);

// merge
scores.merge("PLAYER", 10, Integer::sum);

// forEach
scores.forEach((key, value) -> 
    System.out.println(key + ": " + value));
```

### Performance Characteristics
- Get/Put/Remove: O(1) with HashMap backing
- Memory Usage: Efficient caching of case-insensitive strings
- Thread Safety: Depends on backing map implementation
- String Key Cache: Internal String key cache (≤ 100 characters by default) with API to change it

### Use Cases
- HTTP headers storage
- Configuration management
- Case-insensitive lookups
- Property maps
- Database column mapping
- XML/JSON attribute mapping
- File system operations

### Implementation Notes
- String keys are wrapped in CaseInsensitiveString internally
- Non-String keys are handled without modification
- Original String case is preserved
- Backing map type is preserved when copying from source
- Cache limit configurable via setMaxCacheLengthString()

### Thread Safety Notes
- Thread safety depends on backing map implementation
- Default implementation (LinkedHashMap) is not thread-safe
- Use ConcurrentHashMap or Collections.synchronizedMap() for thread safety
- Cache operations are thread-safe

---
## LRUCache
[Source](/src/main/java/com/cedarsoftware/util/LRUCache.java)

A thread-safe Least Recently Used (LRU) cache implementation that offers two distinct strategies for managing cache entries: Locking and Threaded.

### Key Features
- Two implementation strategies (Locking and Threaded)
- Thread-safe operations
- Configurable maximum capacity
- Supports null keys and values
- Full Map interface implementation
- Optional eviction listeners
- Automatic cleanup of expired entries

### Implementation Strategies

#### Locking Strategy
- Perfect size maintenance (never exceeds capacity)
- Non-blocking get() operations using try-lock
- O(1) access for get(), put(), and remove()
- Stringent LRU ordering (maintains strict LRU order in typical operations, with possible deviations under heavy concurrent access)
- Suitable for scenarios requiring exact capacity control

#### Threaded Strategy
- Near-perfect capacity maintenance
- No blocking operations
- O(1) access for all operations
- Background thread for cleanup
- May temporarily exceed capacity
- Excellent performance under high load (like ConcurrentHashMap)
- Suitable for scenarios prioritizing throughput

### Usage Examples

**Basic Usage (Locking Strategy):**
```java
// Create cache with capacity of 100
LRUCache<String, User> cache = new LRUCache<>(100);

// Add entries
cache.put("user1", new User("John"));
cache.put("user2", new User("Jane"));

// Retrieve entries
User user = cache.get("user1");
```

**Threaded Strategy with Custom Cleanup:**
```java
// Create cache with threaded strategy
LRUCache<String, User> cache = new LRUCache<>(
    1000,                          // capacity
    LRUCache.StrategyType.THREADED // strategy
);

// Or with custom cleanup delay
LRUCache<String, User> cache = new LRUCache<>(
    1000,    // capacity
    50       // cleanup delay in milliseconds
);
```

**With Eviction Listener (coming soon):**
```java
// Create cache with eviction notification
LRUCache<String, Session> sessionCache = new LRUCache<>(
    1000,
    (key, value) -> log.info("Session expired: " + key)
);
```

### Performance Characteristics

**Locking Strategy:**
- get(): O(1), non-blocking
- put(): O(1), requires lock
- remove(): O(1), requires lock
- Memory: Proportional to capacity
- Exact capacity maintenance

**Threaded Strategy:**
- get(): O(1), never blocks
- put(): O(1), never blocks
- remove(): O(1), never blocks
- Memory: May temporarily exceed capacity
- Background cleanup thread

### Use Cases

**Locking Strategy Ideal For:**
- Strict memory constraints
- Exact capacity requirements
- Lower throughput scenarios
- When temporary oversizing is unacceptable

**Threaded Strategy Ideal For:**
- High-throughput requirements
- When temporary oversizing is acceptable
- Reduced contention priority
- Better CPU utilization

### Implementation Notes
- Both strategies maintain approximate LRU ordering
- Threaded strategy uses shared cleanup thread
- Cleanup thread is daemon (won't prevent JVM shutdown)
- Supports proper shutdown in container environments
- Thread-safe null key/value handling

### Thread Safety Notes
- All operations are thread-safe
- Locking strategy uses ReentrantLock
- Threaded strategy uses ConcurrentHashMap
- Safe for concurrent access
- No external synchronization needed

### Shutdown Considerations
```java
// For threaded strategy, proper shutdown:
try {
    cache.shutdown();  // Cleans up background threads
} catch (Exception e) {
    // Handle shutdown failure
}
```

---
## TTLCache
[Source](/src/main/java/com/cedarsoftware/util/TTLCache.java)

A thread-safe cache implementation that automatically expires entries after a specified Time-To-Live (TTL) duration. Optionally supports Least Recently Used (LRU) eviction when a maximum size is specified.

### Key Features
- Automatic entry expiration based on TTL
- Optional maximum size limit with LRU eviction
- Thread-safe operations
- Supports null keys and values
- Background cleanup of expired entries
- Full Map interface implementation
- Efficient memory usage

### Usage Examples

**Basic TTL Cache:**
```java
// Create cache with 1-hour TTL
TTLCache<String, UserSession> cache = new TTLCache<>(
    TimeUnit.HOURS.toMillis(1)  // TTL of 1 hour
);

// Add entries
cache.put("session1", userSession);
```

**TTL Cache with Size Limit:**
```java
// Create cache with TTL and max size
TTLCache<String, UserSession> cache = new TTLCache<>(
    TimeUnit.MINUTES.toMillis(30),  // TTL of 30 minutes
    1000                            // Maximum 1000 entries
);
```

**Custom Cleanup Interval:**
```java
TTLCache<String, Document> cache = new TTLCache<>(
    TimeUnit.HOURS.toMillis(2),    // TTL of 2 hours
    500,                           // Maximum 500 entries
    TimeUnit.MINUTES.toMillis(5)   // Cleanup every 5 minutes
);
```

### Performance Characteristics
- get(): O(1)
- put(): O(1)
- remove(): O(1)
- containsKey(): O(1)
- containsValue(): O(n)
- Memory: Proportional to number of entries
- Background cleanup thread shared across instances

### Configuration Options
- Time-To-Live (TTL) duration
- Maximum cache size (optional)
- Cleanup interval (optional)
- Default cleanup interval: 60 seconds
- Minimum cleanup interval: 10 milliseconds

### Use Cases
- Session management
- Temporary data caching
- Rate limiting
- Token caching
- Resource pooling
- Temporary credential storage
- API response caching

### Implementation Notes
- Uses ConcurrentHashMapNullSafe for thread-safe storage
- Single background thread for all cache instances
- LRU tracking via doubly-linked list
- Weak references prevent memory leaks
- Automatic cleanup of expired entries
- Try-lock approach for LRU updates

### Thread Safety Notes
- All operations are thread-safe
- Background cleanup is non-blocking
- Safe for concurrent access
- No external synchronization needed
- Lock-free reads for better performance

### Cleanup Behavior
- Automatic removal of expired entries
- Background thread handles cleanup
- Cleanup interval is configurable
- Expired entries removed on access
- Size limit enforced on insertion

### Shutdown Considerations
```java
// Proper shutdown in container environments
try {
    TTLCache.shutdown();  // Stops background cleanup thread
} catch (Exception e) {
    // Handle shutdown failure
}
```
---
## TrackingMap
[Source](/src/main/java/com/cedarsoftware/util/TrackingMap.java)

A Map wrapper that tracks key access patterns, enabling monitoring and optimization of map usage. Tracks which keys have been accessed via `get()` or `containsKey()` methods, allowing for identification and removal of unused entries.

### Key Features
- Tracks key access patterns
- Supports removal of unused entries
- Wraps any Map implementation
- Full Map interface implementation
- Access pattern merging capability
- Maintains original map behavior
- Memory usage optimization support

### Usage Examples

**Basic Usage:**
```java
// Create a tracking map
Map<String, User> userMap = new HashMap<>();
TrackingMap<String, User> tracker = new TrackingMap<>(userMap);

// Access some entries
tracker.get("user1");
tracker.containsKey("user2");

// Remove unused entries
tracker.expungeUnused();  // Removes entries never accessed
```

**Usage Pattern Analysis:**
```java
TrackingMap<String, Config> configMap = new TrackingMap<>(sourceMap);

// After some time...
Set<String> usedKeys = configMap.keysUsed();
System.out.println("Accessed configs: " + usedKeys);
```

**Merging Usage Patterns:**
```java
// Multiple tracking maps
TrackingMap<String, Data> map1 = new TrackingMap<>(source1);
TrackingMap<String, Data> map2 = new TrackingMap<>(source2);

// Merge access patterns
map1.informAdditionalUsage(map2);
```

**Memory Optimization:**
```java
TrackingMap<String, Resource> resourceMap = 
    new TrackingMap<>(resources);

// Periodically clean unused resources
scheduler.scheduleAtFixedRate(() -> {
    resourceMap.expungeUnused();
}, 1, 1, TimeUnit.HOURS);
```

### Performance Characteristics
- get(): O(1) + tracking overhead
- put(): O(1)
- containsKey(): O(1) + tracking overhead
- expungeUnused(): O(n)
- Memory: Additional Set for tracking

### Use Cases
- Memory optimization
- Usage pattern analysis
- Resource cleanup
- Access monitoring
- Configuration optimization
- Cache efficiency improvement
- Dead code detection

### Implementation Notes
- Not thread-safe
- Wraps any Map implementation
- Maintains wrapped map's characteristics
- Tracks only get() and containsKey() calls
- put() operations are not tracked
- Supports null keys and values

### Access Tracking Details
- Tracks calls to get()
- Tracks calls to containsKey()
- Does not track put() operations
- Does not track containsValue()
- Access history survives remove operations
- Clear operation resets tracking

### Available Operations
```java
// Core tracking operations
Set<K> keysUsed()              // Get accessed keys
void expungeUnused()           // Remove unused entries

// Usage pattern merging
void informAdditionalUsage(Collection<K>)    // Merge from collection
void informAdditionalUsage(TrackingMap<K,V>) // Merge from another tracker

// Map access
Map<K,V> getWrappedMap()       // Get underlying map
```

### Thread Safety Notes
- Not thread-safe by default
- External synchronization required
- Wrap with Collections.synchronizedMap() if needed
- Consider concurrent access patterns
- Protect during expungeUnused()

---
## ConcurrentHashMapNullSafe
[Source](/src/main/java/com/cedarsoftware/util/ConcurrentHashMapNullSafe.java)

A thread-safe Map implementation that extends ConcurrentHashMap's capabilities by supporting null keys and values. Provides all the concurrency benefits of ConcurrentHashMap while allowing null entries.

### Key Features
- Full thread-safety and concurrent operation support
- Allows null keys and values
- High-performance concurrent operations
- Full Map and ConcurrentMap interface implementation
- Maintains ConcurrentHashMap's performance characteristics
- Configurable initial capacity and load factor
- Atomic operations support

### Usage Examples

**Basic Usage:**
```java
// Create a new map
ConcurrentMap<String, User> map = 
    new ConcurrentHashMapNullSafe<>();

// Support for null keys and values
map.put(null, new User("John"));
map.put("key", null);

// Regular operations
map.put("user1", new User("Alice"));
User user = map.get("user1");
```

**With Initial Capacity:**
```java
// Create with known size for better performance
ConcurrentMap<Integer, String> map = 
    new ConcurrentHashMapNullSafe<>(1000);

// Create with capacity and load factor
ConcurrentMap<Integer, String> map = 
    new ConcurrentHashMapNullSafe<>(1000, 0.75f);
```

**Atomic Operations:**
```java
ConcurrentMap<String, Integer> scores = 
    new ConcurrentHashMapNullSafe<>();

// Atomic operations with null support
scores.putIfAbsent("player1", null);
scores.replace("player1", null, 100);

// Compute operations
scores.computeIfAbsent("player2", k -> 0);
scores.compute("player1", (k, v) -> (v == null) ? 1 : v + 1);
```

**Bulk Operations:**
```java
// Create from existing map
Map<String, Integer> source = Map.of("A", 1, "B", 2);
ConcurrentMap<String, Integer> map = 
    new ConcurrentHashMapNullSafe<>(source);

// Merge operations
map.merge("A", 10, Integer::sum);
```

### Performance Characteristics
- get(): O(1) average case
- put(): O(1) average case
- remove(): O(1) average case
- containsKey(): O(1)
- size(): O(1)
- Concurrent read operations: Lock-free
- Write operations: Segmented locking
- Memory overhead: Minimal for null handling

### Thread Safety Features
- Atomic operations support
- Lock-free reads
- Segmented locking for writes
- Full happens-before guarantees
- Safe publication of changes
- Consistent iteration behavior

### Use Cases
- Concurrent caching
- Shared resource management
- Thread-safe data structures
- High-concurrency applications
- Null-tolerant collections
- Distributed systems
- Session management

### Implementation Notes
- Based on ConcurrentHashMap
- Uses sentinel objects for null handling
- Maintains thread-safety guarantees
- Preserves map contract
- Consistent serialization behavior
- Safe iterator implementation

### Atomic Operation Support
```java
// Atomic operations examples
map.putIfAbsent(key, value);     // Add if not present
map.replace(key, oldVal, newVal); // Atomic replace
map.remove(key, value);           // Conditional remove

// Compute operations
map.computeIfAbsent(key, k -> generator.get());
map.computeIfPresent(key, (k, v) -> processor.apply(v));
map.compute(key, (k, v) -> calculator.calculate(k, v));
```

---
## ConcurrentNavigableMapNullSafe
[Source](/src/main/java/com/cedarsoftware/util/ConcurrentNavigableMapNullSafe.java)

A thread-safe NavigableMap implementation that extends ConcurrentSkipListMap's capabilities by supporting null keys and values while maintaining sorted order. Provides all the navigation and concurrent benefits while allowing null entries.

### Key Features
- Full thread-safety and concurrent operation support
- Allows null keys and values
- Maintains sorted order with null handling
- Complete NavigableMap interface implementation
- Bidirectional navigation capabilities
- Range-view operations
- Customizable comparator support

### Usage Examples

**Basic Usage:**
```java
// Create with natural ordering
ConcurrentNavigableMap<String, Integer> map = 
    new ConcurrentNavigableMapNullSafe<>();

// Support for null keys and values
map.put(null, 100);    // Null keys are supported
map.put("B", null);    // Null values are supported
map.put("A", 1);

// Navigation operations
Integer first = map.firstEntry().getValue();  // Returns 1
Integer last = map.lastEntry().getValue();    // Returns 100 (null key)
```

**Custom Comparator:**
```java
// Create with custom ordering
Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
ConcurrentNavigableMap<String, Integer> map = 
    new ConcurrentNavigableMapNullSafe<>(comparator);

// Custom ordering is maintained
map.put("a", 1);
map.put("B", 2);
map.put(null, 3);
```

**Navigation Operations:**
```java
ConcurrentNavigableMap<Integer, String> map = 
    new ConcurrentNavigableMapNullSafe<>();

// Navigation methods
Map.Entry<Integer, String> lower = map.lowerEntry(5);
Map.Entry<Integer, String> floor = map.floorEntry(5);
Map.Entry<Integer, String> ceiling = map.ceilingEntry(5);
Map.Entry<Integer, String> higher = map.higherEntry(5);
```

**Range Views:**
```java
// Submap views
ConcurrentNavigableMap<String, Integer> subMap = 
    map.subMap("A", true, "C", false);

// Head/Tail views
ConcurrentNavigableMap<String, Integer> headMap = 
    map.headMap("B", true);
ConcurrentNavigableMap<String, Integer> tailMap = 
    map.tailMap("B", true);
```

### Performance Characteristics
- get(): O(log n)
- put(): O(log n)
- remove(): O(log n)
- containsKey(): O(log n)
- firstKey()/lastKey(): O(1)
- subMap operations: O(1)
- Memory overhead: Logarithmic

### Thread Safety Features
- Lock-free reads
- Lock-free writes
- Full concurrent operation support
- Consistent range view behavior
- Safe iteration guarantees
- Atomic navigation operations

### Use Cases
- Priority queues
- Sorted caches
- Range-based data structures
- Time-series data
- Event scheduling
- Version control
- Hierarchical data management

### Implementation Notes
- Based on ConcurrentSkipListMap
- Null sentinel handling
- Maintains total ordering
- Thread-safe navigation
- Consistent range views
- Preserves NavigableMap contract

### Navigation Operation Support
```java
// Navigation examples
K firstKey = map.firstKey();           // Smallest key
K lastKey = map.lastKey();             // Largest key
K lowerKey = map.lowerKey(key);        // Greatest less than
K floorKey = map.floorKey(key);        // Greatest less or equal
K ceilingKey = map.ceilingKey(key);    // Least greater or equal
K higherKey = map.higherKey(key);      // Least greater than

// Descending operations
NavigableSet<K> descKeys = map.descendingKeySet();
ConcurrentNavigableMap<K,V> descMap = map.descendingMap();
```

### Range View Operations
```java
// Range view examples
map.subMap(fromKey, fromInclusive, toKey, toInclusive);
map.headMap(toKey, inclusive);
map.tailMap(fromKey, inclusive);

// Polling operations
Map.Entry<K,V> first = map.pollFirstEntry();
Map.Entry<K,V> last = map.pollLastEntry();
```

---
## ConcurrentList
[Source](/src/main/java/com/cedarsoftware/util/ConcurrentList.java)

A thread-safe List implementation that provides synchronized access to list operations using read-write locks. Can be used either as a standalone thread-safe list or as a wrapper to make existing lists thread-safe.

### Key Features
- Full thread-safety with read-write lock implementation
- Standalone or wrapper mode operation
- Read-only snapshot iterators
- Non-blocking concurrent reads
- Exclusive write access
- Safe collection views
- Null element support (if backing list allows)

### Usage Examples

**Basic Usage:**
```java
// Create a new thread-safe list
List<String> list = new ConcurrentList<>();
list.add("item1");
list.add("item2");

// Create with initial capacity
List<String> list = new ConcurrentList<>(1000);

// Wrap existing list
List<String> existing = new ArrayList<>();
List<String> concurrent = new ConcurrentList<>(existing);
```

**Concurrent Operations:**
```java
ConcurrentList<User> users = new ConcurrentList<>();

// Safe concurrent access
users.add(new User("Alice"));
User first = users.get(0);

// Bulk operations
List<User> newUsers = Arrays.asList(
    new User("Bob"), 
    new User("Charlie")
);
users.addAll(newUsers);
```

**Thread-Safe Iteration:**
```java
ConcurrentList<String> list = new ConcurrentList<>();
list.addAll(Arrays.asList("A", "B", "C"));

// Safe iteration with snapshot view
for (String item : list) {
    System.out.println(item);
}

// List iterator (read-only)
ListIterator<String> iterator = list.listIterator();
while (iterator.hasNext()) {
    String item = iterator.next();
    // Process item
}
```

### Performance Characteristics
- Read operations: Non-blocking
- Write operations: Exclusive access
- Iterator creation: O(n) copy
- get(): O(1)
- add(): O(1) amortized
- remove(): O(n)
- contains(): O(n)
- size(): O(1)

### Thread Safety Features
- Read-write lock separation
- Safe concurrent reads
- Exclusive write access
- Snapshot iterators
- Thread-safe bulk operations
- Atomic modifications

### Use Cases
- Shared data structures
- Producer-consumer scenarios
- Multi-threaded caching
- Concurrent data collection
- Thread-safe logging
- Event handling
- Resource management

### Implementation Notes
- Uses ReentrantReadWriteLock
- Supports null elements
- No duplicate creation in wrapper mode
- Read-only iterator snapshots
- Unsupported operations:
    - listIterator(int)
    - subList(int, int)

### Operation Examples
```java
// Thread-safe operations
List<Integer> numbers = new ConcurrentList<>();

// Modification operations
numbers.add(1);                      // Single element add
numbers.addAll(Arrays.asList(2, 3)); // Bulk add
numbers.remove(1);                   // Remove by index
numbers.removeAll(Arrays.asList(2)); // Bulk remove

// Access operations
int first = numbers.get(0);             // Get by index
boolean contains = numbers.contains(1); // Check containment
int size = numbers.size();              // Get size
boolean empty = numbers.isEmpty();      // Check if empty

// Bulk operations
numbers.clear();                        // Remove all elements
numbers.retainAll(Arrays.asList(1, 2)); // Keep only specified
```

### Collection Views
```java
// Safe iteration examples
List<String> list = new ConcurrentList<>();

// Array conversion
Object[] array = list.toArray();
String[] strArray = list.toArray(new String[0]);

// Iterator usage
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    // Safe to process item
}

// List iterator
ListIterator<String> listIt = list.listIterator();
while (listIt.hasNext()) {
    // Forward iteration
}
```

---
## ArrayUtilities
[Source](/src/main/java/com/cedarsoftware/util/ArrayUtilities.java)

A utility class providing static methods for array operations, offering null-safe and type-safe array manipulations with support for common array operations and conversions.

### Key Features
- Immutable common array constants
- Null-safe array operations
- Generic array manipulation
- Collection to array conversion
- Array combining utilities
- Subset creation
- Shallow copy support

### Usage Examples

**Basic Operations:**
```java
// Check for empty arrays
boolean empty = ArrayUtilities.isEmpty(array);
int size = ArrayUtilities.size(array);

// Use common empty arrays
Object[] emptyObj = ArrayUtilities.EMPTY_OBJECT_ARRAY;
byte[] emptyBytes = ArrayUtilities.EMPTY_BYTE_ARRAY;
```

**Array Creation and Manipulation:**
```java
// Create typed arrays
String[] strings = ArrayUtilities.createArray("a", "b", "c");
Integer[] numbers = ArrayUtilities.createArray(1, 2, 3);

// Combine arrays
String[] array1 = {"a", "b"};
String[] array2 = {"c", "d"};
String[] combined = ArrayUtilities.addAll(array1, array2);
// Result: ["a", "b", "c", "d"]

// Remove items
Integer[] array = {1, 2, 3, 4};
Integer[] modified = ArrayUtilities.removeItem(array, 1);
// Result: [1, 3, 4]
```

**Array Subsetting:**
```java
// Create array subset
String[] full = {"a", "b", "c", "d", "e"};
String[] sub = ArrayUtilities.getArraySubset(full, 1, 4);
// Result: ["b", "c", "d"]
```

**Collection Conversion:**
```java
// Convert Collection to typed array
List<String> list = Arrays.asList("x", "y", "z");
String[] array = ArrayUtilities.toArray(String.class, list);

// Shallow copy
String[] original = {"a", "b", "c"};
String[] copy = ArrayUtilities.shallowCopy(original);
```

### Common Constants
```java
ArrayUtilities.EMPTY_OBJECT_ARRAY   // Empty Object[]
ArrayUtilities.EMPTY_BYTE_ARRAY     // Empty byte[]
ArrayUtilities.EMPTY_CHAR_ARRAY     // Empty char[]
ArrayUtilities.EMPTY_CHARACTER_ARRAY // Empty Character[]
ArrayUtilities.EMPTY_CLASS_ARRAY    // Empty Class<?>[]
```

### Performance Characteristics
- isEmpty(): O(1)
- size(): O(1)
- shallowCopy(): O(n)
- addAll(): O(n)
- removeItem(): O(n)
- getArraySubset(): O(n)
- toArray(): O(n)

### Implementation Notes
- Thread-safe (all methods are static)
- Null-safe operations
- Generic type support
- Uses System.arraycopy for efficiency
- Uses Arrays.copyOfRange for subsetting
- Direct array manipulation for collection conversion

### Best Practices
```java
// Use empty constants instead of creating new arrays
Object[] empty = ArrayUtilities.EMPTY_OBJECT_ARRAY;  // Preferred
Object[] empty2 = new Object[0];                     // Avoid

// Use type-safe array creation
String[] strings = ArrayUtilities.createArray("a", "b");  // Preferred
Object[] objects = new Object[]{"a", "b"};                // Avoid

// Null-safe checks
if (ArrayUtilities.isEmpty(array)) {  // Preferred
    // handle empty case
}
if (array == null || array.length == 0) {  // Avoid
    // handle empty case
}
```

### Limitations
- No deep copy support (see [json-io](http://github.com/jdereg/json-io)) 
- No multi-dimensional array specific operations (see [Converter](userguide.md#converter))

---
## ByteUtilities
[Source](/src/main/java/com/cedarsoftware/util/ByteUtilities.java)

A utility class providing static methods for byte array operations and hexadecimal string conversions. Offers thread-safe methods for encoding, decoding, and GZIP detection.

### Key Features
- Hex string to byte array conversion
- Byte array to hex string conversion
- GZIP compression detection
- Thread-safe operations
- Performance optimized
- Null-safe methods

### Usage Examples

**Hex Encoding and Decoding:**
```java
// Encode bytes to hex string
byte[] data = {0x1F, 0x8B, 0x3C};
String hex = ByteUtilities.encode(data);
// Result: "1F8B3C"

// Decode hex string to bytes
byte[] decoded = ByteUtilities.decode("1F8B3C");
// Result: {0x1F, 0x8B, 0x3C}
```

**GZIP Detection:**
```java
// Check if byte array is GZIP compressed
byte[] compressedData = {0x1f, 0x8b, /* ... */};
boolean isGzipped = ByteUtilities.isGzipped(compressedData);
// Result: true
```

**Error Handling:**
```java
// Invalid hex string (odd length)
byte[] result = ByteUtilities.decode("1F8");
// Result: null

// Valid hex string
byte[] valid = ByteUtilities.decode("1F8B");
// Result: {0x1F, 0x8B}
```

### Performance Characteristics
- encode(): O(n) with optimized StringBuilder
- decode(): O(n) with single-pass conversion
- isGzipped(): O(1) constant time
- Memory usage: Linear with input size
- No recursive operations

### Implementation Notes
- Uses pre-defined hex character array
- Optimized StringBuilder sizing
- Direct character-to-digit conversion
- No external dependencies
- Immutable hex character mapping

### Best Practices
```java
// Prefer direct byte array operations
byte[] bytes = {0x1F, 0x8B};
String hex = ByteUtilities.encode(bytes);

// Check for null on decode
byte[] decoded = ByteUtilities.decode(hexString);
if (decoded == null) {
    // Handle invalid hex string
}

// GZIP detection with null check
if (bytes != null && bytes.length >= 2 && ByteUtilities.isGzipped(bytes)) {
    // Handle GZIP compressed data
}
```

### Limitations
- decode() returns null for invalid input
- No partial array operations
- No streaming support
- Fixed hex format (uppercase)
- No binary string conversion
- No endianness handling

### Thread Safety
- All methods are static and thread-safe
- No shared state
- No synchronization required
- Safe for concurrent use
- No instance creation needed

### Use Cases
- Binary data serialization
- Hex string representation
- GZIP detection
- Data format conversion
- Debug logging
- Network protocol implementation
- File format handling

### Error Handling
```java
// Handle potential null result from decode
String hexString = "1F8";  // Invalid (odd length)
byte[] result = ByteUtilities.decode(hexString);
if (result == null) {
    // Handle invalid hex string
    throw new IllegalArgumentException("Invalid hex string");
}

// Ensures sufficient length and starting magic number for GZIP check
byte[] data = new byte[] { 0x1f, 0x8b, 0x44 };  // Too short (< 18)
boolean isGzip = ByteUtilities.isGzipped(data);
```

### Performance Tips
```java
// Efficient for large byte arrays
StringBuilder sb = new StringBuilder(bytes.length * 2);
String hex = ByteUtilities.encode(largeByteArray);

// Avoid repeated encoding/decoding
byte[] data = ByteUtilities.decode(hexString);
// Process data directly instead of converting back and forth
```

This implementation provides efficient and thread-safe operations for byte array manipulation and hex string conversion, with a focus on performance and reliability.

---
## ClassUtilities
[Source](/src/main/java/com/cedarsoftware/util/ClassUtilities.java)

A comprehensive utility class for Java class operations, providing methods for class manipulation, inheritance analysis, instantiation, and resource loading.

### Key Features
- Inheritance distance calculation
- Primitive type handling
- Class loading and instantiation
- Resource loading utilities
- Class alias management
- OSGi/JPMS support
- Constructor caching
- Unsafe instantiation support

### Usage Examples

**Class Analysis:**
```java
// Check inheritance distance
int distance = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
// Result: 1

// Check primitive types
boolean isPrim = ClassUtilities.isPrimitive(Integer.class);
// Result: true

// Check class properties
boolean isFinal = ClassUtilities.isClassFinal(String.class);
boolean privateConstructors = ClassUtilities.areAllConstructorsPrivate(Math.class);
```

**Class Loading and Instantiation:**
```java
// Load class by name
Class<?> clazz = ClassUtilities.forName("java.util.ArrayList", myClassLoader);

// Create new instance
List<Object> args = Arrays.asList("arg1", 42);
Object instance = ClassUtilities.newInstance(converter, MyClass.class, args);

// Convert primitive types
Class<?> wrapper = ClassUtilities.toPrimitiveWrapperClass(int.class);
// Result: Integer.class
```

**Resource Loading:**
```java
// Load resource as string
String content = ClassUtilities.loadResourceAsString("config.json");

// Load resource as bytes
byte[] data = ClassUtilities.loadResourceAsBytes("image.png");
```

**Class Alias Management:**
```java
// Add class alias
ClassUtilities.addPermanentClassAlias(ArrayList.class, "list");

// Remove class alias
ClassUtilities.removePermanentClassAlias("list");
```

### Performance Characteristics
- Constructor caching for improved instantiation
- Optimized class loading
- Efficient inheritance distance calculation
- Resource loading buffering
- ClassLoader caching for OSGi

### Implementation Notes
- Thread-safe operations
- Null-safe methods
- Security checks for instantiation
- OSGi environment detection
- JPMS compatibility
- Constructor accessibility handling

### Best Practices
```java
// Prefer cached constructors
Object obj = ClassUtilities.newInstance(converter, MyClass.class, args);

// Use appropriate ClassLoader
ClassLoader loader = ClassUtilities.getClassLoader(anchorClass);

// Handle primitive types properly
if (ClassUtilities.isPrimitive(clazz)) {
    clazz = ClassUtilities.toPrimitiveWrapperClass(clazz);
}
```

### Security Considerations
```java
// Restricted class instantiation
// These will throw IllegalArgumentException:
ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
ClassUtilities.newInstance(converter, ClassLoader.class, null);

// Safe resource loading
try {
    byte[] data = ClassUtilities.loadResourceAsBytes("config.json");
} catch (IllegalArgumentException e) {
    // Handle missing resource
}
```

### Advanced Features
```java
// Enable unsafe instantiation (use with caution)
ClassUtilities.setUseUnsafe(true);

// Find closest matching class
Map<Class<?>, Handler> handlers = new HashMap<>();
Handler handler = ClassUtilities.findClosest(targetClass, handlers, defaultHandler);

// Check enum relationship
Class<?> enumClass = ClassUtilities.getClassIfEnum(someClass);
```

### Common Use Cases
- Dynamic class loading
- Reflection utilities for dynamically obtaining classes, methods/constructors, fields, annotations
- Resource management
- Type conversion
- Class relationship analysis
- Constructor selection
- Instance creation
- ClassLoader management

This implementation provides a robust set of utilities for class manipulation and reflection operations, with emphasis on security, performance, and compatibility across different Java environments.

---
## Converter
[Source](/src/main/java/com/cedarsoftware/util/convert/Converter.java)

A powerful type conversion utility that supports conversion between various Java types, including primitives, collections, dates, and custom objects.

### Key Features
- Extensive built-in type conversions
- Collection and array conversions
- Null-safe operations
- Custom converter support
- Thread-safe design
- Inheritance-based conversion resolution
- Performance optimized with caching
- Static or Instance API

### Usage Examples

**Basic Conversions:**
```java
// Simple type conversions (using static com.cedarsoftware.util.Converter)
Long x = Converter.convert("35", Long.class);
Date d = Converter.convert("2015/01/01", Date.class);
int y = Converter.convert(45.0, int.class);
String dateStr = Converter.convert(date, String.class);

// Instance based conversion (using com.cedarsoftware.util.convert.Converter)
Converter converter = new Converter(new DefaultConverterOptions());
String str = converter.convert(42, String.class);
```

**Static versus Instance API:**
The static API is the easiest to use. It uses the default `ConverterOptions` object. Simply call
public static APIs on the `com.cedarsoftware.util.Converter` class.

The instance API allows you to create a `com.cedarsoftware.util.converter.Converter` instance with a custom `ConverterOptions` object.  If you add custom conversions, they will be used by the `Converter` instance.
You can create as many instances of the Converter as needed.  Often though, the static API is sufficient.

**Collection Conversions:**
```java
// Array to List
String[] array = {"a", "b", "c"};
List<String> list = converter.convert(array, List.class);

// List to Array
List<Integer> numbers = Arrays.asList(1, 2, 3);
Integer[] numArray = converter.convert(numbers, Integer[].class);

// EnumSet conversion
Object[] enumArray = {Day.MONDAY, "TUESDAY", 3};
EnumSet<Day> days = (EnumSet<Day>)(Object)converter.convert(enumArray, Day.class);
```

**Custom Conversions:**
```java
// Add custom converter
converter.addConversion(String.class, CustomType.class, 
    (from, conv) -> new CustomType(from));

// Use custom converter
CustomType obj = converter.convert("value", CustomType.class);
```

### Supported Conversions

**Primitive Types:**
```java
// Numeric conversions
Integer intVal = converter.convert("123", Integer.class);
Double doubleVal = converter.convert(42, Double.class);
BigDecimal decimal = converter.convert("123.45", BigDecimal.class);

// Boolean conversions
Boolean bool = converter.convert(1, Boolean.class);
boolean primitive = converter.convert("true", boolean.class);
```

**Date/Time Types:**
```java
// Date conversions
Date date = converter.convert("2023-01-01", Date.class);
LocalDateTime ldt = converter.convert(date, LocalDateTime.class);
ZonedDateTime zdt = converter.convert(instant, ZonedDateTime.class);
```

### Checking Conversion Support

```java
import java.util.concurrent.atomic.AtomicInteger;

// Check if conversion is supported
boolean canConvert = converter.isConversionSupportedFor(
        String.class, Integer.class);  // will look up inheritance chain

// Check direct conversion
boolean directSupport = converter.isDirectConversionSupported(
        String.class, Long.class);    // will not look up inheritance chain

// Check simple type conversion
boolean simpleConvert = converter.isSimpleTypeConversionSupported(
        String.class, Date.class);    // built-in JDK types (BigDecimal, Atomic*,

// Fetch supported conversions (as Strings)
Map<String, Set<String>> map = Converter.getSupportedConversions();

// Fetch supported conversions (as Classes)
Map<Class<?>, Set<Class<?>>> map = Converter.getSupportedConversions();
```

### Implementation Notes
- Thread-safe operations
- Caches conversion paths
- Handles primitive types automatically
- Supports inheritance-based resolution
- Optimized collection handling
- Null-safe conversions

### Best Practices
```java
// Prefer primitive wrappers for consistency
Integer value = converter.convert("123", Integer.class);

// Use appropriate collection types
List<String> list = converter.convert(array, ArrayList.class);

// Handle null values appropriately
Object nullVal = converter.convert(null, String.class); // Returns null

// Check conversion support before converting
if (converter.isConversionSupportedFor(sourceType, targetType)) {
    Object result = converter.convert(source, targetType);
}
```

### Performance Considerations
- Uses caching for conversion pairs (no instances created during convertion other than final converted item)
- Optimized collection handling (array to collection, colletion to array, n-dimensional arrays and nested collections, collection/array to EnumSets)
- Efficient type resolution: O(1) operation
- Minimal object creation
- Fast lookup for common conversions

This implementation provides a robust and extensible conversion framework with support for a wide range of Java types and custom conversions.

---
## DateUtilities
[Source](/src/main/java/com/cedarsoftware/util/DateUtilities.java)

A flexible date parsing utility that handles a wide variety of date and time formats, supporting multiple timezone specifications and optional components.

### Key Features
- Multiple date format support
- Flexible time components
- Timezone handling
- Thread-safe operation
- Null-safe parsing
- Unix epoch support
- Extensive timezone abbreviation mapping

### Supported Date Formats

**Numeric Formats:**
```java
// MM/DD/YYYY (with flexible separators: /, -, .)
DateUtilities.parseDate("12-31-2023");
DateUtilities.parseDate("12/31/2023");
DateUtilities.parseDate("12.31.2023");

// YYYY/MM/DD (with flexible separators: /, -, .)
DateUtilities.parseDate("2023-12-31");
DateUtilities.parseDate("2023/12/31");
DateUtilities.parseDate("2023.12.31");
```

**Text-Based Formats:**
```java
// Month Day, Year
DateUtilities.parseDate("January 6th, 2024");
DateUtilities.parseDate("Jan 6, 2024");

// Day Month Year
DateUtilities.parseDate("17th January 2024");
DateUtilities.parseDate("17 Jan 2024");

// Year Month Day
DateUtilities.parseDate("2024 January 31st");
DateUtilities.parseDate("2024 Jan 31");
```

**Unix Style:**
```java
// Full Unix format
DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024");
```

### Time Components

**Time Formats:**
```java
// Basic time
DateUtilities.parseDate("2024-01-15 13:30");

// With seconds
DateUtilities.parseDate("2024-01-15 13:30:45");

// With fractional seconds
DateUtilities.parseDate("2024-01-15 13:30:45.123456");

// With timezone offset
DateUtilities.parseDate("2024-01-15 13:30+01:00");
DateUtilities.parseDate("2024-01-15 13:30:45-0500");

// With named timezone
DateUtilities.parseDate("2024-01-15 13:30 EST");
DateUtilities.parseDate("2024-01-15 13:30:45 America/New_York");
```

### Timezone Support

**Offset Formats:**
```java
// GMT/UTC offset
DateUtilities.parseDate("2024-01-15 15:30+00:00");  // UTC
DateUtilities.parseDate("2024-01-15 10:30-05:00");  // EST
DateUtilities.parseDate("2024-01-15 20:30+05:00");  // IST
```

**Named Timezones:**
```java
// Using abbreviations
DateUtilities.parseDate("2024-01-15 15:30 GMT");
DateUtilities.parseDate("2024-01-15 10:30 EST");
DateUtilities.parseDate("2024-01-15 20:30 IST");

// Using full zone IDs
DateUtilities.parseDate("2024-01-15 15:30 Europe/London");
DateUtilities.parseDate("2024-01-15 10:30 America/New_York");
DateUtilities.parseDate("2024-01-15 20:30 Asia/Kolkata");
```

### Special Features

**Unix Epoch:**
```java
// Parse milliseconds since epoch
DateUtilities.parseDate("1640995200000");  // 2022-01-01 00:00:00 UTC
```

**Default Timezone Control:**
```java
// Parse with specific default timezone
ZonedDateTime date = DateUtilities.parseDate(
    "2024-01-15 14:30:00",
    ZoneId.of("America/New_York"),
    true
);
```

**Optional Components:**
```java
// Optional day of week (ignored in calculation)
DateUtilities.parseDate("Sunday 2024-01-15 14:30");
DateUtilities.parseDate("2024-01-15 14:30 Sunday");

// Flexible date/time separator
DateUtilities.parseDate("2024-01-15T14:30:00");
DateUtilities.parseDate("2024-01-15 14:30:00");
```

### Implementation Notes
- Thread-safe design
- Null-safe operations
- Extensive timezone abbreviation mapping
- Handles ambiguous timezone abbreviations
- Supports variable precision in fractional seconds
- Flexible separator handling
- Optional components support

### Best Practices
```java
// Specify timezone when possible
ZonedDateTime date = DateUtilities.parseDate(
    dateString,
    ZoneId.of("UTC"),
    true
);

// Use full zone IDs for unambiguous timezone handling
DateUtilities.parseDate("2024-01-15 14:30 America/New_York");

// Include seconds for precise time handling
DateUtilities.parseDate("2024-01-15 14:30:00");

// Use ISO format for machine-generated dates
DateUtilities.parseDate("2024-01-15T14:30:00Z");
```

### Error Handling
```java
try {
    Date date = DateUtilities.parseDate("invalid date");
} catch (IllegalArgumentException e) {
    // Handle invalid date format
}

// Null handling
Date date = DateUtilities.parseDate(null);  // Returns null
```

This utility provides robust date parsing capabilities with extensive format support and timezone handling, making it suitable for applications dealing with various date/time string representations.

---
## DeepEquals
[Source](/src/main/java/com/cedarsoftware/util/DeepEquals.java)

A sophisticated utility for performing deep equality comparisons between objects, supporting complex object graphs, collections, and providing detailed difference reporting.

### Key Features
- Deep object graph comparison
- Circular reference detection
- Detailed difference reporting
- Configurable precision for numeric comparisons
- Custom equals() method handling
- String-to-number comparison support
- Thread-safe implementation

### Usage Examples

**Basic Comparison:**
```java
// Simple comparison
boolean equal = DeepEquals.deepEquals(obj1, obj2);

// With options and difference reporting
Map<String, Object> options = new HashMap<>();
if (!DeepEquals.deepEquals(obj1, obj2, options)) {
    String diff = (String) options.get(DeepEquals.DIFF);
    System.out.println("Difference: " + diff);
}
```

**Custom Configuration:**
```java
// Ignore custom equals() for specific classes
Map<String, Object> options = new HashMap<>();
options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, 
    Set.of(MyClass.class, OtherClass.class));

// Allow string-to-number comparisons
options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
```

**Deep Hash Code Generation:**
```java
// Generate hash code for complex objects
int hash = DeepEquals.deepHashCode(complexObject);

// Use in custom hashCode() implementation
@Override
public int hashCode() {
    return DeepEquals.deepHashCode(this);
}
```

### Comparison Support

**Basic Types:**
```java
// Primitives and their wrappers
DeepEquals.deepEquals(10, 10);              // true
DeepEquals.deepEquals(10L, 10);             // true
DeepEquals.deepEquals(10.0, 10);            // true

// Strings and Characters
DeepEquals.deepEquals("test", "test");      // true
DeepEquals.deepEquals('a', 'a');            // true

// Dates and Times
DeepEquals.deepEquals(date1, date2);        // Compares timestamps
```

**Collections and Arrays:**
```java
// Arrays
DeepEquals.deepEquals(new int[]{1,2}, new int[]{1,2});

// Lists (order matters)
DeepEquals.deepEquals(Arrays.asList(1,2), Arrays.asList(1,2));

// Sets (order doesn't matter)
DeepEquals.deepEquals(new HashSet<>(list1), new HashSet<>(list2));

// Maps
DeepEquals.deepEquals(map1, map2);
```

### Implementation Notes
- Thread-safe design
- Efficient circular reference detection
- Precise floating-point comparison
- Detailed difference reporting
- Collection order awareness
- Map entry comparison support
- Array dimension validation

### Best Practices
```java
// Use options for custom behavior
Map<String, Object> options = new HashMap<>();
options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, customEqualsClasses);
options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

// Check differences
if (!DeepEquals.deepEquals(obj1, obj2, options)) {
    String diff = (String) options.get(DeepEquals.DIFF);
    // Handle difference
}

// Generate consistent hash codes
@Override
public int hashCode() {
    return DeepEquals.deepHashCode(this);
}
```

### Performance Considerations
- Caches reflection data
- Optimized collection comparison
- Efficient circular reference detection
- Smart difference reporting
- Minimal object creation
- Thread-local formatting

This implementation provides robust deep comparison capabilities with detailed difference reporting and configurable behavior.

---
## IOUtilities
[Source](/src/main/java/com/cedarsoftware/util/IOUtilities.java)

A comprehensive utility class for I/O operations, providing robust stream handling, compression, and resource management capabilities.

### Key Features
- Stream transfer operations
- Resource management (close/flush)
- Compression utilities
- URL connection handling
- Progress tracking
- XML stream support
- Buffer optimization

### Usage Examples

**Stream Transfer Operations:**
```java
// File to OutputStream
File sourceFile = new File("source.txt");
try (OutputStream fos = Files.newOutputStream(Paths.get("dest.txt"))) {
        IOUtilities.transfer(sourceFile, fos);
}

// InputStream to OutputStream with callback
IOUtilities.transfer(inputStream, outputStream, new TransferCallback() {
    public void bytesTransferred(byte[] bytes, int count) {
        // Track progress
    }
    public boolean isCancelled() {
        return false;  // Continue transfer
    }
});
```

**Compression Operations:**
```java
// Compress byte array
byte[] original = "Test data".getBytes();
byte[] compressed = IOUtilities.compressBytes(original);

// Uncompress byte array
byte[] uncompressed = IOUtilities.uncompressBytes(compressed);

// Stream compression
ByteArrayOutputStream original = new ByteArrayOutputStream();
ByteArrayOutputStream compressed = new ByteArrayOutputStream();
IOUtilities.compressBytes(original, compressed);
```

**URL Connection Handling:**
```java
// Get input stream with automatic encoding detection
URLConnection conn = url.openConnection();
try (InputStream is = IOUtilities.getInputStream(conn)) {
    // Use input stream
}

// Upload file to URL
File uploadFile = new File("upload.dat");
URLConnection conn = url.openConnection();
IOUtilities.transfer(uploadFile, conn, callback);
```

### Resource Management

**Closing Resources:**
```java
// Close Closeable resources
IOUtilities.close(inputStream);
IOUtilities.close(outputStream);

// Close XML resources
IOUtilities.close(xmlStreamReader);
IOUtilities.close(xmlStreamWriter);
```

**Flushing Resources:**
```java
// Flush Flushable resources
IOUtilities.flush(outputStream);
IOUtilities.flush(writer);

// Flush XML writer
IOUtilities.flush(xmlStreamWriter);
```

### Stream Conversion

**Byte Array Operations:**
```java
// Convert InputStream to byte array
byte[] bytes = IOUtilities.inputStreamToBytes(inputStream);

// Transfer exact number of bytes
byte[] buffer = new byte[1024];
IOUtilities.transfer(inputStream, buffer);
```

### Implementation Notes
- Uses 32KB buffer size for transfers
- Supports GZIP and Deflate compression
- Silent exception handling for close/flush
- Thread-safe implementation
- Automatic resource management
- Progress tracking support

### Best Practices
```java
// Use try-with-resources when possible
try (InputStream in = Files.newInputStream(file.toPath())) {
    try (OutputStream out = Files.newOutputStream(dest.toPath())) {
        IOUtilities.transfer(in, out);
    }
}

// Note: try-with-resources handles closing automatically
// The following is unnecessary when using try-with-resources:
// finally {
//     IOUtilities.close(inputStream);
//     IOUtilities.close(outputStream);
// }

// Use callbacks for large transfers
IOUtilities.transfer(source, dest, new TransferCallback() {
    public void bytesTransferred(byte[] bytes, int count) {
        updateProgress(count);
    }
    public boolean isCancelled() {
        return userCancelled;
    }
});
```

### Performance Considerations
- Optimized buffer size (32KB)
- Buffered streams for efficiency
- Minimal object creation
- Memory-efficient transfers
- Streaming compression support
- Progress monitoring capability

This implementation provides a robust set of I/O utilities with emphasis on resource safety, performance, and ease of use.

---
## EncryptionUtilities
[Source](/src/main/java/com/cedarsoftware/util/EncryptionUtilities.java)

A comprehensive utility class providing cryptographic operations including high-performance hashing, encryption, and decryption capabilities.

### Key Features
- Optimized file hashing (MD5, SHA-1, SHA-256, SHA-512)
- AES-128 encryption/decryption
- Zero-copy I/O operations
- Thread-safe implementation
- Custom filesystem support
- Efficient memory usage

### Hash Operations

**File Hashing:**
```java
// High-performance file hashing
String md5 = EncryptionUtilities.fastMD5(new File("large.dat"));
String sha1 = EncryptionUtilities.fastSHA1(new File("large.dat"));
String sha256 = EncryptionUtilities.fastSHA256(new File("large.dat"));
String sha512 = EncryptionUtilities.fastSHA512(new File("large.dat"));
```

**Byte Array Hashing:**
```java
// Hash byte arrays
String md5Hash = EncryptionUtilities.calculateMD5Hash(bytes);
String sha1Hash = EncryptionUtilities.calculateSHA1Hash(bytes);
String sha256Hash = EncryptionUtilities.calculateSHA256Hash(bytes);
String sha512Hash = EncryptionUtilities.calculateSHA512Hash(bytes);
```

### Encryption Operations

**String Encryption:**
```java
// Encrypt/decrypt strings
String encrypted = EncryptionUtilities.encrypt("password", "sensitive data");
String decrypted = EncryptionUtilities.decrypt("password", encrypted);
```

**Byte Array Encryption:**
```java
// Encrypt/decrypt byte arrays
String encryptedHex = EncryptionUtilities.encryptBytes("password", originalBytes);
byte[] decryptedBytes = EncryptionUtilities.decryptBytes("password", encryptedHex);
```

### Custom Cipher Creation

**AES Cipher Configuration:**
```java
// Create encryption cipher
Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher("password");

// Create decryption cipher
Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher("password");

// Create custom mode cipher
Cipher customCipher = EncryptionUtilities.createAesCipher("password", Cipher.ENCRYPT_MODE);
```

### Implementation Notes

**Performance Features:**
- 64KB buffer size for optimal I/O
- DirectByteBuffer for zero-copy operations
- Efficient memory management
- Optimized for modern storage systems

**Security Features:**
- CBC mode with PKCS5 padding
- IV generation from key using MD5
- Standard JDK security providers
- Thread-safe operations

### Best Practices

**Hashing:**
```java
// Prefer SHA-256 or SHA-512 for security
String secureHash = EncryptionUtilities.fastSHA256(file);

// MD5/SHA-1 for legacy or non-security uses only
String legacyHash = EncryptionUtilities.fastMD5(file);
```

**Encryption:**
```java
// Use strong passwords
String strongKey = "complex-password-here";
String encrypted = EncryptionUtilities.encrypt(strongKey, data);

// Handle exceptions appropriately
try {
    Cipher cipher = EncryptionUtilities.createAesEncryptionCipher(key);
} catch (Exception e) {
    // Handle cipher creation failure
}
```

### Performance Considerations
- Uses optimal buffer sizes (64KB)
- Minimizes memory allocation
- Efficient I/O operations
- Zero-copy where possible

### Security Notes
```java
// MD5 and SHA-1 are cryptographically broken
// Use only for checksums or legacy compatibility
String checksum = EncryptionUtilities.fastMD5(file);

// For security, use SHA-256 or SHA-512
String secure = EncryptionUtilities.fastSHA256(file);

// AES implementation details
// - Uses CBC mode with PKCS5 padding
// - IV is derived from key using MD5
// - 128-bit key size
Cipher cipher = EncryptionUtilities.createAesEncryptionCipher(key);
```

### Resource Management
```java
// Resources are automatically managed
try (InputStream in = Files.newInputStream(file.toPath())) {
    // Hash calculation handles cleanup
    String hash = EncryptionUtilities.fastSHA256(file);
}

// DirectByteBuffer is managed internally
String hash = EncryptionUtilities.calculateFileHash(channel, digest);
```

This implementation provides a robust set of cryptographic utilities with emphasis on performance, security, and ease of use.

---
## Executor
[Source](/src/main/java/com/cedarsoftware/util/Executor.java)

A utility class for executing system commands and capturing their output. Provides a convenient wrapper around Java's Runtime.exec() with automatic stream handling and output capture.

### Key Features
- Command execution with various parameter options
- Automatic stdout/stderr capture
- Non-blocking output handling
- Environment variable support
- Working directory specification
- Stream management

### Basic Usage

**Simple Command Execution:**
```java
Executor exec = new Executor();

// Execute simple command
int exitCode = exec.exec("ls -l");
String output = exec.getOut();
String errors = exec.getError();

// Execute with command array (better argument handling)
String[] cmd = {"git", "status", "--porcelain"};
exitCode = exec.exec(cmd);
```

**Environment Variables:**
```java
// Set custom environment variables
String[] env = {"PATH=/usr/local/bin:/usr/bin", "JAVA_HOME=/usr/java"};
int exitCode = exec.exec("mvn clean install", env);

// With command array
String[] cmd = {"python", "script.py"};
exitCode = exec.exec(cmd, env);
```

**Working Directory:**
```java
// Execute in specific directory
File workDir = new File("/path/to/work");
int exitCode = exec.exec("make", null, workDir);

// With command array and environment
String[] cmd = {"npm", "install"};
String[] env = {"NODE_ENV=production"};
exitCode = exec.exec(cmd, env, workDir);
```

### Output Handling

**Accessing Command Output:**
```java
Executor exec = new Executor();
exec.exec("git log -1");

// Get command output
String stdout = exec.getOut();  // Standard output
String stderr = exec.getError(); // Standard error

// Check for success
if (stdout != null && stderr.isEmpty()) {
    // Command succeeded
}
```

### Implementation Notes

**Exit Codes:**
- 0: Typically indicates success
- -1: Process start failure
- Other: Command-specific error codes

**Stream Management:**
- Non-blocking output handling
- Automatic stream cleanup
- Thread-safe output capture

### Best Practices

**Command Arrays vs Strings:**
```java
// Better - uses command array
String[] cmd = {"git", "clone", "https://github.com/user/repo.git"};
exec.exec(cmd);

// Avoid - shell interpretation issues
exec.exec("git clone https://github.com/user/repo.git");
```

**Error Handling:**
```java
Executor exec = new Executor();
int exitCode = exec.exec(command);

if (exitCode != 0) {
    String error = exec.getError();
    System.err.println("Command failed: " + error);
}
```

**Working Directory:**
```java
// Specify absolute paths when possible
File workDir = new File("/absolute/path/to/dir");

// Use relative paths carefully
File relativeDir = new File("relative/path");
```

### Performance Considerations
- Uses separate threads for stdout/stderr
- Non-blocking output capture
- Efficient stream buffering
- Automatic resource cleanup

### Security Notes
```java
// Avoid shell injection - use command arrays
String userInput = "malicious; rm -rf /";
String[] cmd = {"echo", userInput};  // Safe
exec.exec(cmd);

// Don't use string concatenation
exec.exec("echo " + userInput);  // Unsafe
```

### Resource Management
```java
// Resources are automatically managed
Executor exec = new Executor();
exec.exec(command);
// Streams and processes are cleaned up automatically

// Each exec() call is independent
exec.exec(command1);
String output1 = exec.getOut();
exec.exec(command2);
String output2 = exec.getOut();
```

This implementation provides a robust and convenient way to execute system commands while properly handling streams, environment variables, and working directories.

---
## GraphComparator
[Source](/src/main/java/com/cedarsoftware/util/GraphComparator.java)

A powerful utility for comparing object graphs and generating delta commands to transform one graph into another.

### Key Features
- Deep graph comparison
- Delta command generation
- Cyclic reference handling
- Collection support (Lists, Sets, Maps)
- Array comparison
- ID-based object tracking
- Delta application support

### Usage Examples

**Basic Graph Comparison:**
```java
// Define ID fetcher
GraphComparator.ID idFetcher = obj -> {
    if (obj instanceof MyClass) {
        return ((MyClass)obj).getId();
    }
    throw new IllegalArgumentException("Not an ID object");
};

// Compare graphs
List<Delta> deltas = GraphComparator.compare(sourceGraph, targetGraph, idFetcher);

// Apply deltas
DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
List<DeltaError> errors = GraphComparator.applyDelta(sourceGraph, deltas, idFetcher, processor);
```

**Custom Delta Processing:**
```java
DeltaProcessor customProcessor = new DeltaProcessor() {
    public void processArraySetElement(Object source, Field field, Delta delta) {
        // Custom array element handling
    }
    // Implement other methods...
};

GraphComparator.applyDelta(source, deltas, idFetcher, customProcessor);
```

### Delta Commands

**Object Operations:**
```java
// Field assignment
OBJECT_ASSIGN_FIELD      // Change field value
OBJECT_FIELD_TYPE_CHANGED // Field type changed
OBJECT_ORPHAN            // Object no longer referenced

// Array Operations
ARRAY_SET_ELEMENT       // Set array element
ARRAY_RESIZE           // Resize array

// Collection Operations
LIST_SET_ELEMENT       // Set list element
LIST_RESIZE           // Resize list
SET_ADD              // Add to set
SET_REMOVE          // Remove from set
MAP_PUT             // Put map entry
MAP_REMOVE          // Remove map entry
```

### Implementation Notes

**ID Handling:**
```java
// ID fetcher implementation
GraphComparator.ID idFetcher = obj -> {
    if (obj instanceof Entity) {
        return ((Entity)obj).getId();
    }
    if (obj instanceof Document) {
        return ((Document)obj).getDocId();
    }
    throw new IllegalArgumentException("Not an ID object");
};
```

**Delta Processing:**
```java
// Process specific delta types
switch (delta.getCmd()) {
    case ARRAY_SET_ELEMENT:
        // Handle array element change
        break;
    case MAP_PUT:
        // Handle map entry addition
        break;
    case OBJECT_ASSIGN_FIELD:
        // Handle field assignment
        break;
}
```

### Best Practices

**ID Fetcher:**
```java
// Robust ID fetcher
GraphComparator.ID idFetcher = obj -> {
    if (obj == null) throw new IllegalArgumentException("Null object");
    
    if (obj instanceof Identifiable) {
        return ((Identifiable)obj).getId();
    }
    
    throw new IllegalArgumentException(
        "Not an ID object: " + obj.getClass().getName());
};
```

**Error Handling:**
```java
List<DeltaError> errors = GraphComparator.applyDelta(
    source, deltas, idFetcher, processor, true); // failFast=true

if (!errors.isEmpty()) {
    for (DeltaError error : errors) {
        log.error("Delta error: {} for {}", 
            error.getError(), error.getCmd());
    }
}
```

### Performance Considerations
- Uses identity hash maps for cycle detection
- Efficient collection comparison
- Minimal object creation
- Smart delta generation
- Optimized graph traversal

### Limitations
- Objects must have unique IDs
- Collections must be standard JDK types
- Arrays must be single-dimensional
- No support for concurrent modifications
- Field access must be possible

This implementation provides robust graph comparison and transformation capabilities with detailed control over the delta application process.

---
## MathUtilities
[Source](/src/main/java/com/cedarsoftware/util/MathUtilities.java)

A utility class providing enhanced mathematical operations, numeric type handling, and algorithmic functions.

### Key Features
- Min/Max calculations for multiple numeric types
- Smart numeric parsing
- Permutation generation
- Constant definitions
- Thread-safe operations

### Numeric Constants
```java
// Useful BigInteger/BigDecimal constants
BIG_INT_LONG_MIN  // BigInteger.valueOf(Long.MIN_VALUE)
BIG_INT_LONG_MAX  // BigInteger.valueOf(Long.MAX_VALUE)
BIG_DEC_DOUBLE_MIN // BigDecimal.valueOf(-Double.MAX_VALUE)
BIG_DEC_DOUBLE_MAX // BigDecimal.valueOf(Double.MAX_VALUE)
```

### Minimum/Maximum Operations

**Primitive Types:**
```java
// Long operations
long min = MathUtilities.minimum(1L, 2L, 3L);  // Returns 1
long max = MathUtilities.maximum(1L, 2L, 3L);  // Returns 3

// Double operations
double minD = MathUtilities.minimum(1.0, 2.0, 3.0);  // Returns 1.0
double maxD = MathUtilities.maximum(1.0, 2.0, 3.0);  // Returns 3.0
```

**Big Number Types:**
```java
// BigInteger operations
BigInteger minBi = MathUtilities.minimum(
    BigInteger.ONE, 
    BigInteger.TEN
);

BigInteger maxBi = MathUtilities.maximum(
    BigInteger.ONE, 
    BigInteger.TEN
);

// BigDecimal operations
BigDecimal minBd = MathUtilities.minimum(
    BigDecimal.ONE, 
    BigDecimal.TEN
);

BigDecimal maxBd = MathUtilities.maximum(
    BigDecimal.ONE, 
    BigDecimal.TEN
);
```

### Smart Numeric Parsing

**Minimal Type Selection:**
```java
// Integer values within Long range
Number n1 = MathUtilities.parseToMinimalNumericType("123");
// Returns Long(123)

// Decimal values within Double precision
Number n2 = MathUtilities.parseToMinimalNumericType("123.45");
// Returns Double(123.45)

// Large integers
Number n3 = MathUtilities.parseToMinimalNumericType("999999999999999999999");
// Returns BigInteger

// High precision decimals
Number n4 = MathUtilities.parseToMinimalNumericType("1.23456789012345678901");
// Returns BigDecimal
```

### Permutation Generation

**Generate All Permutations:**
```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

// Print all permutations
do {
    System.out.println(list);
} while (MathUtilities.nextPermutation(list));

// Output:
// [1, 2, 3]
// [1, 3, 2]
// [2, 1, 3]
// [2, 3, 1]
// [3, 1, 2]
// [3, 2, 1]
```

### Implementation Notes

**Null Handling:**
```java
// BigInteger/BigDecimal methods throw IllegalArgumentException for null values
try {
    MathUtilities.minimum((BigInteger)null);
} catch (IllegalArgumentException e) {
    // Handle null input
}

// Primitive arrays cannot contain nulls
MathUtilities.minimum(1L, 2L, 3L);  // Always safe
```

**Type Selection Rules:**
```java
// Integer values
"123"       → Long
"999...999" → BigInteger (if > Long.MAX_VALUE)

// Decimal values
"123.45"    → Double
"1e308"     → BigDecimal (if > Double.MAX_VALUE)
"1.234...5" → BigDecimal (if needs more precision)
```

### Best Practices

**Efficient Min/Max:**
```java
// Use varargs for multiple values
long min = MathUtilities.minimum(val1, val2, val3);

// Use appropriate type
BigDecimal precise = MathUtilities.minimum(bd1, bd2, bd3);
```

**Smart Parsing:**
```java
// Let the utility choose the best type
Number n = MathUtilities.parseToMinimalNumericType(numericString);

// Check the actual type if needed
if (n instanceof Long) {
    // Handle integer case
} else if (n instanceof Double) {
    // Handle decimal case
} else if (n instanceof BigInteger) {
    // Handle large integer case
} else if (n instanceof BigDecimal) {
    // Handle high precision decimal case
}
```

### Performance Considerations
- Efficient implementation of min/max operations
- Smart type selection to minimize memory usage
- No unnecessary object creation
- Thread-safe operations
- Optimized permutation generation

This implementation provides a robust set of mathematical utilities with emphasis on type safety, precision, and efficiency.

---
## ReflectionUtils
[Source](/src/main/java/com/cedarsoftware/util/ReflectionUtils.java)

A high-performance reflection utility providing cached access to fields, methods, constructors, and annotations with sophisticated filtering capabilities.

### Key Features
- Cached reflection operations
- Field and method access
- Annotation discovery
- Constructor handling
- Class bytecode analysis
- Thread-safe implementation

### Cache Management

**Custom Cache Configuration (optional - use if you want to use your own cache):**
```java
// Configure custom caches
Map<Object, Method> methodCache = new ConcurrentHashMap<>();
ReflectionUtils.setMethodCache(methodCache);

Map<Object, Field> fieldCache = new ConcurrentHashMap<>();
ReflectionUtils.setFieldCache(fieldCache);

Map<Object, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
ReflectionUtils.setConstructorCache(constructorCache);
```

### Field Operations

**Field Access:**
```java
// Get single field
Field field = ReflectionUtils.getField(MyClass.class, "fieldName");

// Get all fields (including inherited)
List<Field> allFields = ReflectionUtils.getAllDeclaredFields(MyClass.class);

// Get fields with custom filter
List<Field> filteredFields = ReflectionUtils.getAllDeclaredFields(
    MyClass.class,
    field -> !Modifier.isStatic(field.getModifiers())
);

// Get fields as map
Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(MyClass.class);
```

### Method Operations

**Method Access:**
```java
// Get method by name and parameter types
Method method = ReflectionUtils.getMethod(
    MyClass.class, 
    "methodName", 
    String.class, 
    int.class
);

// Get non-overloaded method
Method simple = ReflectionUtils.getNonOverloadedMethod(
    MyClass.class, 
    "uniqueMethod"
);

// Method invocation
Object result = ReflectionUtils.call(instance, method, arg1, arg2);
Object result2 = ReflectionUtils.call(instance, "methodName", arg1, arg2);
```

### Annotation Operations

**Annotation Discovery:**
```java
// Get class annotation
MyAnnotation anno = ReflectionUtils.getClassAnnotation(
    MyClass.class, 
    MyAnnotation.class
);

// Get method annotation
MyAnnotation methodAnno = ReflectionUtils.getMethodAnnotation(
    method, 
    MyAnnotation.class
);
```

### Constructor Operations

**Constructor Access:**
```java
// Get constructor
Constructor<?> ctor = ReflectionUtils.getConstructor(
    MyClass.class, 
    String.class, 
    int.class
);
```

### Implementation Notes

**Caching Strategy:**
```java
// All operations use internal caching
private static final int CACHE_SIZE = 1000;
private static final Map<MethodCacheKey, Method> METHOD_CACHE = 
    new LRUCache<>(CACHE_SIZE);
private static final Map<FieldsCacheKey, Collection<Field>> FIELDS_CACHE = 
    new LRUCache<>(CACHE_SIZE);
```

**Thread Safety:**
```java
// All caches are thread-safe
private static volatile Map<ConstructorCacheKey, Constructor<?>> CONSTRUCTOR_CACHE;
private static volatile Map<MethodCacheKey, Method> METHOD_CACHE;
```

### Best Practices

**Field Access:**
```java
// Prefer getAllDeclaredFields for complete hierarchy
List<Field> fields = ReflectionUtils.getAllDeclaredFields(clazz);

// Use field map for repeated lookups
Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(clazz);
```

**Method Access:**
```java
// Cache method lookups at class level
private static final Method method = ReflectionUtils.getMethod(
    MyClass.class, 
    "process"
);

// Use call() for simplified invocation
Object result = ReflectionUtils.call(instance, method, args);
```

### Performance Considerations
- All reflection operations are cached
- Thread-safe implementation
- Optimized for repeated access
- Minimal object creation
- Efficient cache key generation
- Smart cache eviction

### Security Notes
```java
// Handles security restrictions gracefully
try {
    field.setAccessible(true);
} catch (SecurityException ignored) {
    // Continue with restricted access
}

// Respects security manager
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Handle security checks
}
```

This implementation provides high-performance reflection utilities with sophisticated caching and comprehensive access to Java's reflection capabilities.

---
## StringUtilities
[Source](/src/main/java/com/cedarsoftware/util/StringUtilities.java)

A comprehensive utility class providing enhanced string manipulation, comparison, and conversion operations with null-safe implementations.

### Key Features
- String comparison (case-sensitive and insensitive)
- Whitespace handling
- String trimming operations
- Distance calculations (Levenshtein and Damerau-Levenshtein)
- Encoding conversions
- Random string generation
- Hex encoding/decoding

### Basic Operations

**String Comparison:**
```java
// Case-sensitive comparison
boolean equals = StringUtilities.equals("text", "text");      // true
boolean equals = StringUtilities.equals("Text", "text");      // false

// Case-insensitive comparison
boolean equals = StringUtilities.equalsIgnoreCase("Text", "text");  // true

// Comparison with trimming
boolean equals = StringUtilities.equalsWithTrim(" text ", "text");  // true
boolean equals = StringUtilities.equalsIgnoreCaseWithTrim(" Text ", "text");  // true
```

**Whitespace Handling:**
```java
// Check for empty or whitespace
boolean empty = StringUtilities.isEmpty("   ");        // true
boolean empty = StringUtilities.isEmpty(null);         // true
boolean empty = StringUtilities.isEmpty("  text  ");   // false

// Check for content
boolean hasContent = StringUtilities.hasContent("text");  // true
boolean hasContent = StringUtilities.hasContent("   ");   // false
```

**String Trimming:**
```java
// Basic trim operations
String result = StringUtilities.trim("  text  ");        // "text"
String result = StringUtilities.trimToEmpty(null);       // ""
String result = StringUtilities.trimToNull("  ");        // null
String result = StringUtilities.trimEmptyToDefault(
    "  ", "default");  // "default"
```

### Advanced Features

**Distance Calculations:**
```java
// Levenshtein distance
int distance = StringUtilities.levenshteinDistance("kitten", "sitting");  // 3

// Damerau-Levenshtein distance (handles transpositions)
int distance = StringUtilities.damerauLevenshteinDistance("book", "back");  // 2
```

**Encoding Operations:**
```java
// UTF-8 operations
byte[] bytes = StringUtilities.getUTF8Bytes("text");
String text = StringUtilities.createUTF8String(bytes);

// Custom encoding
byte[] bytes = StringUtilities.getBytes("text", "ISO-8859-1");
String text = StringUtilities.createString(bytes, "ISO-8859-1");
```

**Random String Generation:**
```java
Random random = new Random();
// Generate random string (proper case)
String random = StringUtilities.getRandomString(random, 5, 10);  // "Abcdef"

// Generate random character
String char = StringUtilities.getRandomChar(random, true);  // Uppercase
String char = StringUtilities.getRandomChar(random, false); // Lowercase
```

### String Manipulation

**Quote Handling:**
```java
// Remove quotes
String result = StringUtilities.removeLeadingAndTrailingQuotes("\"text\"");  // "text"
String result = StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\"");  // "text"
```

**Set Conversion:**
```java
// Convert comma-separated string to Set
Set<String> set = StringUtilities.commaSeparatedStringToSet("a,b,c");
// Result: ["a", "b", "c"]
```

### Implementation Notes

**Performance Features:**
```java
// Efficient case-insensitive hash code
int hash = StringUtilities.hashCodeIgnoreCase("Text");

// Optimized string counting
int count = StringUtilities.count("text", 't');
int count = StringUtilities.count("text text", "text");
```

**Pattern Conversion:**
```java
// Convert * and ? wildcards to regex
String regex = StringUtilities.wildcardToRegexString("*.txt");
// Result: "^.*\.txt$"
```

### Best Practices

**Null Handling:**
```java
// Use null-safe methods
String result = StringUtilities.trimToEmpty(nullString);    // Returns ""
String result = StringUtilities.trimToNull(emptyString);    // Returns null
String result = StringUtilities.trimEmptyToDefault(
    nullString, "default");  // Returns "default"
```

**Length Calculations:**
```java
// Safe length calculations
int len = StringUtilities.length(nullString);      // Returns 0
int len = StringUtilities.trimLength(nullString);  // Returns 0
```

### Constants
```java
StringUtilities.EMPTY           // Empty string ""
StringUtilities.FOLDER_SEPARATOR // Forward slash "/"
```

This implementation provides robust string manipulation capabilities with emphasis on null safety, performance, and convenience.

---
## SystemUtilities
[Source](/src/main/java/com/cedarsoftware/util/SystemUtilities.java)

A comprehensive utility class providing system-level operations and information gathering capabilities with a focus on platform independence.

### Key Features
- Environment and property access
- Memory monitoring
- Network interface information
- Process management
- Runtime environment analysis
- Temporary file handling

### System Constants

**Common System Properties:**
```java
SystemUtilities.OS_NAME       // Operating system name
SystemUtilities.JAVA_VERSION  // Java version
SystemUtilities.USER_HOME     // User home directory
SystemUtilities.TEMP_DIR      // Temporary directory
```

### Environment Operations

**Variable Access:**
```java
// Get environment variable with system property fallback
String value = SystemUtilities.getExternalVariable("CONFIG_PATH");

// Get filtered environment variables
Map<String, String> vars = SystemUtilities.getEnvironmentVariables(
    key -> key.startsWith("JAVA_")
);
```

### System Resources

**Processor and Memory:**
```java
// Get available processors
int processors = SystemUtilities.getAvailableProcessors();

// Memory information
MemoryInfo memory = SystemUtilities.getMemoryInfo();
long total = memory.getTotalMemory();
long free = memory.getFreeMemory();
long max = memory.getMaxMemory();

// Check memory availability
boolean hasMemory = SystemUtilities.hasAvailableMemory(1024 * 1024 * 100);

// System load
double load = SystemUtilities.getSystemLoadAverage();
```

### Network Operations

**Interface Information:**
```java
// Get network interfaces
List<NetworkInfo> interfaces = SystemUtilities.getNetworkInterfaces();
for (NetworkInfo ni : interfaces) {
    String name = ni.getName();
    String display = ni.getDisplayName();
    List<InetAddress> addresses = ni.getAddresses();
    boolean isLoopback = ni.isLoopback();
}
```

### Process Management

**Process Information:**
```java
// Get current process ID
long pid = SystemUtilities.getCurrentProcessId();

// Add shutdown hook
SystemUtilities.addShutdownHook(() -> {
    // Cleanup code
});
```

### File Operations

**Temporary Files:**
```java
// Create temp directory
File tempDir = SystemUtilities.createTempDirectory("prefix-");
// Directory will be deleted on JVM exit
```

### Version Management

**Java Version Checking:**
```java
// Check Java version
boolean isJava11OrHigher = SystemUtilities.isJavaVersionAtLeast(11, 0);
```

### Time Zone Handling

**System Time Zone:**
```java
// Get system timezone
TimeZone tz = SystemUtilities.getSystemTimeZone();
```

### Implementation Notes

**Thread Safety:**
```java
// All methods are thread-safe
// Static utility methods only
// No shared state
```

**Error Handling:**
```java
try {
    File tempDir = SystemUtilities.createTempDirectory("temp-");
} catch (IOException e) {
    // Handle filesystem errors
}

try {
    List<NetworkInfo> interfaces = SystemUtilities.getNetworkInterfaces();
} catch (SocketException e) {
    // Handle network errors
}
```

### Best Practices

**Resource Management:**
```java
// Use try-with-resources for system resources
File tempDir = SystemUtilities.createTempDirectory("temp-");
try {
    // Use temporary directory
} finally {
    // Directory will be automatically cleaned up on JVM exit
}
```

**Environment Variables:**
```java
// Prefer getExternalVariable over direct System.getenv
String config = SystemUtilities.getExternalVariable("CONFIG");
// Checks both system properties and environment variables
```

This implementation provides robust system utilities with emphasis on platform independence, proper resource management, and comprehensive error handling.

---
## Traverser
[Source](/src/main/java/com/cedarsoftware/util/Traverser.java)

A utility class for traversing object graphs in Java, with cycle detection and configurable object visitation.

### Key Features
- Complete object graph traversal
- Cycle detection
- Configurable class filtering
- Support for collections, arrays, and maps
- Lambda-based processing
- Legacy visitor pattern support

### Core Methods

**Modern API (Recommended):**
```java
// Basic traversal with lambda
Traverser.traverse(root, classesToSkip, object -> {
    // Process object
});

// Simple traversal without skipping
Traverser.traverse(root, null, object -> {
    // Process object
});
```

### Class Filtering

**Skip Configuration:**
```java
// Create skip set
Set<Class<?>> skipClasses = new HashSet<>();
skipClasses.add(String.class);
skipClasses.add(Integer.class);

// Traverse with filtering
Traverser.traverse(root, skipClasses, obj -> {
    // Only non-skipped objects processed
});
```

### Collection Handling

**Supported Collections:**
```java
// Lists
List<String> list = Arrays.asList("a", "b", "c");
Traverser.traverse(list, null, obj -> {
    // Visits list and its elements
});

// Maps
Map<String, Integer> map = new HashMap<>();
Traverser.traverse(map, null, obj -> {
    // Visits map, keys, and values
});

// Arrays
String[] array = {"x", "y", "z"};
Traverser.traverse(array, null, obj -> {
    // Visits array and elements
});
```

### Object Processing

**Custom Processing:**
```java
// Type-specific processing
Traverser.traverse(root, null, obj -> {
    if (obj instanceof User) {
        processUser((User) obj);
    } else if (obj instanceof Order) {
        processOrder((Order) obj);
    }
});

// Counting objects
AtomicInteger counter = new AtomicInteger(0);
Traverser.traverse(root, null, obj -> counter.incrementAndGet());
```

### Legacy Support

**Deprecated Visitor Pattern:**
```java
// Using visitor interface (deprecated)
Traverser.Visitor visitor = new Traverser.Visitor() {
    @Override
    public void process(Object obj) {
        // Process object
    }
};
Traverser.traverse(root, visitor);
```

### Implementation Notes

**Thread Safety:**
```java
// Not thread-safe
// Use external synchronization if needed
synchronized(lockObject) {
    Traverser.traverse(root, null, obj -> process(obj));
}
```

**Error Handling:**
```java
try {
    Traverser.traverse(root, null, obj -> {
        // Processing that might throw
        riskyOperation(obj);
    });
} catch (Exception e) {
    // Handle processing errors
}
```

### Best Practices

**Memory Management:**
```java
// Limit scope with class filtering
Set<Class<?>> skipClasses = new HashSet<>();
skipClasses.add(ResourceHeavyClass.class);

// Process with limited scope
Traverser.traverse(root, skipClasses, obj -> {
    // Efficient processing
});
```

**Efficient Processing:**
```java
// Avoid heavy operations in processor
Traverser.traverse(root, null, obj -> {
    // Keep processing light
    recordReference(obj);
});

// Collect and process later if needed
List<Object> collected = new ArrayList<>();
Traverser.traverse(root, null, collected::add);
processCollected(collected);
```

This implementation provides a robust object graph traversal utility with emphasis on flexibility, proper cycle detection, and efficient processing options.

---
## UniqueIdGenerator
UniqueIdGenerator is a utility class that generates guaranteed unique, time-based, monotonically increasing 64-bit IDs suitable for distributed environments. It provides two ID generation methods with different characteristics and throughput capabilities.

### Features
- Distributed-safe unique IDs
- Monotonically increasing values
- Clock regression handling
- Thread-safe operation
- Cluster-aware with configurable server IDs
- Two ID formats for different use cases

### Basic Usage

**Standard ID Generation**
```java
// Generate a standard unique ID
long id = UniqueIdGenerator.getUniqueId();
// Format: timestampMs(13-14 digits).sequence(3 digits).serverId(2 digits)
// Example: 1234567890123456.789.99

// Get timestamp from ID
Date date = UniqueIdGenerator.getDate(id);
Instant instant = UniqueIdGenerator.getInstant(id);
```

**High-Throughput ID Generation**
```java
// Generate a 19-digit unique ID
long id = UniqueIdGenerator.getUniqueId19();
// Format: timestampMs(13 digits).sequence(4 digits).serverId(2 digits)
// Example: 1234567890123.9999.99

// Get timestamp from ID
Date date = UniqueIdGenerator.getDate19(id);
Instant instant = UniqueIdGenerator.getInstant19(id);
```

### ID Format Comparison

**Standard Format (getUniqueId)**   
```
Characteristics:
- Format: timestampMs(13-14 digits).sequence(3 digits).serverId(2 digits)
- Sequence: Counts from 000-999 within each millisecond
- Rate: Up to 1,000 IDs per millisecond
- Range: Until year 5138
- Example: 1234567890123456.789.99
```

**High-Throughput Format (getUniqueId19)**
```
Characteristics:
- Format: timestampMs(13 digits).sequence(4 digits).serverId(2 digits)
- Sequence: Counts from 0000-9999 within each millisecond
- Rate: Up to 10,000 IDs per millisecond
- Range: Until year 2286 (positive values)
- Example: 1234567890123.9999.99
```

### Cluster Configuration

Server IDs are determined in the following priority order:

**1. Environment Variable:**
```bash
export JAVA_UTIL_CLUSTERID=42
```

**2. Kubernetes Pod Name:**
```yaml
spec:
  containers:
    - name: myapp
      env:
        - name: HOSTNAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
```

**3. VMware Tanzu:**
```bash
export VMWARE_TANZU_INSTANCE_ID=7
```

**4. Cloud Foundry:**
```bash
export CF_INSTANCE_INDEX=3
```

**5. Hostname Hash (automatic fallback)**
**6. Random Number (final fallback)**

### Implementation Notes

**Thread Safety**
```java
// All methods are thread-safe
// Can be safely called from multiple threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        long id = UniqueIdGenerator.getUniqueId();
        processId(id);
    });
}
```

**Clock Regression Handling**
```java
// Automatically handles system clock changes
// No special handling needed
long id1 = UniqueIdGenerator.getUniqueId();
// Even if system clock goes backwards
long id2 = UniqueIdGenerator.getUniqueId();
assert id2 > id1; // Always true
```

### Best Practices

**Choosing ID Format**
```java
// Use standard format for general purposes
if (normalThroughput) {
    return UniqueIdGenerator.getUniqueId();
}

// Use 19-digit format for high-throughput scenarios
if (highThroughput) {
    return UniqueIdGenerator.getUniqueId19();
}
```

**Error Handling**
```java
try {
    Instant instant = UniqueIdGenerator.getInstant(id);
} catch (IllegalArgumentException e) {
    // Handle invalid ID format
    log.error("Invalid ID format", e);
}
```

**Performance Considerations**
```java
// Batch ID generation if needed
List<Long> ids = new ArrayList<>();
for (int i = 0; i < batchSize; i++) {
    ids.add(UniqueIdGenerator.getUniqueId());
}
```

### Limitations
- Server IDs limited to range 0-99
- High-throughput format limited to year 2286
- Minimal blocking behavior (max 1ms) if sequence numbers exhausted within a millisecond
- Requires proper cluster configuration for distributed uniqueness (otherwise uses hostname-based or random server IDs as for uniqueId within cluster)

### Support
For additional support or to report issues, please refer to the project's GitHub repository or documentation.