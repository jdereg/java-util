# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

**Maven-based Java project with JDK 8 compatibility**

- **Build**: `mvn compile`
- **Test**: `mvn test`
- **Package**: `mvn package`
- **Install**: `mvn install`
- **Run single test**: `mvn test -Dtest=ClassName`
- **Run tests with pattern**: `mvn test -Dtest="*Pattern*"`
- **Clean**: `mvn clean`
- **Generate docs**: `mvn javadoc:javadoc`

## Architecture Overview

**java-util** is a high-performance Java utilities library focused on memory efficiency, thread-safety, and enhanced collections. The architecture follows these key patterns:

### Core Structure
- **Main package**: `com.cedarsoftware.util` - Core utilities and enhanced collections
- **Convert package**: `com.cedarsoftware.util.convert` - Comprehensive type conversion system
- **Cache package**: `com.cedarsoftware.util.cache` - Caching strategies and implementations

### Key Architectural Patterns

**Memory-Efficient Collections**: CompactMap/CompactSet dynamically adapt storage structure based on size, using arrays for small collections and switching to hash-based storage as they grow.

**Null-Safe Concurrent Collections**: ConcurrentHashMapNullSafe, ConcurrentNavigableMapNullSafe, etc. extend JDK concurrent collections to safely handle null keys/values.

**Dynamic Code Generation**: CompactMap/CompactSet use JDK compiler at runtime to generate optimized subclasses when builder API is used (requires full JDK).

**Converter Architecture**: Modular conversion system with dedicated conversion classes for each target type, supporting thousands of built-in conversions between Java types.

**ClassValue Optimization**: ClassValueMap/ClassValueSet leverage JVM's ClassValue for extremely fast Class-based lookups.

## Development Conventions

### Code Style (from agents.md)
- Use **four spaces** for indentation—no tabs
- Keep lines under **120 characters**
- End files with newline, use Unix line endings
- Follow standard Javadoc for public APIs
- **JDK 1.8 source compatibility** - do not use newer language features

### Library Usage Patterns
- Use `ReflectionUtils` APIs instead of direct reflection
- Use `DeepEquals.deepEquals()` for data structure verification in tests (pass options to see diff)
- Use null-safe ConcurrentMaps from java-util for null support
- Use `DateUtilities.parse()` or `Converter.convert()` for date parsing
- Use `Converter.convert()` for type marshaling
- Use `FastByteArrayInputStream/OutputStream` and `FastReader/FastWriter` for performance
- Use `StringUtilities` APIs for null-safe string operations
- Use `UniqueIdGenerator.getUniqueId19()` for unique IDs (up to 10,000/ms, strictly increasing)
- Use `IOUtilities` for stream handling and transfers
- Use `ClassValueMap/ClassValueSet` for fast Class-based lookups
- Use `CaseInsensitiveMap` for case-insensitive string keys
- Use `CompactMap/CompactSet` for memory-efficient large collections

## Testing Framework

- **JUnit 5** (Jupiter) with parameterized tests
- **AssertJ** for fluent assertions
- **Mockito** for mocking
- Test resources in `src/test/resources/`
- Comprehensive test coverage with pattern: `*Test.java`

## Special Considerations

### JDK vs JRE Environments
- Builder APIs (`CompactMap.builder()`, `CompactSet.builder()`) require full JDK (compiler tools)
- These APIs throw `IllegalStateException` in JRE-only environments
- Use pre-built classes (`CompactLinkedMap`, `CompactCIHashMap`, etc.) or custom subclasses in JRE environments

### OSGi and JPMS Support
- Full OSGi bundle with proper manifest entries
- JPMS module `com.cedarsoftware.util` with exports for main packages
- No runtime dependencies on external libraries

### Thread Safety
- Many collections are thread-safe by design (Concurrent* classes)
- LRUCache and TTLCache are thread-safe with configurable strategies
- Use appropriate concurrent collections for multi-threaded scenarios