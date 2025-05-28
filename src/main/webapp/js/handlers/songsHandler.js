import { getSongGenres, getSongs } from "../apiService.js";
import { delay } from "../utils/delayUtils.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";
import { handleSongUploadSubmit } from './sharedFormHandlers.js';
import { extractUniqueAlbumSummaries, addAlbumSummaryIfNew } from '../utils/orderUtils.js';

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
    let albumSummaries = [];
    let genres = null;
    let genreError = null;

    // Define the success callback for the songs page song upload
    function songsPageSongUploadSuccess(newSongWithAlbum, appContainerForRender, currentSongsList) {

        // Update AllSongs section
        if (currentSongsList && Array.isArray(currentSongsList)) {
            currentSongsList.unshift(newSongWithAlbum);
        }
        const songListContainerOnSongsPage = appContainerForRender.querySelector('#songs .song-list');
        if (songListContainerOnSongsPage && currentSongsList) {
            renderAllUserSongsList(songListContainerOnSongsPage, currentSongsList, null);
        }

        // Update albumSummaries and re-render song upload form
        if (newSongWithAlbum.album) {
            const result = addAlbumSummaryIfNew(albumSummaries, newSongWithAlbum.album);
            if (result.wasAdded) {
                albumSummaries = result.updatedSummaries;
                // Re-render the song upload section with updated albumSummaries
                const formSection = appContainerForRender.querySelector('#add-song');
                if (formSection) {
                    renderSongUploadSectionOnSongsPage(formSection, genres, albumSummaries, genreError);
                    // Re-attach submit listener to the new form
                    const newForm = document.getElementById('add-song-form-songs-page');
                    if (newForm) {
                        newForm.addEventListener('submit', songFormSubmitHandler);
                    }
                }
            }
        }
    }

    // Fetch initial data for the page
    const [genreResult, songResult] = await Promise.all([
        _fetchGenresWithDelayInternal(),
        _fetchSongsWithDelayInternal()
    ]);

    genres = genreResult.genres;
    genreError = genreResult.error;

    // Populate the allUserSongs list and derive albumSummaries
    if (songResult.songs && Array.isArray(songResult.songs)) {
        allUserSongs.push(...songResult.songs);
        albumSummaries = extractUniqueAlbumSummaries(allUserSongs);
    }

    // Render the song upload form section using fetched genres and derived albumSummaries
    const songFormSectionOnSongsPage = appContainer.querySelector('#add-song');
    renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genres, albumSummaries, genreError);

    // Render the list of all user songs
    const songListContainer = appContainer.querySelector('#songs .song-list');
    renderAllUserSongsList(songListContainer, allUserSongs, songResult.error);

    // Add event listener for the song upload form on this page
    const songFormOnSongsPage = document.getElementById('add-song-form-songs-page');

    // Define the event handler separately to re-attach if the form is re-rendered
    async function songFormSubmitHandler(event) {
        const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
        const errorDivId = 'create-song-error-songs-page';
        await handleSongUploadSubmit(
            event,
            fieldIds,
            errorDivId,
            songsPageSongUploadSuccess,
            appContainer,
            allUserSongs
        );
    }

    if (songFormOnSongsPage) {
        songFormOnSongsPage.addEventListener('submit', songFormSubmitHandler);
    }
}
