import { createFormField } from '../utils/formUtils.js'; // Keep if other forms are built here, or for song item rendering
import { createSongUploadFormElement } from './sharedComponents.js';

// Helper function to create a title (similar to homeView, consider shared viewUtils.js)
function createHeaderContainer(titleText, size = 'h2') {
    const h = document.createElement(size);
    h.textContent = titleText;
    return h;
}

/**
 * Renders the basic structure of the Songs page.
 * @param {HTMLElement} appContainer The main application container.
 */
export function renderSongsView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

    const songGrid = document.createElement('div');
    songGrid.className = 'grid song-grid';

    // Section 1: All Songs
    const allSongsSection = document.createElement('section');
    allSongsSection.id = 'songs';
    allSongsSection.appendChild(createHeaderContainer('All Songs'));

    const songListDiv = document.createElement('div');
    songListDiv.className = 'song-list';

    // Placeholder for song items
    const loadingSongsP = document.createElement('p');
    loadingSongsP.id = 'all-songs-loader-message';
    loadingSongsP.textContent = 'Loading songs...';
    songListDiv.appendChild(loadingSongsP);
    allSongsSection.appendChild(songListDiv);
    songGrid.appendChild(allSongsSection);

    // Section 2: Upload New Song
    const uploadSongSection = document.createElement('section');
    uploadSongSection.id = 'add-song';
    uploadSongSection.appendChild(createHeaderContainer('Upload New Song'));

    // Placeholder for the song form
    const formLoaderP = document.createElement('p');
    formLoaderP.id = 'song-form-loader-message-songs-page';
    formLoaderP.textContent = 'Loading song form...';
    uploadSongSection.appendChild(formLoaderP);
    songGrid.appendChild(uploadSongSection);

    appContainer.appendChild(songGrid);
}

/**
 * Renders the actual song upload form into its designated section.
 * @param {HTMLElement} sectionContainer The container element for the song upload form.
 * @param {Array<Object>|null} genres Fetched genres.
 * @param {Object|null} error Error object if genre fetching failed.
 */
export function renderSongUploadSectionOnSongsPage(sectionContainer, genres, error = null) {
    const loaderMessage = sectionContainer.querySelector('#song-form-loader-message-songs-page');
    if (loaderMessage) loaderMessage.remove();

    const existingFormOrError = sectionContainer.querySelector('form, p.general-error-message');
    if (existingFormOrError) existingFormOrError.remove();

    const formElement = createSongUploadFormElement('add-song-form', genres, error);
    sectionContainer.appendChild(formElement);
    // TODO: Event listener for this form ('add-song-form') submission needs to be attached in the handler.
}

/**
 * Renders the list of all user songs.
 * @param {HTMLElement} songListContainer The div where song items should be appended.
 * @param {Array<Object>|null} songs Array of songWithAlbum objects.
 * @param {Object|null} error Error object if song fetching failed.
 */
export function renderAllUserSongsList(songListContainer, songs, error = null) {
    const loaderMessage = songListContainer.querySelector('#all-songs-loader-message');
    if (loaderMessage) loaderMessage.remove();

    if (error) {
        const errorP = document.createElement('p');
        errorP.className = 'general-error-message';
        errorP.textContent = 'Failed to load songs. Please try refreshing.';
        songListContainer.appendChild(errorP);
        return;
    }

    if (!songs || songs.length === 0) {
        const noSongsP = document.createElement('p');
        noSongsP.textContent = 'No songs found. Upload your first song!';
        songListContainer.appendChild(noSongsP);
        return;
    }

    songs.forEach(songWithAlbum => {
        // TODO: Implement createSongArticleElement similar to homeView.js or songs_mockup.html structure
        // For now, a simple placeholder:
        const songArticle = document.createElement('article');
        songArticle.className = 'song-item';

        const img = document.createElement('img');

        console.log(songWithAlbum.album);
        // For now, using placeholder as image path construction needs more info (e.g., base URL for images)


        img.src = `api/v1/songs/${songWithAlbum.song.idSong}/image`;
        img.alt = songWithAlbum.song.title || "Song cover";
        img.onerror = () => {
            img.src = 'images/image_placeholder.png';
        };

        const metadataDiv = document.createElement('div');
        metadataDiv.className = 'song-metadata';

        const titleH3 = document.createElement('h3');
        titleH3.textContent = songWithAlbum.song.title;
        metadataDiv.appendChild(titleH3);

        const artistAlbumP = document.createElement('p');
        artistAlbumP.textContent = `${songWithAlbum.album.artist} • ${songWithAlbum.album.name}`;
        metadataDiv.appendChild(artistAlbumP);

        const genreYearP = document.createElement('p');
        genreYearP.textContent = `${songWithAlbum.song.genre} • ${songWithAlbum.song.year}`;
        metadataDiv.appendChild(genreYearP);

        songArticle.appendChild(img);
        songArticle.appendChild(metadataDiv);
        songListContainer.appendChild(songArticle);
    });
}
