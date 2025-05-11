## Brief overview
These guidelines provide rules for using Git-related MCP tools within this project to ensure correct and efficient version control practices.

## Git Tool Usage
- **Full Path Requirement:**
  - When using Git tools provided by an MCP server, always provide the full path to the project repository as an argument (e.g., `/home/biscotti/GitProjects/TIW2015-spolify`).
- **Staging Files (`git_add`):**
  - Avoid using `git_add .` as it can incorrectly include files from the `.git` directory.
  - Instead, first use `git_status` to review the list of changed files.
  - Then, explicitly list the specific files to be staged with `git_add [file1] [file2] ...`.
- **Commit Message Creation (`git_commit`):**
  - Before creating a commit, use `git_diff` (or `git_diff_staged` if changes are already staged) to review the modifications.
  - Use the insights from the diff to write a clear, concise, and meaningful commit message that accurately describes the changes, following the project's commit message guidelines.
