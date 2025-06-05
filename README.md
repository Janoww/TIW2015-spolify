# Spolify - Music Playlist Application (Javascript version)

Spolify is a web-based music playlist management application. Users can upload songs, organize them into playlists, and play them. This is a Single Page Application (SPA) built with Vanilla JavaScript for the frontend and Java Servlets for the backend, providing a seamless user experience without full page reloads.

## Team Members

- **Zining Chen** (<zining.chen@mail.polimi>)
- **Edoardo Bergamo** (<edoardo.bergamo@mail.polimi.it>)

## Features

- **User Authentication**: login and registration.
- **Song Management**: Upload songs with details (title, genre, album info, artist, year), including audio files and album cover images.
- **Album Creation**: Albums are automatically created if they don't exist when uploading a song.
- **Playlist Management**: Create, view, and manage personal playlists. Add songs to playlists from your library.
- **Custom Song Ordering**: Reorder songs within playlists using a drag-and-drop interface and save the custom order.
- **Global Audio Player**: A persistent player UI that activates when a song is selected, allowing playback across different views.
- **Dedicated Songs Page**: View all uploaded songs in one place.
- **Dynamic UI**: SPA architecture ensures smooth navigation and interactions without page reloads.
- **RESTful API**: Backend services exposed via RESTful APIs.

## Technology Stack

- **Frontend**: HTML, CSS, Vanilla JavaScript
- **Backend**: Java Servlets
- **Application Server**: Apache Tomcat (tested with v10.1)
- **Database**: MySQL
- **Build Tool**: Apache Maven

## Installation

### Prerequisites

- Java Development Kit (JDK) (version 21 or later)
- Apache Maven
- Apache Tomcat (e.g., v10.1 or later)
- MySQL Server

### 1. Database Setup

1. Start your MySQL server.
2. Execute the SQL script `TIW.sql` (located in the project root) to create the necessary tables and schema.
3. **Configure Database Connection**:
   The database connection parameters (URL, username, password, and driver) are configured as context parameters in the `src/main/webapp/WEB-INF/web.xml` file.

   To change these settings:

   1. Open `src/main/webapp/WEB-INF/web.xml`.
   2. Locate the following `<context-param>` elements:

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

   3. Modify the `<param-value>` for each of these to match your MySQL database configuration. For example, update `jdbc:mysql://localhost:3306/TIW2025` to point to your database (e.g., `jdbc:mysql://localhost:3306/spolify_db`), and update `dbUser` and `dbPassword` accordingly.
   4. Ensure the `dbDriver` (e.g., `com.mysql.cj.jdbc.Driver`) is correct for your MySQL version and that the corresponding JDBC driver JAR is included in your project's classpath (typically managed by Maven in `pom.xml` and placed in `WEB-INF/lib` in the deployed `.war` file).

   The `AppContextListener` (`src/main/java/it/polimi/tiw/projects/listeners/AppContextListener.java`) reads these context parameters to establish the database connection.

### 2. Build the Project

1. Clone this repository or download the source code.
2. Open a terminal and navigate to the project's root directory (where `pom.xml` is located).
3. Build the project using Maven:

   ```bash
   mvn clean package
   ```

   This command will compile the source code, run tests, and package the application into a `.war` file (e.g., `Spolify.war`, check the `target/` directory). The exact name depends on the `artifactId` and `version` in `pom.xml`.

### 3. Deploy to Apache Tomcat

1. Ensure Apache Tomcat is installed and configured.
2. Copy the generated `.war` file (e.g., `target/spolify.war`) to the `webapps/` directory of your Tomcat installation.
3. Start Apache Tomcat. If Tomcat is already running, it should auto-deploy the application.

### 4. (Optional) Generate Mock Data

The project includes a utility to generate mock data for testing purposes.

1. Navigate to the project root directory in your terminal.
2. Run the following Maven command:

   ```bash
   mvn compile exec:java -Pgenerate
   ```

   To clean up mock data, you can run:

   ```bash
   mvn compile exec:java -Pcleanup
   ```

## Usage

1. Once Tomcat is running and the application is deployed, open your web browser.
2. Navigate to the application URL. This is typically `http://localhost:8080/<context-path>/`, where `<context-path>` is the name of your deployed `.war` file (e.g., `Spolify` if the war is `Spolify.war`).

   - Example: `http://localhost:8080/Spolify/`

3. **Authentication**:

   - You will be directed to the login page.
   - If you are a new user, click the link to switch to the signup form and create an account.
   - Log in with your credentials.

4. **Navigating the Application**:
   - **Home Page**: After login, you'll see the home page. Here you can:
     - View your existing playlists.
     - Upload new songs using the "Upload Song" form (provide song title, genre, album details, artist, year, audio file, and an optional album image).
     - Create new playlists using the "Create Playlist" form, selecting songs from your library.
   - **Songs Page**: Access via the navigation bar.
     - View a complete list of all songs you have uploaded.
     - The "Upload Song" form is also available here.
   - **Playlist View**: Click on a playlist name from the Home Page.
     - See all songs in that playlist, displayed in a paginated slider if there are many songs.
     - Add more songs to the current playlist.
   - **Global Audio Player**:
     - Clicking on any song title from a list (e.g., in Playlist View, Songs Page) will load and start playing the song in the global audio player.
     - The player UI (with controls like play/pause, volume, track info) will appear and remain accessible as you navigate other parts of the application.
     - You can close the player using its close button.
   - **Reorder Playlist Songs**:
     - On the Home Page, each playlist has a "Reorder" option.
     - This opens a modal displaying all songs in that playlist.
     - Drag and drop song titles to arrange them in your preferred order.
     - Click "Save Order" to persist your custom arrangement.
   - **Logout**: Click the "Logout" button in the main navigation bar to end your session.

## License

This project is licensed under the terms of the `LICENSE` file. Please see the `LICENSE` file for more details.
