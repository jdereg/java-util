# AGENTS

These instructions guide any automated agent (such as Codex) that modifies this
repository.

## Coding Conventions
- Use **four spaces** for indentation—no tabs.
- End every file with a newline and use Unix line endings.
- Keep code lines under **120 characters** where possible.
- Follow standard Javadoc style for any new public APIs.
- This library maintains JDK 1.8 source compatibility, please make sure to not use source constructs or expected JDK libary calls beyond JDK 1.8.
- Whenever you need to use reflection, make sure you use ReflectionUtils APIs from java-util.
- For data structure verification in JUnit tests, use DeepEquals.deepEquals() [make sure to pass the option so you can see the "diff"].  This will make it clear where there is a difference in a complex data structure.
- If you need null support in ConcurrentMap implementations, use java-utils ConcurrentMaps that are null safe.
- Whenever parsing a String date, use either java-util DateUtilities.parse() (Date or ZonedDateTime), or use Converter.converter() which will use it inside.
- Use Converter.convert() as needed to marshal data types to match.
- For faster stream reading, use the FastByteArrayInputStream and FastByteArrayOutputStream.
- For faster Readers, use FastReader and FastWriter.
- USe StringUtilities APIs for common simplifications like comparing without worrying about null, for example.  Many other APIs on there.
- When a Unique ID is needed, use the UniqueIdGenerator.getUniqueId19() as it will give you a long, up to 10,000 per millisecond, and you can always get the time of when it was created, from it, and it is strictly increasing.
- IOUtilities has some nice APIs to close streams without extra try/catch blocks, and also has a nice transfer APIs, and transfer APIs that show call back with transfer stats.
- ClassValueMap and ClassValueSet make using JDK's ClassValue much easier yet retain the benefits of ClassValue in terms of speed.
- Of course, for CaseInsensitiveMaps, there is no better one that CaseInsensitiveMap.
- And if you need to create massive quantity of Maps, CompactMap (and it's variants) use significantly less space that regular JDK maps.

## Commit Messages
- Start with a short imperative summary (max ~50 characters).
- Leave a blank line after the summary, then add further details if needed.
- Don’t amend or rewrite existing commits.
- Please list the Codex agent as the author so we can see that in the "Blame" view at the line number level.

## Testing
- Run `mvn -q test` before committing to ensure tests pass.
- If tests can’t run due to environment limits, note this in the PR description.

## Documentation
- Update `changelog.md` with a bullet about your change.
- Update `userguide.md` whenever you add or modify public-facing APIs.

## Pull Request Notes
- Summarize key changes and reference the main files touched.
- Include a brief “Testing” section summarizing test results or noting any limitations.

