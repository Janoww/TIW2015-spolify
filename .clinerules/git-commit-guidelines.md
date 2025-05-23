## Brief overview

These guidelines are for formatting git commit messages to ensure consistency and clarity in the project's history. They are based on the observed patterns in the existing git log.

## Commit Message Format

- Messages should follow the pattern: `type: description`.
  - `type` should be in lowercase.
  - Follow the type with a colon and a space.
  - `description` should be a concise summary of the changes.
- Example: `feat: add user authentication module`

## Common Commit Types

Based on the project history, common types include:

- `feat`: A new feature or enhancement.
- `fix`: A bug fix.
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).
- `refactor`: A code change that neither fixes a bug nor adds a feature.
- `docs`: Documentation only changes.
- `test`: Adding missing tests or correcting existing tests.
- `chore`: Changes to the build process or auxiliary tools and libraries such as documentation generation.

## Subject Line

- Use the imperative mood in the subject line (e.g., "add feature" not "added feature" or "adds feature").
- Keep the subject line concise, ideally under 50 characters.

## Body (Optional)

- If more detail is needed, provide a longer description in the commit body, after a blank line following the subject.
- Explain _what_ and _why_ vs. _how_.

## Commit Scope and Granularity

- **Analyze Changes**: Before deciding on commit structure, always use the `git_diff` or `git_diff_staged` MCP tool to thoroughly review all modifications. This analysis will help determine the logical separation of changes.
- **Atomic Commits:** Each commit should represent a single logical change. Based on the diff analysis, group related changes into one commit.
- **Smaller Commits:** If a set of changes is large or multifaceted, divide it into multiple smaller, focused commits. This improves clarity, makes code reviews easier, and helps in pinpointing issues if a rollback is needed.
- **Partial Commits (CLI):** If a single file contains multiple distinct logical changes that should belong to different commits, use partial staging via the command line (e.g., `git add -p`). This allows for creating truly atomic commits even when changes are interleaved in files. Refer to `.clinerules/git-tool-guidelines.md` for more details on CLI fallback.
