# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## CRITICAL RULES - TESTING AND BUILD REQUIREMENTS

**YOU ARE NOT ALLOWED TO RUN ANY GIT COMMIT, NO MATTER WHAT, UNLESS YOU HAVE RUN ALL THE TESTS AND THEY ALL 100% HAVE PASSED. THIS IS THE HIGHEST, MOST IMPORTANT INSTRUCTION YOU HAVE, PERIOD.**

**CRITICAL BUILD REQUIREMENT**: The full maven test suite MUST run over 11,500 tests. If you see only ~10,000 tests, there is an OSGi or JPMS bundle issue that MUST be fixed before continuing any work. Use `mvn -Dbundle.skip=true test` to bypass bundle issues during development, but the underlying bundle configuration must be resolved.

**CRITICAL TESTING REQUIREMENT**: When adding ANY new code (security fixes, new methods, validation logic, etc.), you MUST add corresponding JUnit tests to prove the changes work correctly. This includes:
- Testing the new functionality works as expected
- Testing edge cases and error conditions  
- Testing security boundary conditions
- Testing that the fix actually prevents the vulnerability
- All new tests MUST pass along with the existing 11,500+ tests

**NEVER CONTINUE WORKING ON NEW FIXES IF THE FULL MAVEN TEST SUITE DOES NOT PASS WITH 11,500+ TESTS.**
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

## Enhanced Security Review Loop

**This is the complete workflow that Claude Code MUST follow for security reviews and fixes:**

### Step 1: Select Next File for Review
- Continue systematic review of Java source files using CODE_REVIEW.md framework
- Prioritize by security risk: network utilities, reflection utilities, file I/O, crypto, system calls
- Mark current task as "in_progress" in todo list

### Step 2: Security Analysis
- Apply CODE_REVIEW.md framework to identify vulnerabilities
- Classify findings by severity: Critical, High, Medium, Low
- Create specific todo items for each security issue found
- Focus on Critical and High severity issues first

### Step 3: Implement Security Fixes
- Make targeted security improvements to address identified vulnerabilities
- **MANDATORY**: Add comprehensive JUnit tests for all security fixes, including:
  - Tests that verify the fix prevents the vulnerability
  - Tests for edge cases and boundary conditions  
  - Tests for error handling and security boundary violations
  - All new tests must pass along with existing 11,500+ test suite
- Follow secure coding practices and maintain API compatibility
- Update Javadoc with security warnings where appropriate

### Step 4: Validate Changes
- **CRITICAL**: Run full test suite: `mvn clean test`
- **VERIFY**: Ensure 11,500+ tests pass (not ~10,000)
- **REQUIREMENT**: All tests must be 100% passing before proceeding
- If tests fail, fix issues before continuing to next step
- Mark security fix todos as "completed" only when tests pass

### Step 5: Update Documentation
- **changelog.md**: Add entry describing security fixes under appropriate version
- **userguide.md**: Update if security changes affect public APIs or usage patterns
- **Javadoc**: Ensure security warnings and usage guidance are clear
- **README.md**: Update if security changes affect high-level functionality

### Step 6: Commit Approval Process
**MANDATORY HUMAN APPROVAL STEP:**
Present a commit approval request to the human with:
- Summary of security vulnerabilities fixed
- List of files modified 
- Test results confirmation (11,500+ tests passing)
- Documentation updates made
- Clear description of security improvements
- Ask: "Should I commit these security fixes? (Y/N)"

**CRITICAL**: NEVER commit without explicit human approval (Y/N response)

### Step 7: Commit Changes (Only After Human Approval)
- Use descriptive commit message format:
  ```
  Security: Fix [vulnerability type] in [component]
  
  - [Specific fix 1]
  - [Specific fix 2] 
  - [Test coverage added]
  
  🤖 Generated with [Claude Code](https://claude.ai/code)
  
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
- Only commit after receiving explicit "Y" approval from human
- Mark commit-related todos as "completed"

### Step 8: Continue Review Loop
- Move to next highest priority security issue
- Repeat this complete 8-step process
- Maintain todo list to track progress across entire codebase

**This loop ensures systematic security hardening with proper testing, documentation, and human oversight for all changes.**