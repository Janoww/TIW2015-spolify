import { createSongUploadFormElement } from './sharedComponents.js';
import { getSongImageURL } from '../apiService.js';
import { createHeaderContainer, createParagraphElement, createElement, createLoaderContainer } from '../utils/viewUtils.js';
import { getSongsOrdered } from '../utils/orderUtils.js';

/**
 * Renders the basic structure of the Songs page.
 * @param {HTMLElement} appContainer The main application container.
 */
export function renderSongsView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

    const songGrid = createElement('div', { className: 'grid song-grid' });

    // Section 1: All Songs
    const allSongsSection = createElement('section', { id: 'songs' });
    allSongsSection.appendChild(createHeaderContainer('All Songs', 'h2'));

    const songListDiv = createElement('div', { className: 'song-list' });

    const allSongsLoader = createLoaderContainer('Loading songs...', 'all-songs-loader-message')
    songListDiv.appendChild(allSongsLoader);
    allSongsSection.appendChild(songListDiv);
    songGrid.appendChild(allSongsSection);

    // Section 2: Upload New Song
    const uploadSongSection = createElement('section', { id: 'add-song' });
    uploadSongSection.appendChild(createHeaderContainer('Upload New Song', 'h2'));

    const formLoader = createLoaderContainer('Loading song form...', 'song-form-loader-message-songs-page')
    uploadSongSection.appendChild(formLoader);
    songGrid.appendChild(uploadSongSection);

    appContainer.appendChild(songGrid);
}

/**
 * Renders the actual song upload form into its designated section.
 * @param {HTMLElement} sectionContainer The container element for the song upload form.
 * @param {Array<Object>|null} genres Fetched genres.
 * @param {Object|null} error Error object if genre fetching failed.
 * @param {string} formId The ID for the form element.
 * @param {string} errorDivId The ID for the error message div.
 */
export function renderSongUploadSectionOnSongsPage(sectionContainer, genres, error = null, formId = 'add-song-form-songs-page', errorDivId = 'create-song-error-songs-page') {
    const loaderMessage = sectionContainer.querySelector('#song-form-loader-message-songs-page');
    if (loaderMessage) loaderMessage.remove();

    // Remove any existing form or error message if re-rendering
    sectionContainer.querySelectorAll('form, div.general-error-message').forEach(el => el.remove());

    const formElement = createSongUploadFormElement(formId, genres, error);

    const errorDiv = createElement('div', { className: 'general-error-message', id: errorDivId });

    sectionContainer.appendChild(errorDiv);
    sectionContainer.appendChild(formElement);
}

/**
 * Creates an article element for a single song.
 * @param {Object} songWithAlbum - An object containing song and album details.
 * @returns {HTMLElement} The created article element for the song.
 */
function createSongArticleElement(songWithAlbum) {
    const songArticle = createElement('article', { className: 'song-item' });
    const playButton = createElement('button', { id: `song-action-${songWithAlbum.song.idSong}` });
    playButton.style.display = 'none'; // Or handle visibility via CSS classes

    const label = createElement('label', {
        className: 'song-metadata',
        attributes: { htmlFor: `song-action-${songWithAlbum.song.idSong}` }
    });

    const img = document.createElement('img');
    img.src = getSongImageURL(songWithAlbum.song.idSong);
    img.alt = songWithAlbum.song.title || "Song cover";
    img.onerror = () => {
        img.src = 'images/image_placeholder.png';
    };

    const metadataDiv = createElement('div', { className: 'song-text' });
    const titleH3 = createHeaderContainer(songWithAlbum.song.title, 'h3');
    const artistAlbumP = createParagraphElement(`${songWithAlbum.album.artist} • ${songWithAlbum.album.name}`);
    const genreYearP = createParagraphElement(`${songWithAlbum.song.genre} • ${songWithAlbum.song.year}`);

    metadataDiv.appendChild(titleH3);
    metadataDiv.appendChild(artistAlbumP);
    metadataDiv.appendChild(genreYearP);

    label.appendChild(img);
    label.appendChild(metadataDiv);

    songArticle.appendChild(playButton);
    songArticle.appendChild(label);

    return songArticle;
}

/**
 * Renders the list of all user songs.
 * @param {HTMLElement} songListContainer The div where song items should be appended.
 * @param {Array<Object>|null} songs Array of songWithAlbum objects.
 * @param {Object|null} error Error object if song fetching failed.
 */
export function renderAllUserSongsList(songListContainer, songs, error = null) {
    songListContainer.innerHTML = '';

    if (error) {
        const errorP = createElement('div', { textContent: 'Failed to load songs. Please try refreshing.', className: 'general-error-message' });
        songListContainer.appendChild(errorP);
        return;
    }

    if (!songs || songs.length === 0) {
        const noSongsP = createParagraphElement('No songs found. Upload your first song!');
        songListContainer.appendChild(noSongsP);
        return;
    }

    const songArticles = getSongsOrdered(songs)?.map(createSongArticleElement);

    songListContainer.append(...songArticles)
}
