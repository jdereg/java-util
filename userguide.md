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
