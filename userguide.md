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