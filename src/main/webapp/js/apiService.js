const API_BASE_URL = 'api/v1';

/**
 * @typedef {object} User
 * @property {string} username - The user's username.
 * @property {string} name - The user's first name.
 * @property {string} surname - The user's last name.
 */

/**
 * @typedef {object} LoginCredentials
 * @property {string} username - The username for login.
 * @property {string} password - The password for login.
 */

/**
 * @typedef {object} UserData
 * @property {string} username - The username for signup.
 * @property {string} password - The password for signup.
 * @property {string} name - The user's first name.
 * @property {string} surname - The user's last name.
 */

/**
 * @typedef {object} Playlist
 * @property {number} idPlaylist - The unique ID of the playlist.
 * @property {string} name - The name of the playlist.
 * @property {string} birthday - The creation timestamp of the playlist (ISO 8601 format).
 * @property {string} idUser - The ID of the user who owns the playlist.
 */

/**
 * @typedef {object} PlaylistCreationData
 * @property {string} name - The name for the new playlist.
 * @property {Array<number>} [songIds] - Optional array of song IDs to add to the playlist upon creation.
 */

/**
 * @typedef {object} AddSongsData
 * @property {Array<number>} songIds - Array of song IDs to add to the playlist.
 */

/**
 * @typedef {object} AddSongsToPlaylistResponse
 * @property {string} message - A message confirming the processing of songs.
 * @property {Array<number>} addedSongIds - IDs of songs successfully added.
 * @property {Array<number>} duplicateSongIds - IDs of songs already present in the playlist.
 */

/**
 * @typedef {object} AlbumDetails
 * @property {number} idAlbum - The unique ID of the album.
 * @property {string} name - The name of the album.
 * @property {number} year - The release year of the album.
 * @property {string} artist - The artist of the album.
 * @property {string} [image] - Optional path or URL to the album cover image.
 * @property {string} idUser - The ID of the user who owns the album.
 */

/**
 * @typedef {object} SongWithAlbum
 * @property {number} idSong - The unique ID of the song.
 * @property {string} title - The title of the song.
 * @property {number} idAlbum - The ID of the album this song belongs to.
 * @property {number} year - The release year of the song.
 * @property {string} [genre] - The genre of the song.
 * @property {string} audioFile - The filename or path to the audio file.
 * @property {string} idUser - The ID of the user who owns the song.
 * @property {AlbumDetails} album - The details of the album this song belongs to.
 */

/**
 * @typedef {object} Genre
 * @property {string} name - The name of the genre (e.g., "ROCK").
 * @property {string} description - A description of the genre (e.g., "Rock Music").
 */

/**
 * @typedef {object} FetchOptions
 * @property {string} [method] - The HTTP method (e.g., 'GET', 'POST').
 * @property {object} [headers] - HTTP headers.
 * @property {object|string|FormData} [body] - The request body.
 */

/**
 * @typedef {Error} ApiError
 * @property {number} status - The HTTP status code of the error response.
 * @property {string} message - The error message.
 * @property {object} details - Additional details about the error, often the parsed JSON error response.
 * @property {Response} response - The raw Fetch API Response object.
 * @property {boolean} [isNetworkError] - True if the error is a network error or fetch setup error.
 */

/**
 * Private helper function to make API calls.
 * @param {string} endpoint - The API endpoint (e.g., '/users').
 * @param {FetchOptions} [options={}] - Fetch options (method, body, headers).
 * @param {boolean} [isFormData=false] - True if the body is FormData.
 * @returns {Promise<object|string>} - The JSON response from the API or text for non-JSON responses.
 * @throws {ApiError} - An error object with status, message, details, and response.
 */
async function _fetchApi(endpoint, options = {}, isFormData = false) {
    const url = `${API_BASE_URL}${endpoint}`;
    const fetchOptions = {...options};

    fetchOptions.headers = {...fetchOptions.headers};

    if (!isFormData) {
        fetchOptions.headers['Accept'] = 'application/json';
        if (fetchOptions.body && typeof fetchOptions.body === 'object') {
            fetchOptions.headers['Content-Type'] = 'application/json';
            fetchOptions.body = JSON.stringify(fetchOptions.body);
        }
    } else {
        fetchOptions.headers['Accept'] = 'application/json';
    }

    try {
        const response = await fetch(url, fetchOptions);

        if (!response.ok) {
            let errorData = {};
            try {
                errorData = await response.json();
            } catch (e) {
                console.error(`Fail to parse in json the response: ${e}`);
            }
            const error = new Error(errorData.error || response.statusText || 'Unknown API error');
            error.status = response.status;
            error.details = errorData;
            error.response = response;
            throw error;
        }

        const contentType = response.headers.get("content-type");
        if (response.status === 204 || !contentType?.includes("application/json")) {
            return response.text();
        }

        return await response.json();

    } catch (error) {
        if (error.status) {
            throw error;
        } else {
            console.error('Network or fetch setup error:', error);
            const networkError = new Error(error.message || 'Network error or request failed to send');
            networkError.status = 0;
            networkError.details = {};
            networkError.isNetworkError = true;
            throw networkError;
        }
    }
}

// --- Authentication & User ---

/**
 * Checks the authentication status of the current user.
 * @returns {Promise<User>} A promise that resolves to the current user's data if authenticated.
 * @throws {ApiError} If the user is not authenticated or if there's a server error.
 */
export async function checkAuthStatus() {
    return _fetchApi('/auth/me', {method: 'GET'});
}

/**
 * Logs in a user with the provided credentials.
 * @param {LoginCredentials} credentials - The user's login credentials.
 * @returns {Promise<User>} A promise that resolves to the authenticated user's data.
 * @throws {ApiError} If login fails (e.g., invalid credentials, server error).
 */
export async function login(credentials) {
    return _fetchApi('/auth/login', {method: 'POST', body: credentials});
}

/**
 * Logs out the currently authenticated user.
 * @returns {Promise<{message: string}>} A promise that resolves to a success message.
 * @throws {ApiError} If logout fails or if there's a server error.
 */
export async function logout() {
    return _fetchApi('/auth/logout', {method: 'POST'});
}

/**
 * Registers a new user.
 * @param {UserData} userData - The data for the new user.
 * @returns {Promise<User>} A promise that resolves to the newly created user's data.
 * @throws {ApiError} If registration fails (e.g., username exists, validation error, server error).
 */
export async function signup(userData) {
    return _fetchApi('/users', {method: 'POST', body: userData});
}

// --- Playlists ---

/**
 * Fetches all playlists for the authenticated user.
 * @returns {Promise<Array<Playlist>>} A promise that resolves to an array of the user's playlists.
 * @throws {ApiError} If fetching fails or if there's a server error.
 */
export async function getPlaylists() {
    return _fetchApi('/playlists', {method: 'GET'});
}

/**
 * Creates a new playlist.
 * @param {PlaylistCreationData} playlistData - The data for the new playlist.
 * @returns {Promise<Playlist>} A promise that resolves to the newly created playlist object.
 * @throws {ApiError} If creation fails (e.g., validation error, server error).
 */
export async function createPlaylist(playlistData) {
    return _fetchApi('/playlists', {method: 'POST', body: playlistData});
}

/**
 * Fetches the current order of songs for a specific playlist.
 * @param {number|string} playlistId - The ID of the playlist.
 * @returns {Promise<Array<number>>} A promise that resolves to an array of song IDs representing the order.
 * @throws {ApiError} If fetching fails (e.g., playlist not found, server error).
 */
export async function getPlaylistSongOrder(playlistId) {
    return _fetchApi(`/playlists/${playlistId}/order`, {method: 'GET'});
}

/**
 * Adds one or more songs to an existing playlist.
 * @param {number|string} playlistId - The ID of the playlist.
 * @param {AddSongsData} songIdsData - An object containing an array of song IDs to add.
 * @returns {Promise<AddSongsToPlaylistResponse>} A promise that resolves to an object detailing added and duplicate songs.
 * @throws {ApiError} If adding songs fails (e.g., playlist/song not found, server error).
 */
export async function addSongsToPlaylist(playlistId, songIdsData) {
    return _fetchApi(`/playlists/${playlistId}/songs`, {method: 'POST', body: songIdsData});
}

/**
 * Updates the order of songs in a specific playlist.
 * @param {number|string} playlistId - The ID of the playlist.
 * @param {Array<number>} orderedSongIdsData - An array of song IDs in the desired new order.
 * @returns {Promise<Array<number>>} A promise that resolves to an array of song IDs confirming the new order.
 * @throws {ApiError} If updating order fails (e.g., invalid data, playlist not found, server error).
 */
export async function updatePlaylistOrder(playlistId, orderedSongIdsData) {
    return _fetchApi(`/playlists/${playlistId}/order`, {method: 'PUT', body: orderedSongIdsData});
}

// --- Songs ---

/**
 * Fetches all songs for the authenticated user.
 * @returns {Promise<Array<SongWithAlbum>>} A promise that resolves to an array of songs with their album details.
 * @throws {ApiError} If fetching fails or if there's a server error.
 */
export async function getSongs() {
    return _fetchApi('/songs', {method: 'GET'});
}

/**
 * Uploads a new song.
 * @param {HTMLFormElement} formElement - The HTML form element containing the song data.
 * @returns {Promise<SongWithAlbum>} A promise that resolves to the newly created song with its album details.
 * @throws {ApiError} If upload fails (e.g., validation error, server error).
 */
export async function uploadSong(formElement) {
    const formData = new FormData();

    formData.append('title', formElement['song-title'].value);
    formData.append('albumTitle', formElement['album-title'].value);
    formData.append('albumArtist', formElement['album-artist'].value);
    formData.append('albumYear', formElement['album-year'].value);
    formData.append('genre', formElement['song-genre'].value);

    // File fields - only append if a file is selected
    if (formElement['album-image'].files && formElement['album-image'].files.length > 0) {
        formData.append('albumImage', formElement['album-image'].files[0]);
    }
    if (formElement['song-audio'].files && formElement['song-audio'].files.length > 0) {
        formData.append('audioFile', formElement['song-audio'].files[0]);
    }

    return _fetchApi('/songs', {method: 'POST', body: formData}, true);
}

/**
 * Fetches details for a specific song.
 * @param {number|string} songId - The ID of the song.
 * @returns {Promise<SongWithAlbum>} A promise that resolves to the song's details with its album information.
 * @throws {ApiError} If fetching fails (e.g., song not found, server error).
 */
export async function getSongDetails(songId) {
    return _fetchApi(`/songs/${songId}`, {method: 'GET'});
}

/**
 * Fetches all available song genres.
 * @returns {Promise<Array<Genre>>} A promise that resolves to an array of genre objects.
 * @throws {ApiError} If fetching fails or if there's a server error.
 */
export async function getSongGenres() {
    return _fetchApi('/songs/genres', {method: 'GET'});
}

// --- URL Builders ---

/**
 * Constructs the URL for a song's album image.
 * @param {number|string} songId - The ID of the song.
 * @returns {string} The URL to fetch the song's album image.
 */
export function getSongImageURL(songId) {
    return `${API_BASE_URL}/songs/${songId}/image`;
}

/**
 * Constructs the URL for a song's audio file.
 * @param {number|string} songId - The ID of the song.
 * @returns {string} The URL to fetch the song's audio file.
 */
export function getSongAudioURL(songId) {
    return `${API_BASE_URL}/songs/${songId}/audio`;
}
