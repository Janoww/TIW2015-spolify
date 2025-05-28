import { getSongGenres, getSongs } from "../apiService.js";
import { delay } from "../utils/delayUtils.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";
import { handleSongUploadSubmit } from './sharedFormHandlers.js';

/**
 * Fetches song genres from the API with a built-in delay.
 * @returns {Promise<{genres: Array<object>|null, error: object|null}>} A promise that resolves to an object containing the fetched genres and an error object (null if successful).
 */
async function _fetchGenresWithDelayInternal() {
    try {
        const [genres,] = await Promise.all([getSongGenres(), delay()]);
        return { genres, error: null };
    } catch (error) {
        console.error(`Failed to load genres for Songs Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        return { genres: null, error };
    }
}

/**
 * Fetches all user songs from the API with a built-in delay.
 * @returns {Promise<{songs: Array<object>, error: object|null}>} A promise that resolves to an object containing the fetched songs (empty array on error) and an error object (null if successful).
 */
async function _fetchSongsWithDelayInternal() {
    try {
        const [songs,] = await Promise.all([getSongs(), delay()]);
        return { songs, error: null };
    } catch (error) {
        console.error(`Error loading all user songs for Songs page: Status ${error.status}, Message: ${error.message}`, error.details || '');
        return { songs: [], error };
    }
}

/**
 * Initializes the Songs page.
 * This involves rendering the basic view, fetching all user songs and available genres,
 * populating the song list, setting up the song upload form, and attaching necessary event listeners.
 * @param {HTMLElement} appContainer - The main HTML element in which the songs page content will be rendered.
 */
export async function initSongPage(appContainer) {
    renderSongsView(appContainer);

    let allUserSongs = [];

    // Define the success callback for the songs page song upload
    function songsPageSongUploadSuccess(newSong, appContainerForRender, currentSongsList) {
        if (currentSongsList && Array.isArray(currentSongsList)) {
            currentSongsList.unshift(newSong);
        }
        const songListContainerOnSongsPage = appContainerForRender.querySelector('#songs .song-list');
        if (songListContainerOnSongsPage && currentSongsList) {
            renderAllUserSongsList(songListContainerOnSongsPage, currentSongsList, null);
        }
    }

    // Fetch initial data for the page
    const [genreResult, songResult] = await Promise.all([
        _fetchGenresWithDelayInternal(),
        _fetchSongsWithDelayInternal()
    ]);

    // Render the song upload form section using fetched genres
    const songFormSectionOnSongsPage = appContainer.querySelector('#add-song');
    renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genreResult.genres, genreResult.error);

    // Populate the allUserSongs list and render it
    if (songResult.songs && Array.isArray(songResult.songs)) {
        allUserSongs.push(...songResult.songs);
    }
    const songListContainer = appContainer.querySelector('#songs .song-list');
    renderAllUserSongsList(songListContainer, allUserSongs, songResult.error);

    // Add event listener for the song upload form on this page
    // Default form ID from renderSongUploadSectionOnSongsPage is 'add-song-form-songs-page'
    const songFormOnSongsPage = document.getElementById('add-song-form-songs-page');
    if (songFormOnSongsPage) {
        const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
        // Default error div ID from renderSongUploadSectionOnSongsPage is 'create-song-error-songs-page'
        const errorDivId = 'create-song-error-songs-page';

        songFormOnSongsPage.addEventListener('submit', async (event) => {
            await handleSongUploadSubmit(
                event,
                fieldIds,
                errorDivId,
                songsPageSongUploadSuccess,
                appContainer,
                allUserSongs
            );
        });
    }
}
