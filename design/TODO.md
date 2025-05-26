# Project TODO List - Spolify (JavaScript Version)

This document outlines the missing logic, parts to be implemented, and potential improvements for the Spolify project, focusing on the JavaScript Single Page Application (SPA) version.

## I. Backend (Java Servlets & DAOs)

The backend API is specified in `design/specification.md`. The primary focus here is ensuring all specified endpoints and underlying DAO logic are fully implemented and robust.

1.  **API Endpoint Completeness & Correctness (Final Review):**
    - **Review Task:** Conduct a final pass comparing all API endpoints, methods, request/response structures, and status codes in `design/specification.md` against the implemented servlet logic to ensure 100% functional coverage and correctness.
    - **Consideration:** `PlaylistOrderDAO` uses file-based storage for custom order, as `playlist_content` table lacks an explicit order index. (Architectural note, current implementation is file-based and verified as robust in its file operations).

## II. Frontend (JavaScript - SPA)

1.  **Core SPA Structure (`app.js` / Main Controller):**

    - **Implement/Refine Router:** Current navigation in `app.js` is via direct function calls. Implement a more robust client-side router (e.g., hash-based or custom) for view transitions, history, and potential URL parameters (e.g., `/playlist/{id}`).
    - **Implement Navigation Handler:** Wire up the "Playlist" button in `index.html`'s navbar (e.g., to show a view of all user playlists or navigate to the home playlist section).
    - **Implement Global State Management:** Define a clear strategy for managing global state (current user, auth status, cached data like playlists/songs if beneficial). `sessionStorage` is used for `currentUser`.
    - **Initialization Logic:** `app.js` correctly checks session and initializes login/home page. (Verified as functionally implemented).

2.  **Authentication (`loginView.js`, `loginHandler.js`):**

    - **Improve Signup Flow:** After successful signup (`POST /users`), `loginHandler.js` should transition to `initHomePage` and store user data in `sessionStorage` (reflecting API spec that user is logged in), instead of redirecting back to login. (Backend `POST /users` correctly sets session attribute; this TODO is for frontend handler).
    - **Refactor Validation:** Move form validation functions (`validateField`, `validateForm`) from `loginHandler.js` to `formUtils.js` for reuse.

3.  **Home View (`homeView.js`, `homeHandler.js`):**

    - **Implement Playlist Actions:** In `homeHandler.js`, add event listeners for "View" and "Reorder" buttons on playlist items. "View" should navigate to Playlist View. "Reorder" should open Reorder Modal.
    - **Complete Song Upload Logic:** In `homeHandler.js` (`initHomePage`):
      - Replace stub `validateForm` with shared utility.
      - Implement UI updates (e.g., refresh song lists for "Create Playlist" form) and user feedback (success/error messages) after song upload.
    - **Complete Playlist Creation Logic:** In `homeHandler.js` (`initHomePage`):
      - Replace stub `validateForm` with shared utility.
      - Implement UI updates (refresh playlist list) and user feedback after playlist creation.

4.  **Playlist View (New: `playlistView.js`, `playlistHandler.js`):**

    - **Data Fetching Strategy:**
      - On selecting a playlist, `playlistHandler.js` needs to fetch song data. `GET /api/v1/playlists/{playlistId}/order` returns song IDs. Full song details (title, album image, artist, album name for sorting) will require an additional fetch (e.g., `GET /api/v1/songs` and client-side filtering/joining) or modification of `/order` endpoint to include details. **Clarify and implement efficient data fetching.**
    - **Render Playlist Songs:** `playlistView.js` to display songs (title, album image), 5 at a time, sorted by default (artist A-Z, album year asc.) or custom order.
    - **Implement Client-Side Pagination:** "Previous"/"Next" buttons in `playlistView.js`, logic in `playlistHandler.js`.
    - **Implement "Add Songs to Playlist" Form:**
      - `playlistView.js` to render the form.
      - `playlistHandler.js` to fetch user's songs (`GET /api/v1/songs`), handle selection, submit to `POST /api/v1/playlists/{playlistId}/songs`.
      - Refresh playlist view on success, handling `addedSongIds`/`duplicateSongIds`.
    - **Implement "Reorder" Button:** In `playlistView.js`, button to trigger the Reorder Modal.

5.  **Player View (New: `playerView.js`, `songPlayerHandler.js`):**

    - **Implement Song Detail Display:** `songPlayerHandler.js` to fetch details (`GET /api/v1/songs/{songId}`) when a song is selected. `playerView.js` to render details and album image (`GET /api/v1/songs/{songId}/image`).
    - **Implement Audio Player:** `playerView.js` to include HTML5 `<audio>`. `songPlayerHandler.js` to manage audio source (`GET /api/v1/songs/{songId}/audio`) and playback.

6.  **Reorder Playlist Modal (New: `reorderModal.js`, `playlistHandler.js`):**

    - **Implement Modal Structure:** `reorderModal.js` to create modal (adapt from `modal_test.html`).
    - **Implement Functionality (`playlistHandler.js` / `reorderModal.js`):**
      - Fetch/populate playlist songs (addressing data needs as in Playlist View).
      - Implement client-side drag-and-drop.
      - "Save Order" button: Collects new song ID order, validates, submits to `PUT /api/v1/playlists/{playlistId}/order`. Update UI on success.
      - "Cancel" button functionality.

7.  **All Songs View (`songsView.js`, `homeHandler.js` or New: `songsHandler.js`):**

    - **Complete Song Item Rendering:** In `songsView.js` (`renderAllUserSongsList`), complete the `createSongArticleElement` to properly render interactive song items (clickable to Player View).
    - **Implement Song Click Navigation:** Add event listeners in the handler (`initSongPage` or new `songsHandler.js`) for song items to navigate to Player View.
    - **Complete Song Upload on Songs Page:** Implement event listener and submission logic for the 'add-song-form' in `initSongPage` (in `homeHandler.js` or new `songsHandler.js`), including validation and UI feedback.
    - **Refactor Song Upload:** Consider creating a shared function for song upload submission logic used in both Home and Songs pages.

8.  **Navigation (`sharedComponents.js`, `app.js`):**

    - **Implement Consistent Navigation:** Ensure "Home", "Playlist", "Songs", "Logout" buttons in `index.html`'s navbar are consistently handled by `app.js` or a dedicated navigation module.
    - **Active State:** Highlight active navigation link.

9.  **Utilities (`formUtils.js`, `viewUtils.js`):**

    - **Consolidate Validation:** Move form validation functions (`validateField`, `validateForm`) from `loginHandler.js` to `formUtils.js`. Ensure all forms use this.
    - **Create `viewUtils.js`:** Move common DOM creation helpers (e.g., `createHeaderContainer`, `createParagraphElement`) from `homeView.js`/`songsView.js` to a new `viewUtils.js`.

10. **CSS and Styling:**

    - **Implement:** Styles for all new views/components (Playlist View, Player View, Reorder Modal).
    - **Consistency:** Ensure consistency with `global.css` and existing styles. Apply color palette from `design/specification.md`.

11. **Error Handling (Client-Side):**
    - **Standardize:** Implement a consistent, user-friendly error display mechanism across all forms, API interactions, and views (e.g., dedicated error message areas, toast notifications). Avoid generic `alert()`.

## III. Backend Security Review & Enhancements (OWASP Based)

1.  **Session Management:**

    - **Verify/Implement Secure Session Cookies:** Ensure session cookies are configured with `HttpOnly`, `Secure` (when HTTPS is enabled), and `SameSite=Strict` (or `Lax`) attributes. This is typically done in `web.xml` or programmatically via `ServletContext` during application initialization (e.g., in `AppContextListener`).
    - **Session Fixation on Signup (`UserApiServlet`):** Implement session invalidation (`oldSession.invalidate()`) before creating a new session for a newly registered user to prevent any possibility of using a pre-existing session ID.

2.  **Input Validation:**

    - **Review Validation Regex Strength:** Review regex patterns (from `AppContextListener`) for username, name, surname, playlist name, and standard text fields. Ensure they are sufficiently strict to prevent injection attacks (e.g., disallowing unexpected metacharacters) while meeting business requirements.
    - **Password Complexity:** Consider enforcing password complexity rules (e.g., requiring uppercase, lowercase, numbers, special characters) beyond just length, if not already part of the password validation regex/logic. This would be configured in `AppContextListener` and checked in `AuthApiServlet` and `UserApiServlet`.

3.  **Password Storage (`UserDAO`):**

    - **CRITICAL VULNERABILITY:** `UserDAO.createUser` stores passwords in plain text, and `UserDAO.checkCredentials` compares plain text passwords. This **MUST** be changed.
    - **Implement Secure Hashing:** Modify `UserDAO.createUser` to hash passwords using a strong, adaptive hashing algorithm (e.g., bcrypt, scrypt, Argon2) with a unique salt per user before database persistence.
    - **Implement Hash Verification:** Modify `UserDAO.checkCredentials` to retrieve the stored hash for the username and verify the provided password against this hash using the same algorithm.

4.  **File Upload Security (`AudioDAO`, `ImageDAO`):**

    - **Deployment Best Practice:** Ensure the base storage directory for `AudioDAO` and `ImageDAO` is configured outside the web application's publicly accessible deployment folder to prevent direct URL access to files. Document this requirement.
    - **Consideration:** Consider implementing virus scanning for uploaded files if the environment and requirements permit.

5.  **Access Control / Authorization (DAOs):**

    - **`SongDAO.deleteSong(int songId)`:** This method lacks a `userId` parameter for ownership check. If exposed to user actions (currently no API endpoint does), it **MUST** be modified to include user-based authorization to prevent users from deleting others' songs. If for internal/admin use only, its usage must be strictly controlled.
    - **Contextual Verification for `AlbumDAO` methods:** Ensure that internal calls to `AlbumDAO.findAlbumById(int idAlbum)` and `AlbumDAO.findAllAlbums()` (which do not filter by user) are always preceded by appropriate authorization checks on a related, user-owned entity.

6.  **Filesystem Cleanup (`PlaylistOrderDAO`):**

    - **`PlaylistOrderDAO.deletePlaylistOrder`:** Ensure this is called when a playlist is deleted via `PlaylistDAO.deletePlaylist` (or a service layer) to prevent orphaned order files.

7.  **Output Encoding (General):**

    - While primary responsibility for XSS prevention in an SPA lies with frontend rendering (using `textContent`, proper attribute encoding), ensure any server-generated error messages or data that _might_ be interpreted as HTML by older/misconfigured clients are appropriately encoded if not purely JSON.

8.  **Logging:**

    - **Review Logs:** Ensure sensitive information (e.g., full session IDs in non-debug modes, raw passwords beyond initial validation failure indication) is not excessively logged, following project guidelines.

9.  **Dependency Management:**
    - **Review Dependencies:** Regularly review project dependencies (`pom.xml`) for known vulnerabilities using tools like OWASP Dependency-Check. (This is an ongoing process, not a one-time fix).

## IV. General & Documentation

1.  **`design/specification.md` Alignment:**

    - **Playlist Data:** Critically review how playlist songs (with full details and order) are fetched for the Playlist View and Reorder Modal. The current API spec (`GET /playlists/{playlistId}/order` returning only IDs) implies significant client-side data management or additional fetches.
    - **Ensure Frontend Meets API Contracts:** As frontend components are built, continuously verify they correctly consume and send data according to API specifications.

2.  **Testing:**

    - Plan for thorough testing of all frontend components, interactions, and edge cases.
    - Backend tests should cover all API functionalities, data persistence logic, and security controls.

3.  **Code Quality & Consistency:**
    - Maintain consistent coding style, proper commenting, and modular design in both Java and JavaScript.
    - **Modularity:** Break down large handler functions if they become too complex. Separate view rendering, event handling, and API call logic more strictly.
