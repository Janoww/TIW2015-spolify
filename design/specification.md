# Music Playlist Application - Design Specification

## 1. Introduction

This document outlines the design for a web-based music playlist management application. Users can upload songs, organize them into playlists, and play them. This specification focuses on the Single Page Application (SPA) version, providing a seamless user experience without full page reloads.

**Technology Stack:**

- **Frontend:** HTML, CSS, JavaScript (Vanilla JS)
- **Backend:** Java Servlets (running on Apache Tomcat 10.1)
- **Database:** MySQL
- **API Format:** RESTful APIs exchanging JSON data

## 2. Architecture

The application follows a standard client-server SPA architecture:

- **Frontend:** A single HTML page dynamically updated using JavaScript. It handles user interactions, makes asynchronous requests (e.g., using `fetch` API) to the backend, and updates the DOM accordingly.
- **Backend:** Java Servlets expose RESTful API endpoints. They handle business logic, interact with the Data Access Object (DAO) layer, and manage user sessions.
- **DAO Layer:** Java classes responsible for interacting with the MySQL database (CRUD operations).
- **Database:** MySQL stores user data, song metadata, album information, and playlist structures.

(See Component Diagram: [component_diagram.puml](/design/component_diagram.puml))

## 3. Database Schema

The database consists of the following tables:

(See ERD: [erd.puml](/design/erd.puml))

- **User:** Stores user credentials and basic information.
  - `idUser` (PK, BINARY(16), NOT NULL)
  - `username` (VARCHAR(100), UNIQUE, NOT NULL)
  - `password` (VARCHAR(100), NOT NULL - _Store hashed passwords_)
  - `name` (VARCHAR(100))
  - `surname` (VARCHAR(100))
- **Album:** Stores album details. Each album is associated with a user.
  - `idAlbum` (PK, INT, Auto-Increment, NOT NULL)
  - `name` (VARCHAR(100), NOT NULL) - _Unique per user_
  - `year` (INT, NOT NULL)
  - `artist` (VARCHAR(100), NOT NULL)
  - `image` (VARCHAR(255)) - _Optional Album cover image_
  - `idUser` (FK, BINARY(16), NOT NULL - References User.idUser)
- **Song:** Stores song metadata and file paths. Each song belongs to an album and is associated with a user.
  - `idSong` (PK, INT, Auto-Increment, NOT NULL)
  - `title` (VARCHAR(100), NOT NULL)
  - `idAlbum` (FK, INT, NOT NULL - References Album.idAlbum)
  - `year` (INT, NOT NULL) - _Year of the song, potentially different from album year_
  - `genre` (VARCHAR(100))
  - `audioFile` (VARCHAR(255), NOT NULL - _Audio filename_)
  - `idUser` (FK, BINARY(16), NOT NULL - References User.idUser)
- **playlist_metadata:** Stores playlist metadata. Each playlist is created by a user.
  - `idPlaylist` (PK, INT, Auto-Increment, NOT NULL)
  - `name` (VARCHAR(100), NOT NULL) - _Unique per user_
  - `birthday` (TIMESTAMP, NOT NULL, DEFAULT CURRENT*TIMESTAMP) - \_Creation timestamp*
  - `idUser` (FK, BINARY(16), NOT NULL - References User.idUser)
- **playlist_content:** Joining table for the N-N relationship between `playlist_metadata` and `Song`.
  - `idPlaylist` (PK, FK, INT, NOT NULL - References playlist_metadata.idPlaylist)
  - `idSong` (PK, FK, INT, NOT NULL - References Song.idSong)
  - _Unique constraint on (`idSong`, `idPlaylist`)_

## 4. API Endpoints (Servlets)

The backend will expose RESTful API endpoints, all prefixed with `/api/v1/`. The primary servlets and their functionalities are:

- **Authentication & User Management:**
  - `POST /auth/login`: Authenticates an existing user.
    - Request: JSON `{ "username": "...", "password": "..." }`.
    - Response: On success (200 OK), returns JSON `{ "username": "...", "name": "...", "surname": "..." }` and sets an HTTP session cookie. On failure, returns an appropriate error status (e.g., 400 Bad Request for invalid input, 401 Unauthorized for incorrect credentials, 500 Internal Server Error).
  - `POST /users`: Registers a new user.
    - Request: JSON `{ "username": "...", "password": "...", "name": "...", "surname": "..." }`.
    - Response: On success (201 CREATED), returns JSON `{ "username": "...", "name": "...", "surname": "..." }` and sets an HTTP session cookie. On failure (e.g., username already exists, validation errors), returns an appropriate error status (e.g., 400 Bad Request, 409 Conflict, 500 Internal Server Error).
  - `POST /auth/logout`: Logs out the currently authenticated user.
    - Request: No body required.
    - Response: On success (200 OK), returns JSON `{ "message": "Logout successful." }`. Invalidates the user's HTTP session.
  - `GET /auth/me`: Checks if the current user has an active session.
    - Request: No body required.
    - Response: If a session is active (200 OK), returns JSON `{ "username": "...", "name": "...", "surname": "..." }`. If no active session (401 Unauthorized).
- **Home Page Data:**
  - `GET /home`: Fetches data for the home view after login. Response: `{ playlists: [...], songs: [...] }` (user's playlists sorted by date desc, user's songs sorted by artist/album year).
- **Songs:**
  - `GET /songs`: Fetches all songs for the authenticated user.
    - Response: JSON array of `SongWithAlbum` objects. Each object includes full song details and associated album details.
  - `POST /songs`: Uploads a new song. If an album with the provided `albumTitle` doesn't exist for the user, a new album is created.
    - Request: `multipart/form-data` containing:
      - `title` (text, required): The title of the song.
      - `genre` (text, required): The genre of the song (must be one of the predefined values, see `GET /songs/genres`).
      - `albumTitle` (text, required): The title of the album.
      - `albumArtist` (text, required): The artist of the album.
      - `albumYear` (number, required): The year of the album. This year is also used as the song's year upon creation.
      - `audioFile` (file, required): The audio file for the song (e.g., `audio.mp3`).
      - `albumImage` (file, optional): The cover image for the album (e.g., `cover.jpg`). This is used if a new album is being created and this part is provided.
    - Response: JSON `SongWithAlbum` object representing the newly created song and its (potentially new) album.
  - `GET /songs/genres`: Fetches all available song genres.
    - Response: JSON array of objects, where each object has a `name` (e.g., "ROCK") and `description` (e.g., "Rock Music") for the genre.
  - `GET /songs/{songId}`: Fetches details for a specific song, identified by `songId`.
    - Response: JSON `SongWithAlbum` object containing full song details and associated album details.
  - `GET /songs/{songId}/audio`: Fetches the audio file for a specific song.
    - Response: The audio file stream (e.g., `audio/mpeg`, `audio/ogg`).
  - `GET /songs/{songId}/image`: Fetches the album cover image for the album associated with a specific song.
    - Response: The image file stream (e.g., `image/jpeg`, `image/png`).
- **Playlists:**
  - `POST /playlists`: Creates a new playlist. Request: `{ title: "...", songIds: [...] }`. Response: Success/failure, new playlist details.
  - `GET /playlists/{playlistId}`: Fetches details and songs for a specific playlist. Response: `{ playlist details, songs: [...] }` (songs sorted by default or custom order).
  - `POST /playlists/{playlistId}/songs`: Adds existing songs to a playlist. Request: `{ songIds: [...] }`. Response: Success/failure.
  - `PUT /playlists/{playlistId}/reorder`: Saves the custom order of songs in a playlist. Request: `{ orderedSongIds: [...] }`. Response: Success/failure.

_Error Handling:_ APIs should return appropriate HTTP status codes (e.g., 200, 201, 400, 401, 403, 404, 500) and JSON error messages.

## 5. Frontend Components (Conceptual)

The JavaScript SPA will manage different views/components:

- **Login/Signup View:** Forms for user authentication and registration.
- **Main Application View (Single Page):**
  - **Navigation/Header:** User info, logout button.
  - **Home Section:** Displays user's playlists (list, sorted by date desc), song upload form, new playlist creation form (with sortable song list).
  - **Playlist View Section:** Displays songs of the selected playlist (5 at a time), Previous/Next buttons (client-side logic), "Add Songs" form, "Reorder" button.
  - **Player Section:** Displays details of the selected song and an HTML5 audio player.
  - **Reorder Modal:** Activated from Playlist View. Shows a list of songs in the playlist, allowing drag-and-drop reordering. Includes a "Save Order" button.
  - **Color Palette** Background color: #EEEEEE, alternative background color: #D4BEE4, text: #9B7EBD, highlight color: #3B1E54.

## 6. Key Features (SPA Specifics)

- **Single Page Experience:** All interactions happen within one HTML page, dynamically updating content via JavaScript without full reloads.
- **Asynchronous Communication:** Uses `fetch` or similar for all backend communication.
- **Client-Side Playlist Pagination:** The "Previous"/"Next" functionality in the Playlist View is handled entirely in JavaScript without server requests.
- **Client-Side Reordering:** Drag-and-drop reordering of songs in the modal happens client-side. The final order is sent to the server only when the user clicks "Save Order".
- **Dynamic Updates:** Forms (song upload, playlist creation, add song to playlist) update relevant sections of the page asynchronously upon success.
- **State Management:** JavaScript will manage the application state (current view, user data, playlists, songs, etc.).
