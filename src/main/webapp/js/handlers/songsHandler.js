import { getSongGenres as apiGetSongGenres, getSongs } from "../apiService.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";


export async function initSongPage(appContainer) {
    // Render the basic songs view structure, including a placeholder for its song form
    renderSongsView(appContainer);

    const songFormSectionOnSongsPage = appContainer.querySelector('#add-song');
    let genresForSongPage = null;
    let genreErrorForSongPage = null;
    try {
        genresForSongPage = await apiGetSongGenres();
    } catch (error) {
        console.error(`Failed to load genres for Songs Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreErrorForSongPage = error;
    }

    renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genresForSongPage, genreErrorForSongPage);
    // TODO: Add event listener for the 'add-song-form' on this page.
    // Load and render all user songs in the '#songs .song-list'
    const songListContainer = appContainer.querySelector('#songs .song-list');
    let allUserSongs = null;
    let songsError = null;
    try {
        allUserSongs = await getSongs();
    } catch (error) {
        console.error(`Error loading all user songs for Songs page: Status ${error.status}, Message: ${error.message}`, error.details || '');
        songsError = error;
    }
    renderAllUserSongsList(songListContainer, allUserSongs, songsError);

}