# Music Playlist Application (Spolify)

## Introduction

This is a web-based music playlist management application. Users can upload songs, organize them into playlists, and play them. This version of the application is a Multi-Page Application (MPA) that does not rely on JavaScript. Each major user action results in a full page reload, with the server rendering HTML using Thymeleaf templates and handling interactions via standard form submissions.

## Technology Stack

- **Frontend:** HTML, CSS, Thymeleaf (server-side templating)
- **Backend:** Java Servlets (Jakarta Servlet API 6.1.0)
- **Web Server:** Apache Tomcat (e.g., 10.1.x or later, compatible with Servlet 6.0+)
- **Database:** MySQL (e.g., 8.x)
- **Build Tool:** Apache Maven
- **Java Version:** JDK 21

## Prerequisites

Before you begin, ensure you have the following installed:

- Java Development Kit (JDK) 21 or later
- Apache Maven (e.g., 3.6.x or later)
- MySQL Server (e.g., 8.x)
- Apache Tomcat (e.g., 10.1.x or later, compatible with Jakarta EE 10 / Servlet 6.0)

## Setup and Installation

Follow these steps to set up and run the project:

**1. Clone the Repository:**

First, clone the project repository to your local machine.

**2. Database Setup:**

- Ensure your MySQL server is running.
- Connect to your MySQL server using a client (e.g., MySQL command-line client, MySQL Workbench).
- Execute the `TIW.sql` script located in the root of the project to create the database schema. This script will create a database named `TIW2025` and all the necessary tables.

  Example using the MySQL command-line client:

  ```bash
  mysql -u your_mysql_username -p < TIW.sql
  ```

  You will be prompted to enter the password for `your_mysql_username`.

**3. Configure Database Credentials:**

- Open the `src/main/webapp/WEB-INF/web.xml` file in your project.
- Locate the `<context-param>` section for database configuration. It looks like this:

  ```xml
  <context-param>
      <param-name>dbUrl</param-name>
      <param-value>jdbc:mysql://localhost:3306/TIW2025</param-value>
  </context-param>
  <context-param>
      <param-name>dbUser</param-name>
      <param-value>tiw</param-value>
  </context-param>
  <context-param>
      <param-name>dbPassword</param-name>
      <param-value>TIW2025</param-value>
  </context-param>
  <context-param>
      <param-name>dbDriver</param-name>
      <param-value>com.mysql.cj.jdbc.Driver</param-value>
  </context-param>
  ```

- **Update the following `param-value` fields:**
  - `dbUrl`: Modify if your MySQL server is on a different host or port, or if you used a different database name than `TIW2025`.
  - `dbUser`: Change `tiw` to your actual MySQL username.
  - `dbPassword`: Change `TIW2025` to your actual MySQL password.

**4. Build the Project:**

- Navigate to the root directory of the project in your terminal (the directory containing `pom.xml`).
- Use Apache Maven to compile the project and package it into a WAR (Web Application Archive) file:

  ```bash
  mvn clean package
  ```

- This command will clean previous builds and create a new WAR file named `Spolify.war` in the `target/` directory (as specified by `<finalName>Spolify</finalName>` in `pom.xml`).

## Deployment

- Deploy the generated `Spolify.war` file from the `target/` directory to your Apache Tomcat server.
- The most common method is to copy the `Spolify.war` file into the `webapps` directory of your Tomcat installation.
  - Example: `cp target/Spolify.war /path/to/your/tomcat/webapps/`
- Apache Tomcat will automatically detect the new WAR file and deploy the application.

## Running the Application

- Start your Apache Tomcat server if it's not already running.
  - You can typically do this by executing `startup.sh` (on Linux/macOS) or `startup.bat` (on Windows) located in the `bin` directory of your Tomcat installation.
- Once Tomcat is running and the application is deployed, open your web browser and navigate to the application. The URL will generally be:

  `http://localhost:8080/Spolify/`

  - The context path `/Spolify/` is derived from the WAR file name (`Spolify.war`).
  - The application's entry point is `index.html`, which serves as the login and signup page.
