import { getSongGenres, getSongs } from "../apiService.js";
import { delay } from "../utils/delayUtils.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";

/**
 * 
 * @param {HTMLElement} appContainer 
 */
export async function initSongPage(appContainer) {
    // Render the basic songs view structure, including a placeholder for its song form
    renderSongsView(appContainer);

    await Promise.all([_completeSongForm(appContainer), _completeSongList(appContainer)]);

    // TODO: Add event listener for the 'add-song-form' on this page.

}

async function _completeSongForm(appContainer) {
    const songFormSectionOnSongsPage = appContainer.querySelector('#add-song');
    let genresForSongPage = null;
    let genreErrorForSongPage = null;
    try {
        [genresForSongPage,] = await Promise.all([getSongGenres(), delay()]);
    } catch (error) {
        console.error(`Failed to load genres for Songs Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreErrorForSongPage = error;
    }

    renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genresForSongPage, genreErrorForSongPage);
}

async function _completeSongList(appContainer) {
    const songListContainer = appContainer.querySelector('#songs .song-list');
    let allUserSongs = null;
    let songsError = null;
    try {
        [allUserSongs,] = await Promise.all([getSongs(), delay()]);
    } catch (error) {
        console.error(`Error loading all user songs for Songs page: Status ${error.status}, Message: ${error.message}`, error.details || '');
        songsError = error;
    }

    renderAllUserSongsList(songListContainer, allUserSongs, songsError);
}