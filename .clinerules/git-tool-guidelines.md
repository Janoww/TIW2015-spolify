## Brief overview

These guidelines provide rules for using Git-related MCP tools within this project to ensure correct and efficient version control practices.

## Git Tool Usage

- **Full Path Requirement:**
  - When using Git tools provided by an MCP server, always provide the full path to the project repository as an argument (e.g., `/home/biscotti/GitProjects/TIW2015-spolify`).
- **Staging Files (`git_add` MCP Tool):**
  - **Whole File Staging:** The `git_add` tool from the Git MCP server stages entire files. It cannot be used to stage only parts of a modified file (i.e., it does not support `git add -p` or hunk-based staging).
  - **Avoid `git_add .`:** Avoid using `git_add .` as it can incorrectly include files from the `.git` directory.
  - **Explicit Staging:** First use `git_status` to review the list of changed files. Then, explicitly list the specific files to be staged with `git_add [file1] [file2] ...`.
  - **Staging Deleted Files:** To stage deleted files, it's recommended to use `git add -u` (or `git add --update`) via the `execute_command` tool. This ensures Git correctly stages removals of tracked files. The `git_add` MCP tool might not handle this specific scenario as directly as the CLI command.
  - **Partial Commits (CLI Fallback):** If only specific changes within a file need to be committed (a partial commit), the standard Git command-line interface (CLI) (e.g., `git add -p`) must be used instead of the `git_add` MCP tool. This is also mentioned in the "Limitations and Fallback to Command Line" section.
- **Commit Message Creation (`git_commit`):**
  - Before creating a commit, use `git_diff` (or `git_diff_staged` if changes are already staged) to review the modifications.
  - Use the insights from the diff to write a clear, concise, and meaningful commit message that accurately describes the changes, following the project's commit message guidelines.

## Limitations and Fallback to Command Line

While MCP Git tools are convenient, there are situations where falling back to the standard Git command line interface (CLI) is recommended or necessary:

- **Interactive Operations:** For commands requiring interactive user input (e.g., `git rebase -i`, `git add -p`), the Git CLI is essential.
- **Complex Merge Conflict Resolution:** Resolving intricate merge conflicts often requires visual diff tools and manual intervention best handled via the CLI and associated tools.
- **Authentication Issues:** If MCP tools face issues with credentials for remote operations (push, pull, fetch), the CLI (with established credential helpers) should be used.
- **Unsupported Commands/Options:** If a specific Git command, a newer feature, or a particular option is not supported by the available MCP tools, use the Git CLI.
- **Git Hooks Behavior:** If precise execution of Git hooks (e.g., `pre-commit`, `post-commit`) is critical, using the Git CLI ensures they are triggered as expected.
- **Detailed Error Diagnosis:** For complex errors, the Git CLI may provide more verbose or specific diagnostic information than MCP tool responses.
- **Environment-Specific Configurations:** If local Git configurations (e.g., global `.gitconfig` settings, aliases) significantly impact command behavior, the CLI will reflect these more accurately.
