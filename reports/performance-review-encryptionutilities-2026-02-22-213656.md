# Performance Review Plan
Generated: 2026-02-22-213656
Scope: /Users/jderegnaucourt/workspace/java-util/src/main/java/com/cedarsoftware/util/EncryptionUtilities.java

## Executive Summary
EncryptionUtilities is generally stable and test-clean, but one correctness defect is severe: legacy AES/CBC payloads are occasionally misclassified as versioned AES/GCM payloads and fail to decrypt. This defect is intermittent (payload-byte dependent) and impacts backward compatibility in production data streams. Performance is dominated by PBKDF2, with additional avoidable overhead from per-call cipher/provider setup and repeated array slicing in GCM decryption paths.

## Performance Bottlenecks
### Critical Issues (Severe Impact)
- [ ] **Legacy payload misclassification in decrypt/decryptBytes** (`EncryptionUtilities.java:990-1000`, `1028-1039`)
  - Root cause: version detection relies on `data[0] == 1` only.
  - Reproduction: legacy CBC ciphertext with first byte `0x01` and length > header is parsed as GCM, causing `IllegalStateException`; legacy decrypt path succeeds with `createAesDecryptionCipher(key).doFinal(data)`.
  - Impact: intermittent backward-compatibility failures for legacy encrypted values.

### High Priority Issues
- [ ] **Allocation-heavy GCM decrypt parsing** (`EncryptionUtilities.java:991-994`, `1029-1032`)
  - Three `Arrays.copyOfRange()` calls per decrypt create salt/iv/ciphertext copies.
  - Optimize with offset-based APIs (`GCMParameterSpec(..., data, off, len)` and `Cipher.doFinal(data, off, len)`) to eliminate ciphertext copy and reduce heap churn.
- [ ] **Cipher/provider lookup on every encrypt/decrypt call** (`EncryptionUtilities.java:910`, `952`, `996`, `1034`)
  - `Cipher.getInstance("AES/GCM/NoPadding")` in hot path adds repeated provider lookup/object creation.
  - Introduce ThreadLocal cipher instances (with per-call `init`) for reduced overhead under sustained throughput.
- [ ] **Repeated security-property parsing on hot paths** (`EncryptionUtilities.java:284-317`, `782`, `901-903`, `943-944`)
  - Per-call `System.getProperty()` and parse logic for salt/iv bounds and PBKDF2 iteration validation.
  - Cache parsed values with explicit invalidation for tests/runtime config refresh.

### Medium Priority Issues
- [ ] **Duplicate encrypt/decrypt logic increases maintenance and optimization cost** (`EncryptionUtilities.java:893-965`, `976-1041`)
  - String and byte[] methods duplicate most control flow.
  - Consolidating internals enables single optimization path and lowers regression risk.
- [ ] **Digest creation overhead in high-frequency hash workloads** (`EncryptionUtilities.java:627-633`, digest wrapper methods)
  - Repeated `MessageDigest.getInstance(...)` allocations can be reduced with ThreadLocal digest reuse where call patterns are bursty.

## Performance Metrics
### Current State
- Encryption microbenchmark (80 ops, short payload): **61.96 ms/op** average (`encrypt`).
- Decryption microbenchmark (80 ops, short payload): **61.86 ms/op** average (`decrypt`).
- File hash microbenchmark (1 MiB file, SHA-256, 20 ops): **4.58 ms/op** average (`fastSHA256`).
- Key bottlenecks:
  - PBKDF2 iteration cost in `deriveKey()` (`EncryptionUtilities.java:782-789`),
  - extra allocations in GCM decrypt parsing,
  - repeated cipher/provider and property resolution.

### Target State
- Encryption average: **< 55 ms/op** for same benchmark profile.
- Decryption average: **< 55 ms/op** for same benchmark profile.
- SHA-256 file hash (1 MiB): **< 4.2 ms/op**.
- Key bottlenecks after optimization: PBKDF2 remains dominant; object-allocation and setup overhead materially reduced.

## Optimization Plan
### Quick Wins (1-3 days)
1. Fix decrypt format detection robustness (remove one-byte collision behavior) while preserving legacy compatibility.
2. Remove avoidable array copies in GCM decrypt paths using offset-based APIs.
3. Add focused regression tests for legacy-ciphertext/version-byte collision in both `decrypt()` and `decryptBytes()`.

### Major Optimizations (1-2 weeks)
1. Add ThreadLocal cipher reuse for AES/GCM encrypt/decrypt paths.
2. Add cached security-property parsing with safe cache invalidation hooks.
3. Refactor duplicated string/byte[] encrypt/decrypt logic into shared byte-oriented core methods.

### Architectural Changes (2-4 weeks)
1. Introduce explicit on-wire magic + version header for future encrypted payload formats.
2. Add migration support strategy (legacy CBC, v1 GCM, and future versions) with deterministic parser ordering.
3. Provide structured crypto telemetry hooks (timings, failures by mode/version) for production tuning.

## Implementation Strategy
### Phase 1: Low-hanging Fruit
- Implement deterministic format detection and fallback rules.
- Eliminate decrypt copy hot spots.
- Add regression tests for collision and backward compatibility.

### Phase 2: Algorithm Improvements
- Reuse crypto primitives safely via ThreadLocal pools.
- Consolidate duplicate logic to reduce code-path divergence.
- Cache validated configuration values to reduce repeated parsing overhead.

### Phase 3: System-level Changes
- Define and roll out explicit payload framing for future versions.
- Add runtime instrumentation for throughput/latency/failure tracking.
- Validate behavior under mixed legacy and modern ciphertext populations.

## Performance Testing Plan
- Keep existing targeted suites as functional guardrails:
  - `EncryptionTest`
  - `EncryptionSecurityTest`
  - `EncryptionUtilitiesLowLevelTest`
- Add dedicated regression tests for legacy-version collision cases.
- Add microbench harness for:
  - encrypt/decrypt small/medium/large payloads,
  - decrypt allocation profiles,
  - file hash throughput for 1 MiB / 10 MiB / 100 MiB files.
- Run full suite (`mvn clean test`) after each optimization batch.

## Risk Assessment
### Safe Optimizations
- Decrypt copy reduction via offset-based APIs.
- Internal method consolidation without API change.
- Additional regression coverage for compatibility behavior.

### Moderate Risk
- ThreadLocal cipher reuse (requires strict init/reset correctness).
- Property cache introduction (must preserve dynamic override expectations in tests).

### High Risk
- On-wire format framing changes affecting backward compatibility.
- Parser-order changes that could alter handling of malformed ciphertext.

## Recommendations
- Prioritize correctness fix for legacy-ciphertext collision before any throughput tuning.
- Add compatibility tests that encrypt with deprecated CBC helpers and decrypt with modern APIs.
- Introduce lightweight continuous performance checks on encryption/decryption and file-hash paths.
- Keep PBKDF2 defaults strong; optimize around setup/allocation overhead first.

## Estimated Impact
- Quick wins: **10-20%** end-to-end improvement on decrypt-heavy flows and elimination of intermittent legacy decrypt failures.
- Major optimizations: **20-35%** improvement for sustained encrypt/decrypt workloads.
- Full plan: **30-45%** improvement in utility-level throughput for hot crypto paths (PBKDF2 still dominant).

## Effort Estimate
- Quick wins: **1-2 engineering days**.
- Major optimizations: **3-5 engineering days**.
- Complete plan: **1.5-3 engineering weeks**, including compatibility validation and benchmark hardening.
