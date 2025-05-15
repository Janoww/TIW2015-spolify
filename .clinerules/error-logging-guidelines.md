## Brief overview

These guidelines describe preferences for logging errors, ensuring that logs are informative but not overly verbose for common issues.

## Error Logging

- **Java Logging Implementation:**

  - When writing Java code, utilize the SLF4J API (`org.slf4j:slf4j-api`) with the Logback classic (`ch.qos.logback:logback-classic`) implementation for all logging purposes. This is the standard logging setup defined in the project's `pom.xml`.
  - Obtain a logger instance per class: `private static final Logger logger = LoggerFactory.getLogger(MyClass.class);`

- **Concise Logging for Common/Expected Errors:**

  - For errors that are common and expected as part of normal application flow (e.g., invalid user credentials, input validation failures), log a concise message.
  - This message should include key information relevant to the error (e.g., username for a failed login, the specific validation that failed) and the error message itself.
  - Avoid logging the full stack trace for these types of errors to keep logs cleaner and more readable.
  - Example: `logger.warn("Invalid credentials attempt for username: {}. Details: {}", username, e.getMessage());`

- **Detailed Logging for Unexpected/Critical Errors:**
  - For errors that are unexpected, indicate a bug, or could break application functionality, log the full stack trace along with a descriptive message.
  - This ensures that developers have enough information to diagnose and fix critical issues.
  - Example: `logger.error("Critical error processing payment for orderId: {}. Details: {}", orderId, e.getMessage(), e);`
