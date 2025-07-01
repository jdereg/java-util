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

## 🎯 WORK PHILOSOPHY - INCREMENTAL ATOMIC CHANGES 🎯

**Mental Model: Work with a "List of Changes" approach**

### The Change Hierarchy
- **Top-level changes** (e.g., "Fix security issues in DateUtilities")
  - **Sub-changes** (e.g., "Fix ReDoS vulnerability", "Fix thread safety")
    - **Sub-sub-changes** (e.g., "Limit regex repetition", "Add validation tests")

### Workflow for EACH Individual Change
1. **Pick ONE change** from any level (top-level, sub-change, sub-sub-change)
2. **Implement the change**
   - During development: Use single test execution for speed (`mvn test -Dtest=SpecificTest`)
   - Iterate until the specific functionality works
3. **When you think the change is complete:**
   - **MANDATORY**: Run full test suite: `mvn clean test`
   - **ALL 11,500+ tests MUST pass**
   - **If ANY test fails**: Fix immediately, run full tests again
4. **Once ALL tests pass:**
   - Ask for commit approval: "Should I commit this change? (Y/N)"
   - Human approves, commit immediately
   - Move to next change in the list

### Core Principles
- **Minimize Work-in-Process**: Keep delta between local files and committed git files as small as possible
- **Always Healthy State**: Committed code is always in perfect health (all tests pass)
- **Atomic Commits**: Each commit represents one complete, tested, working change
- **Human Controls Push**: Human decides when to push commits to remote

**🎯 GOAL: Each change is complete, tested, and committed before starting the next change**

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

**This workflow follows the INCREMENTAL ATOMIC CHANGES philosophy for systematic code reviews and improvements:**

### Step 1: Build Change List (Analysis Phase)
- Review Java source files using appropriate analysis framework
- For **Security**: Prioritize by risk (network utilities, reflection, file I/O, crypto, system calls)
- For **Performance**: Focus on hot paths, collection usage, algorithm efficiency
- For **Features**: Target specific functionality or API enhancements
- **Create hierarchical todo list:**
  - Top-level items (e.g., "Security review of DateUtilities")
  - Sub-items (e.g., "Fix ReDoS vulnerability", "Fix thread safety")
  - Sub-sub-items (e.g., "Limit regex repetition", "Add test coverage")

### Step 2: Pick ONE Change from the List
- Select the highest priority change from ANY level (top, sub, sub-sub)
- Mark as "in_progress" in todo list
- **Focus on this ONE change only**

### Step 3: Implement the Single Change
- Make targeted improvement to address the ONE selected issue
- **During development iterations**: Use targeted test execution for speed (`mvn test -Dtest=SpecificTest`)
  - This allows quick feedback loops while developing the specific feature/fix
  - Continue iterating until the targeted tests pass and functionality works
- **MANDATORY**: Add comprehensive JUnit tests for this specific change:
  - Tests that verify the improvement works correctly
  - Tests for edge cases and boundary conditions  
  - Tests for error handling and regression prevention
- Follow coding best practices and maintain API compatibility
- Update Javadoc and comments where appropriate

### Step 4: Completion Gate - ABSOLUTELY MANDATORY
**When you believe the issue/fix is complete and targeted tests are passing:**

- **🚨 CRITICAL - NON-NEGOTIABLE 🚨**: Run FULL test suite: `mvn test`
  - **This takes only ~16 seconds but tests ALL 11,500+ tests**
  - **This is the quality gate that ensures project health**
- **🚨 VERIFY ALL TESTS PASS 🚨**: Ensure 11,500+ tests pass (not ~10,000)
- **🚨 ZERO TOLERANCE FOR TEST FAILURES 🚨**: All tests must be 100% passing before proceeding
- **If even ONE test fails**: Fix issues immediately, run full tests again
- **NEVER move to Step 5, 6, 7, or 8 until ALL tests pass**
- **NEVER start new work until ALL tests pass**
- Mark improvement todos as "completed" only when ALL tests pass

**⚠️ WARNING: Skipping full test validation is a CRITICAL PROCESS VIOLATION ⚠️**

**THE PROCESS:**
1. **Development Phase**: Use targeted tests (`mvn test -Dtest=SpecificTest`) for fast iteration
2. **Completion Gate**: Run full test suite (`mvn test`) when you think you're done
3. **Quality Verification**: ALL 11,500+ tests must pass before proceeding

### Step 5: Update Documentation (for this ONE change)
- **changelog.md**: Add entry for this specific change under appropriate version
- **userguide.md**: Update if this change affects public APIs or usage patterns  
- **Javadoc**: Ensure documentation reflects this change
- **README.md**: Update if this change affects high-level functionality

### Step 6: Request Atomic Commit Approval
**MANDATORY HUMAN APPROVAL STEP for this ONE change:**
Present a commit approval request to the human with:
- Summary of this ONE improvement made (specific security fix, performance enhancement, etc.)
- List of files modified for this change
- Test results confirmation (ALL 11,500+ tests passing)
- Documentation updates made for this change
- Clear description of this change and its benefits
- Ask: "Should I commit this change?"

**CRITICAL COMMIT RULES:**
- **ONLY commit if human responds exactly "Y" or "Yes"**
- **If human does NOT write "Y" or "Yes", do NOT commit**
- **If human does not respond "Y" or "Yes", pay close attention to next instruction**
- **NEVER commit without explicit "Y" or "Yes" approval**

### Step 7: Atomic Commit (Only After Human Approval)
- **Immediately commit this ONE change** after receiving "Y" approval
- Use descriptive commit message format for this specific change:
  ```
  [Type]: [Brief description of this ONE change]
  
  - [This specific change implemented]
  - [Test coverage added for this change]
  - [Any documentation updated]
  
  🤖 Generated with [Claude Code](https://claude.ai/code)
  
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
  Where [Type] = Security, Performance, Feature, Refactor, etc.
- Mark this specific todo as "completed"
- **Repository is now in healthy state with this change committed**

### Step 8: Return to Change List
- **Pick the NEXT change** from the hierarchical list (top-level, sub, sub-sub)
- **Repeat Steps 2-7 for this next change**
- **Continue until all changes in the list are complete**
- Maintain todo list to track progress across entire scope

**Special Cases - Tinkering/Exploratory Work:**
For non-systematic changes, individual experiments, or small targeted fixes, the process can be adapted:
- Steps 1-2 can be simplified or skipped for well-defined changes
- Steps 4-6 remain mandatory (testing, documentation, human approval)
- Commit messages should still be descriptive and follow format

**This loop ensures systematic code improvement with proper testing, documentation, and human oversight for all changes.**

## 📦 DEPLOYMENT PROCESS 📦

**Maven deployment to Maven Central via Sonatype OSSRH**

### Prerequisites Check
Before deployment, verify the following conditions are met:

1. **Clean Working Directory**: No uncommitted local files
```bash
git status
# Should show: "nothing to commit, working tree clean"
```

2. **Remote Sync**: All local commits are pushed to remote
```bash
git push origin master
# Should be up to date with origin/master
```

3. **Dependency Verification**: json-io dependency must be correct version
   - json-io is test-scope only (java-util has zero runtime dependencies)
   - json-io version must be "1 behind" the current java-util version
   - This prevents circular dependency (java-util → json-io → java-util)
   - Current: json-io 4.55.0 in pom.xml (test scope)

### Deployment Steps

1. **Run Maven Deploy with Release Profile**
```bash
mvn clean deploy -DperformRelease=true
```
   - This will take significant time due to additional tests enabled with performRelease=true
   - Includes GPG signing of artifacts (requires GPG key and passphrase configured)
   - Uploads to Sonatype OSSRH staging repository
   - Automatically releases to Maven Central (autoReleaseAfterClose=true)

2. **Tag the Release**
```bash
git tag -a x.y.z -m "x.y.zYYYYMMDDHHMMSS"
```
   - Replace x.y.z with actual version (e.g., 3.6.0)
   - Replace YYYYMMDDHHMMSS with current timestamp in 24-hour format
   - Example: `git tag -a 3.6.0 -m "3.6.020250101120000"`

3. **Push Tags to Remote**
```bash
git push --tags
```

### Configuration Details
- **Sonatype OSSRH**: Configured in pom.xml distributionManagement
- **GPG Signing**: Automated via maven-gpg-plugin when performRelease=true
- **Nexus Staging**: Uses nexus-staging-maven-plugin with autoReleaseAfterClose
- **Bundle Generation**: OSGi bundle via maven-bundle-plugin
- **JPMS Module**: Module-info.java added via moditect-maven-plugin

### Security Notes
- GPG key and passphrase must be configured in Maven settings.xml
- OSSRH credentials required for Sonatype deployment
- Never commit GPG passphrases or credentials to repository

### Post-Deployment Verification
1. Check Maven Central: https://search.maven.org/artifact/com.cedarsoftware/java-util
2. Verify OSGi bundle metadata in deployed JAR
3. Confirm module-info.class present for JPMS support
4. Test dependency resolution in downstream projects (json-io, n-cube)