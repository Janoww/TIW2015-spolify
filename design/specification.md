# Music Playlist Application - Design Specification

## 1. Introduction

This document outlines the design for a web-based music playlist management application. Users can upload songs, organize them into playlists, and play them. This specification focuses on the Single Page Application (SPA) version, providing a seamless user experience without full page reloads.

**Technology Stack:**

* **Frontend:** HTML, CSS, JavaScript (Vanilla JS or a simple framework/library)
* **Backend:** Java Servlets (running on Apache Tomcat 10.1)
* **Database:** MySQL
* **API Format:** RESTful APIs exchanging JSON data

## 2. Architecture

The application follows a standard client-server SPA architecture:

* **Frontend:** A single HTML page dynamically updated using JavaScript. It handles user interactions, makes asynchronous requests (e.g., using `fetch` API) to the backend, and updates the DOM accordingly.
* **Backend:** Java Servlets expose RESTful API endpoints. They handle business logic, interact with the Data Access Object (DAO) layer, and manage user sessions.
* **DAO Layer:** Java classes responsible for interacting with the MySQL database (CRUD operations).
* **Database:** MySQL stores user data, song metadata, album information, and playlist structures.

(See Component Diagram: component_diagram.puml)

## 3. Database Schema

The database consists of the following tables:

(See ERD: erd.puml)

* **User:** Stores user credentials and basic information.
  * `user_id` (PK, INT, Auto-Increment)
  * `username` (VARCHAR, UNIQUE, NOT NULL)
  * `password` (VARCHAR, NOT NULL - *Store hashed passwords*)
  * `name` (VARCHAR, NOT NULL)
  * `surname` (VARCHAR, NOT NULL)
* **Album:** Stores album details.
  * `album_id` (PK, INT, Auto-Increment)
  * `title` (VARCHAR, NOT NULL)
  * `artist` (VARCHAR, NOT NULL)
  * `publication_year` (INT, NOT NULL)
  * `image_path` (VARCHAR, NOT NULL - *Path to the album cover image*)
* **Song:** Stores song metadata and file paths.
  * `song_id` (PK, INT, Auto-Increment)
  * `title` (VARCHAR, NOT NULL)
  * `genre` (VARCHAR, NOT NULL - *Assuming predefined genres*)
  * `audio_file_path` (VARCHAR, NOT NULL - *Path to the audio file*)
  * `album_id` (FK, INT, NOT NULL - References Album)
  * `owner_user_id` (FK, INT, NOT NULL - References User)
* **Playlist:** Stores playlist information.
  * `playlist_id` (PK, INT, Auto-Increment)
  * `title` (VARCHAR, NOT NULL)
  * `creation_date` (DATE, NOT NULL)
  * `owner_user_id` (FK, INT, NOT NULL - References User)
* **PlaylistSong:** Joining table for the N-N relationship between Playlist and Song. Includes custom ordering.
  * `playlist_id` (PK, FK, INT, NOT NULL - References Playlist)
  * `song_id` (PK, FK, INT, NOT NULL - References Song)
  * `custom_order` (INT, NULL - Stores the user-defined order index; NULL indicates default order)

## 4. API Endpoints (Servlets)

The backend will expose the following RESTful endpoints:

* **Authentication:**
  * `POST /login`: Authenticates user. Request: `{username, password}`. Response: Success/failure, user info. Sets HTTP session.
  * `POST /signup`: Registers a new user. Request: `{username, password, name, surname}`. Response: Success/failure.
  * `POST /logout`: Invalidates user session. Response: Success/failure.
* **Home Page Data:**
  * `GET /home`: Fetches data for the home view after login. Response: `{ playlists: [...], songs: [...] }` (user's playlists sorted by date desc, user's songs sorted by artist/album year).
* **Songs:**
  * `POST /songs`: Uploads a new song and its album info. Request: Multipart form-data (song file, album image, title, genre, album title, artist, year). Response: Success/failure, new song details.
  * `GET /songs/{songId}`: Fetches details for a specific song (for the player). Response: `{ song details }`.
* **Playlists:**
  * `POST /playlists`: Creates a new playlist. Request: `{ title: "...", songIds: [...] }`. Response: Success/failure, new playlist details.
  * `GET /playlists/{playlistId}`: Fetches details and songs for a specific playlist. Response: `{ playlist details, songs: [...] }` (songs sorted by default or custom order).
  * `POST /playlists/{playlistId}/songs`: Adds existing songs to a playlist. Request: `{ songIds: [...] }`. Response: Success/failure.
  * `PUT /playlists/{playlistId}/reorder`: Saves the custom order of songs in a playlist. Request: `{ orderedSongIds: [...] }`. Response: Success/failure.

*Error Handling:* APIs should return appropriate HTTP status codes (e.g., 200, 201, 400, 401, 403, 404, 500) and JSON error messages.

## 5. Frontend Components (Conceptual)

The JavaScript SPA will manage different views/components:

* **Login/Signup View:** Forms for user authentication and registration.
* **Main Application View (Single Page):**
  * **Navigation/Header:** User info, logout button.
  * **Home Section:** Displays user's playlists (list, sorted by date desc), song upload form, new playlist creation form (with sortable song list).
  * **Playlist View Section:** Displays songs of the selected playlist (5 at a time), Previous/Next buttons (client-side logic), "Add Songs" form, "Reorder" button.
  * **Player Section:** Displays details of the selected song and an HTML5 audio player.
  * **Reorder Modal:** Activated from Playlist View. Shows a list of songs in the playlist, allowing drag-and-drop reordering. Includes a "Save Order" button.

## 6. Key Features (SPA Specifics)

* **Single Page Experience:** All interactions happen within one HTML page, dynamically updating content via JavaScript without full reloads.
* **Asynchronous Communication:** Uses `fetch` or similar for all backend communication.
* **Client-Side Playlist Pagination:** The "Previous"/"Next" functionality in the Playlist View is handled entirely in JavaScript without server requests.
* **Client-Side Reordering:** Drag-and-drop reordering of songs in the modal happens client-side. The final order is sent to the server only when the user clicks "Save Order".
* **Dynamic Updates:** Forms (song upload, playlist creation, add song to playlist) update relevant sections of the page asynchronously upon success.
* **State Management:** JavaScript will manage the application state (current view, user data, playlists, songs, etc.).
