const API_BASE_URL = 'api/v1';

/**
 * Private helper function to make API calls.
 * @param {string} endpoint - The API endpoint (e.g., '/users').
 * @param {object} options - Fetch options (method, body, headers).
 * @param {boolean} isFormData - True if the body is FormData.
 * @returns {Promise<any>} - The JSON response from the API.
 * @throws {object} - An error object with status, message, details, and response.
 */
async function _fetchApi(endpoint, options = {}, isFormData = false) {
    const url = `${API_BASE_URL}${endpoint}`;
    const fetchOptions = { ...options };

    fetchOptions.headers = { ...fetchOptions.headers };

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
                console.error("Fail to parse in json the response");
            }
            throw {
                status: response.status,
                message: errorData.error || response.statusText || 'Unknown API error',
                details: errorData,
                response: response
            };
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
            throw {
                status: 0,
                message: error.message || 'Network error or request failed to send',
                details: {},
                isNetworkError: true
            };
        }
    }
}

// --- Authentication & User ---
export async function checkAuthStatus() {
    return _fetchApi('/auth/me', { method: 'GET' });
}

export async function login(credentials) {
    return _fetchApi('/auth/login', { method: 'POST', body: credentials });
}

export async function logout() {
    return _fetchApi('/auth/logout', { method: 'POST' });
}

export async function signup(userData) {
    return _fetchApi('/users', { method: 'POST', body: userData });
}

// --- Playlists ---
export async function getPlaylists() {
    return _fetchApi('/playlists', { method: 'GET' });
}

export async function createPlaylist(playlistData) {
    return _fetchApi('/playlists', { method: 'POST', body: playlistData });
}

export async function getPlaylistSongOrder(playlistId) {
    return _fetchApi(`/playlists/${playlistId}/order`, { method: 'GET' });
}

export async function addSongsToPlaylist(playlistId, songIdsData) {
    return _fetchApi(`/playlists/${playlistId}/songs`, { method: 'POST', body: songIdsData });
}

export async function updatePlaylistOrder(playlistId, orderedSongIdsData) {
    return _fetchApi(`/playlists/${playlistId}/order`, { method: 'PUT', body: orderedSongIdsData });
}

// --- Songs ---
export async function getSongs() {
    return _fetchApi('/songs', { method: 'GET' });
}

export async function uploadSong(formData) {
    return _fetchApi('/songs', { method: 'POST', body: formData }, true);
}

export async function getSongDetails(songId) {
    return _fetchApi(`/songs/${songId}`, { method: 'GET' });
}

export async function getSongGenres() {
    return _fetchApi('/songs/genres', { method: 'GET' });
}

// --- URL Builders ---
export function getSongImageURL(songId) {
    return `${API_BASE_URL}/songs/${songId}/image`;
}

export function getSongAudioURL(songId) {
    return `${API_BASE_URL}/songs/${songId}/audio`;
}
