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
    - Response (200 OK): On success, returns JSON `{ "username": "...", "name": "...", "surname": "..." }` and sets an HTTP session cookie.
    - Error Responses:
      - `400 Bad Request`: Invalid input (e.g., missing fields, invalid format).
      - `401 Unauthorized`: Incorrect credentials.
      - `500 Internal Server Error`: Server-side error.
  - `POST /users`: Registers a new user.
    - Request: JSON `{ "username": "...", "password": "...", "name": "...", "surname": "..." }`.
    - Response (201 CREATED): On success, returns JSON `{ "username": "...", "name": "...", "surname": "..." }` and sets an HTTP session cookie.
    - Error Responses:
      - `400 Bad Request`: Invalid input or validation errors.
      - `409 Conflict`: Username already exists.
      - `500 Internal Server Error`: Server-side error.
  - `POST /auth/logout`: Logs out the currently authenticated user.
    - Request: No body required.
    - Response (200 OK): Returns JSON `{ "message": "Logout successful." }`. Invalidates the user's HTTP session.
    - Error Responses:
      - `500 Internal Server Error`: If an unexpected server error occurs during logout.
  - `GET /auth/me`: Checks if the current user has an active session.
    - Request: No body required.
    - Response (200 OK): If a session is active, returns JSON `{ "username": "...", "name": "...", "surname": "..." }`.
    - Error Responses:
      - `401 Unauthorized`: No active session.
- **Songs:**
  - `GET /songs`: Fetches all songs for the authenticated user.
    - Request: No body required.
    - Response (200 OK): JSON array of `SongWithAlbum` objects. Each object includes full song details and associated album details.
    - Error Responses:
      - `401 Unauthorized`: User not authenticated.
      - `500 Internal Server Error`: Server-side error.
  - `POST /songs`: Uploads a new song. If an album with the provided `albumTitle` doesn't exist for the user, a new album is created.
    - Request: `multipart/form-data` containing:
      - `title` (text, required): The title of the song.
      - `genre` (text, required): The genre of the song (must be one of the predefined values, see `GET /songs/genres`).
      - `albumTitle` (text, required): The title of the album.
      - `albumArtist` (text, required): The artist of the album.
      - `albumYear` (number, required): The year of the album. This year is also used as the song's year upon creation.
      - `audioFile` (file, required): The audio file for the song (e.g., `audio.mp3`).
      - `albumImage` (file, optional): The cover image for the album (e.g., `cover.jpg`). This is used if a new album is being created and this part is provided.
    - Response (201 CREATED): JSON `SongWithAlbum` object representing the newly created song and its (potentially new) album.
    - Error Responses:
      - `400 Bad Request`: Invalid input (e.g., missing required fields, invalid genre, invalid year format, file processing error).
      - `401 Unauthorized`: User not authenticated.
      - `409 Conflict`: If a constraint violation occurs (e.g., song title already exists in the album for that user, though this specific check might vary based on DAO implementation).
      - `500 Internal Server Error`: Server-side error (e.g., DAO exception, file storage issue).
  - `GET /songs/genres`: Fetches all available song genres.
    - Request: No body required.
    - Response (200 OK): JSON array of objects, where each object has a `name` (e.g., "ROCK") and `description` (e.g., "Rock Music") for the genre.
    - Error Responses:
      - `401 Unauthorized`: User not authenticated (if authentication is enforced for this endpoint, though typically it might be public).
      - `500 Internal Server Error`: Server-side error.
  - `GET /songs/{songId}`: Fetches details for a specific song, identified by `songId`.
    - Request: No body required.
    - Response (200 OK): JSON `SongWithAlbum` object containing full song details and associated album details.
    - Error Responses:
      - `400 Bad Request`: Invalid `songId` format.
      - `401 Unauthorized`: User not authenticated.
      - `404 Not Found`: Song not found or not owned by the user.
      - `500 Internal Server Error`: Server-side error.
  - `GET /songs/{songId}/audio`: Fetches the audio file for a specific song.
    - Request: No body required.
    - Response (200 OK): The audio file stream (e.g., `audio/mpeg`, `audio/ogg`) with appropriate `Content-Type` and `Content-Disposition` headers.
    - Error Responses:
      - `400 Bad Request`: Invalid `songId` format.
      - `401 Unauthorized`: User not authenticated.
      - `404 Not Found`: Song not found, not owned by the user, or audio file is missing.
      - `500 Internal Server Error`: Server-side error (e.g., error reading file).
  - `GET /songs/{songId}/image`: Fetches the album cover image for the album associated with a specific song.
    - Request: No body required.
    - Response (200 OK): The image file stream (e.g., `image/jpeg`, `image/png`) with appropriate `Content-Type` and `Content-Disposition` headers.
    - Error Responses:
      - `400 Bad Request`: Invalid `songId` format.
      - `401 Unauthorized`: User not authenticated.
      - `404 Not Found`: Song not found, album not found, not owned by the user, or image file is missing.
      - `500 Internal Server Error`: Server-side error (e.g., error reading file).
- **Playlists:**

  - `GET /playlists`: Fetches all playlists for the authenticated user.
    - Request: No body required.
    - Response (200 OK): JSON array of `Playlist` objects.
    - Error Responses:
      - `401 Unauthorized`: User not authenticated.
      - `500 Internal Server Error`: Server-side error.
  - `POST /playlists`: Creates a new playlist.
    - Request: JSON `{ "name": "...", "songIds": [...] }` (songIds is optional, if provided must be an array of positive integers).
    - Response (201 CREATED): On success, returns the created `Playlist` object.
    - Error Responses:
      - `400 Bad Request`: Invalid input (e.g., missing name, invalid name format, invalid song IDs).
      - `401 Unauthorized`: User not authenticated.
      - `409 Conflict`: Playlist name already exists for the user.
      - `500 Internal Server Error`: Server-side error (e.g., DAO exception).
  - `POST /playlists/{playlistId}/songs`: Adds one or more songs to an existing playlist.

    - Request: JSON `{ "songIds": [123, 456, ...] }`. `songIds` must be a non-empty array of positive integers.
    - Response (200 OK): On successful processing, returns JSON:

      ```json
      {
        "message": "Songs processed for playlist {playlistId}.",
        "addedSongIds": [
          /* IDs of songs successfully added */
        ],
        "duplicateSongIds": [
          /* IDs of songs already present in the playlist */
        ]
      }
      ```

    - Error Responses:
      - `400 Bad Request`: Invalid JSON, missing/empty `songIds`, invalid song ID format.
      - `401 Unauthorized`: User not authenticated.
      - `403 Forbidden`: User does not own the playlist, or a specified song is not owned by the user.
      - `404 Not Found`: Playlist with `{playlistId}` not found, or a specified song ID not found.
      - `500 Internal Server Error`: Other server-side errors.

  - `GET /playlists/{playlistId}/order`: Fetches the current order of songs for a specific playlist.
    - Request: No body required.
    - Response (200 OK): JSON array of song IDs representing the order. Example: `[101, 105, 102]`
    - Error Responses:
      - `400 Bad Request`: Invalid playlist ID format.
      - `401 Unauthorized`: User not authenticated.
      - `404 Not Found`: Playlist not found or user does not have access.
      - `500 Internal Server Error`: Other server-side errors.
  - `PUT /playlists/{playlistId}/order`: Updates the order of songs in a specific playlist.
    - Request: JSON array of song IDs in the desired new order. Example: `[105, 101, 102]`. The list must contain all and only the song IDs currently in the playlist, without duplicates.
    - Response (200 OK): JSON array of song IDs confirming the new order. Example: `[105, 101, 102]`
    - Error Responses:
      - `400 Bad Request`: Invalid JSON format, invalid playlist ID, song ID list does not match current playlist content (e.g., missing songs, extra songs, duplicate songs in request, invalid song IDs).
      - `401 Unauthorized`: User not authenticated.
      - `404 Not Found`: Playlist not found or user does not have access.
      - `500 Internal Server Error`: Other server-side errors.

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
