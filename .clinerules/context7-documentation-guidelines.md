## Brief overview

These guidelines describe how to utilize the Context7 MCP server for retrieving various types of documentation. This helps in accessing relevant information for development tasks directly.

## Context7 Documentation Workflow

- **Step 1: Resolve Library ID**

  - When documentation is needed for a specific library, tool, or technology (e.g., Git, JavaScript, Java JDK), first use the `resolve-library-id` tool from the `github.com/upstash/context7-mcp` server.
  - Provide the name of the library or topic you are looking for (e.g., "git", "javascript", "jdk").
  - This tool will return a list of matching libraries. Select the most appropriate Context7-compatible library ID from the results.

- **Step 2: Get Library Documentation**
  - Once a valid Context7-compatible library ID is obtained, use the `get-library-docs` tool from the `github.com/upstash/context7-mcp` server.
  - Pass the `context7CompatibleLibraryID` obtained in the previous step.
  - Optionally, specify a `topic` to narrow down the documentation search.
  - This will fetch the relevant documentation.

## Examples of Documentation Retrieval

- **Git Commands:**
  - To get documentation for Git:
    1. Use `resolve-library-id` with `libraryName: "git"` (resolved to `/git/git`).
    2. Or directly use `get-library-docs` with ID `/git/git`.
- **JavaScript Cheatsheets:**
  - To get a JavaScript cheatsheet:
    1. Use `resolve-library-id` with `libraryName: "javascript cheatsheet"` (resolved to `/wilfredinni/javascript-cheatsheet`).
    2. Or directly use `get-library-docs` with ID `/wilfredinni/javascript-cheatsheet`.
- **Java JDK Documentation:**

  - To get documentation for the Java Development Kit (JDK):
    1. Use `resolve-library-id` with `libraryName: "jdk"` or `libraryName: "openjdk jdk"` (resolved to `/openjdk/jdk`).
    2. Or directly use `get-library-docs` with ID `/openjdk/jdk`, potentially specifying a topic like "collections" or "streams".

- **OWASP Cheatsheets:**
  - To get documentation for OWASP security risks and standards:
    1. Use `resolve-library-id` with `libraryName: "OWASP cheatsheet"` (resolved to `/owasp/cheatsheetseries`).
    2. Or directly use `get-library-docs` with ID `/owasp/cheatsheetseries`, potentially specifying a topic like "XSS", "Servlet", "Tomcat", "html", or "css".
  - If information is not found, search more topics with different keywords to try to retrieve the relevant information.

## General Guideline

- When clarification or detailed information about a technology, library, or API is required during development, consider using the Context7 MCP server as a primary resource for fetching documentation before resorting to external searches.
