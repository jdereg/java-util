# AGENTS

These instructions guide any automated agent (such as Codex) that modifies this
repository.

## Coding Conventions
- Use **four spaces** for indentation—no tabs.
- End every file with a newline and use Unix line endings.
- Keep code lines under **120 characters** where possible.
- Follow standard Javadoc style for any new public APIs.

## Commit Messages
- Start with a short imperative summary (max ~50 characters).
- Leave a blank line after the summary, then add further details if needed.
- Don’t amend or rewrite existing commits.

## Testing
- Run `mvn -q test` before committing to ensure tests pass.
- If tests can’t run due to environment limits, note this in the PR description.

## Documentation
- Update `changelog.md` with a bullet about your change.
- Update `userguide.md` whenever you add or modify public-facing APIs.

## Pull Request Notes
- Summarize key changes and reference the main files touched.
- Include a brief “Testing” section summarizing test results or noting any limitations.

