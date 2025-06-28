# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🚨 CRITICAL RULE - READ FIRST 🚨

**BEFORE doing ANYTHING else, understand this NON-NEGOTIABLE requirement:**

### MANDATORY FULL TEST SUITE VALIDATION

**EVERY change, no matter how small, MUST be followed by running the complete test suite:**

```bash
mvn clean test
```

**ALL 11,500+ tests MUST pass before:**
- Moving to the next issue/file/task
- Committing any changes  
- Asking for human approval
- Starting any new work

**If even ONE test fails:**
- Stop immediately
- Fix the failing test(s)
- Run the full test suite again
- Only proceed when ALL tests pass

**This rule applies to:**
- Security fixes
- Performance improvements
- Feature additions
- Documentation changes
- ANY code modification

**❌ NEVER skip this step**
**❌ NEVER assume tests will pass**
**❌ NEVER move forward with failing tests**

**This is MORE IMPORTANT than the actual change itself.**

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

## Enhanced Review Loop

**This is the complete workflow that Claude Code MUST follow for systematic code reviews and improvements (security, performance, features, etc.):**

### Step 1: Select Next File/Area for Review
- Continue systematic review of Java source files using appropriate analysis framework
- For **Security**: Prioritize by risk (network utilities, reflection, file I/O, crypto, system calls)
- For **Performance**: Focus on hot paths, collection usage, algorithm efficiency
- For **Features**: Target specific functionality or API enhancements
- Mark current task as "in_progress" in todo list

### Step 2: Analysis and Issue Identification  
- Apply appropriate analysis framework (CODE_REVIEW.md for security, performance profiling, feature requirements)
- Classify findings by severity/priority: Critical, High, Medium, Low
- Create specific todo items for each issue found
- Focus on Critical and High priority issues first

### Step 3: Implement Improvements
- Make targeted improvements to address identified issues
- **MANDATORY**: Add comprehensive JUnit tests for all changes, including:
  - Tests that verify the improvement works correctly
  - Tests for edge cases and boundary conditions  
  - Tests for error handling and regression prevention
  - All new tests must pass along with existing 11,500+ test suite
- Follow coding best practices and maintain API compatibility
- Update Javadoc and comments where appropriate

### Step 4: Validate Changes - ABSOLUTELY MANDATORY
- **🚨 CRITICAL - NON-NEGOTIABLE 🚨**: Run full test suite: `mvn clean test`
- **🚨 VERIFY ALL TESTS PASS 🚨**: Ensure 11,500+ tests pass (not ~10,000)
- **🚨 ZERO TOLERANCE FOR TEST FAILURES 🚨**: All tests must be 100% passing before proceeding
- **If even ONE test fails**: Fix issues immediately before continuing to next step
- **NEVER move to Step 5, 6, 7, or 8 until ALL tests pass**
- **NEVER start new work until ALL tests pass**
- Mark improvement todos as "completed" only when tests pass

**⚠️ WARNING: Skipping full test validation is a CRITICAL PROCESS VIOLATION ⚠️**

### Step 5: Update Documentation
- **changelog.md**: Add entry describing improvements under appropriate version
- **userguide.md**: Update if changes affect public APIs or usage patterns  
- **Javadoc**: Ensure documentation reflects changes and provides clear guidance
- **README.md**: Update if changes affect high-level functionality

### Step 6: Commit Approval Process
**MANDATORY HUMAN APPROVAL STEP:**
Present a commit approval request to the human with:
- Summary of improvements made (security fixes, performance enhancements, new features, etc.)
- List of files modified 
- Test results confirmation (11,500+ tests passing)
- Documentation updates made
- Clear description of changes and benefits
- Ask: "Should I commit these changes? (Y/N)"

**CRITICAL**: NEVER commit without explicit human approval (Y/N response)

### Step 7: Commit Changes (Only After Human Approval)
- Use descriptive commit message format:
  ```
  [Type]: [Brief description] in [component]
  
  - [Specific change 1]
  - [Specific change 2] 
  - [Test coverage added]
  
  🤖 Generated with [Claude Code](https://claude.ai/code)
  
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
  Where [Type] = Security, Performance, Feature, Refactor, etc.
- Only commit after receiving explicit "Y" approval from human
- Mark commit-related todos as "completed"

### Step 8: Continue Review Loop
- Move to next highest priority issue
- Repeat this complete 8-step process
- Maintain todo list to track progress across entire codebase

**Special Cases - Tinkering/Exploratory Work:**
For non-systematic changes, individual experiments, or small targeted fixes, the process can be adapted:
- Steps 1-2 can be simplified or skipped for well-defined changes
- Steps 4-6 remain mandatory (testing, documentation, human approval)
- Commit messages should still be descriptive and follow format

**This loop ensures systematic code improvement with proper testing, documentation, and human oversight for all changes.**