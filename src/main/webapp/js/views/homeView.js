import {createFormField} from '../utils/formUtils.js';
import {createSongArticleWithCheckboxElement, createSongUploadFormElement} from './sharedComponents.js'; // Added createSongArticleWithCheckboxElement
import {createElement, createHeaderContainer, createParagraphElement} from '../utils/viewUtils.js';
import {getSongsOrdered} from '../utils/orderUtils.js';

/**
 * Creates an HTML article element representing a playlist item.
 * Includes playlist name, creation date, and action buttons (View, Reorder).
 * @param {Object} playlist - The playlist object.
 * @param {string} playlist.name - The name of the playlist.
 * @param {string} playlist.birthday - The ISO string representation of the playlist's creation date.
 * @param {number} playlist.idPlaylist - The ID of the playlist.
 * @returns {HTMLElement} The created article element for the playlist.
 */
function createPlaylistArticle(playlist) {
    const article = createElement('article', {className: 'playlist-item'});

    const divInfo = createElement('div', {className: 'playlist-info'});
    divInfo.appendChild(createHeaderContainer(playlist.name, 'h3'));
    divInfo.appendChild(createParagraphElement('Created: ' + formatPlaylistDate(playlist.birthday)));
    article.appendChild(divInfo);

    const divActions = createElement('div', {className: 'playlist-actions'});

    const buttonView = createElement('button', {
        textContent: 'View',
        className: 'styled-button view-playlist-button',
        dataset: {playlistId: playlist.idPlaylist}
    });
    const buttonReorder = createElement('button', {
        textContent: 'Reorder',
        className: 'styled-button reorder-playlist-button',
        dataset: {playlistId: playlist.idPlaylist}
    });

    divActions.appendChild(buttonView);
    divActions.appendChild(buttonReorder);
    article.appendChild(divActions);

    return article;
}

/**
 * Renders the basic structure of the home page.
 * Sets up sections for user playlists, new song upload, new playlist creation, and a modal for reordering.
 * @param {HTMLElement} appContainer - The main container element of the application where the home view will be rendered.
 */
export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

    // The Home Page will be divides in a 3 section horizontal grid
    const homeGridDiv = createElement('div', {className: 'grid home-grid'});

    // Section 1: User playlist
    const myPlaylistsSection = createElement('section', {id: 'user-playlists-section'});
    myPlaylistsSection.appendChild(createHeaderContainer('My Playlists', 'h2'));
    const myPlaylistList = createElement('div', {className: 'playlist-list'});
    myPlaylistsSection.appendChild(myPlaylistList); // The list is populated in the homeHandler by renderPlaylists()
    homeGridDiv.appendChild(myPlaylistsSection);

    // Section 2: New Song Upload
    const newSongSection = createElement('section', {id: 'add-song-section'});
    newSongSection.appendChild(createHeaderContainer('Upload New Song', 'h2'));
    const loadingP = createParagraphElement('Loading song form...', 'song-form-loader-message');
    newSongSection.appendChild(loadingP);
    homeGridDiv.appendChild(newSongSection);

    // Section 3: New Playlist Creation
    const newPlaylistSection = createElement('section', {id: 'create-playlist-section'});
    newPlaylistSection.appendChild(createHeaderContainer('Create New Playlist', 'h2'));
    const newPlaylistForm = createElement('form', {id: 'create-playlist-form', attributes: {noValidate: true}});
    newPlaylistForm.appendChild(createFormField('new-playlist-title', 'Playlist Title:', 'text', 'new-playlist-title', true));
    newPlaylistForm.appendChild(createHeaderContainer('Select Songs to Add:', 'h3'));
    const songListDiv = createElement('div', {className: 'song-list'});
    newPlaylistForm.appendChild(songListDiv);
    const newPlaylistSendButton = createElement('button', {
        textContent: 'Create Playlist',
        className: 'styled-button',
        attributes: {type: 'submit'}
    });
    newPlaylistForm.appendChild(newPlaylistSendButton);
    const errorDiv = createElement('div', {className: 'general-error-message', id: 'create-playlist-error'});
    newPlaylistSection.appendChild(errorDiv);
    newPlaylistSection.appendChild(newPlaylistForm);
    homeGridDiv.appendChild(newPlaylistSection);

    appContainer.appendChild(homeGridDiv);

    // Section 4: Modal Popup
    const modalDiv = createElement('div', {className: 'modal', id: 'reorderModal'});
    modalDiv.style.display = 'none';
    const modalContent = createReorderPopup();
    modalDiv.appendChild(modalContent);


    appContainer.appendChild(modalDiv);

}

/**
 * Renders the list of songs (with checkboxes) in the "Create New Playlist" section.
 * Songs are ordered before rendering.
 * @param {HTMLElement} appContainer - The main application container, used to find the song list container.
 * @param {Array<Object>} songWithAlbums - An array of song objects, each potentially including album details.
 */
export function renderSongs(appContainer, songWithAlbums) {
    const songListContainer = appContainer.querySelector('.song-list');

    songListContainer.innerHTML = '';

    if (songWithAlbums) {
        songWithAlbums = getSongsOrdered(songWithAlbums);
        const songArticles = songWithAlbums.map(song => createSongArticleWithCheckboxElement(song));
        songListContainer.append(...songArticles);
    }
}

/**
 * Renders the list of user's playlists in the "My Playlists" section.
 * @param {HTMLElement} appContainer - The main application container, used to find the playlist list container.
 * @param {Array<Object>} playlists - An array of playlist objects to render.
 */
export function renderPlaylists(appContainer, playlists) {

    const playlistListDiv = appContainer.querySelector('.playlist-list');

    playlistListDiv.innerHTML = '';

    if (playlists) {
        playlists.forEach(pl => {
            const article = createPlaylistArticle(pl);
            playlistListDiv.appendChild(article);
        })
    }
}

/**
 * Renders the song upload form section.
 * Clears previous content and appends a new form and an error display area.
 * @param {HTMLElement} sectionContainer - The HTML element where the song upload form will be rendered.
 * @param {Array<string>} genres - An array of available song genres.
 * @param {Array<Object>} albumSummaries - An array of album summary objects (e.g., {idAlbum, name}) for selection.
 * @param {?Error} [error=null] - An optional error object if there was an issue loading form data (e.g., genres).
 * @param {string} [formId='add-song-form-home'] - The ID to be assigned to the created form element.
 * @param {string} [errorDivId='create-song-error'] - The ID for the error message div associated with this form.
 */
export function renderSongUploadSection(sectionContainer, genres, albumSummaries, error = null, formId = 'add-song-form-home', errorDivId = 'create-song-error') {
    // Clear previous content (e.g., the "Loading form..." message)
    ['song-form-loader-message', formId, errorDivId].forEach(el => sectionContainer.querySelector(`#${el}`)?.remove())

    // Remove any existing form or error message if re-rendering

    const formElement = createSongUploadFormElement(formId, genres, albumSummaries, error);

    const errorDiv = createElement('div', {className: 'general-error-message', id: errorDivId});

    sectionContainer.appendChild(errorDiv);
    sectionContainer.appendChild(formElement);
}

/**
 * Formats an ISO date string into a more readable local date format.
 * Example: "January 1, 2023".
 * @param {string} isoString - The ISO date string to format.
 * @returns {string} The formatted date string.
 */
function formatPlaylistDate(isoString) {
    const date = new Date(isoString);
    const options = {year: 'numeric', month: 'long', day: 'numeric'};
    return date.toLocaleDateString(undefined, options);
}

/**
 * Creates the HTML structure for the reorder playlist modal content.
 * Includes a close button, title, instructions, a list for songs, and action buttons (Save, Cancel).
 * @returns {HTMLElement} The div element containing the modal's content.
 */
export function createReorderPopup() {

    const modalContent = createElement('div', {className: 'modal-content'});

    // Close button
    const closeButton = createElement('span', {className: 'close-button', id: 'closeReorderModal'});
    closeButton.innerHTML = "&times;";
    modalContent.appendChild(closeButton);

    // Title
    const heading = createElement('h2', {textContent: 'Reorder Playlist:'});
    modalContent.appendChild(heading);

    // Instructions
    const instructions = createElement('p', {
        className: 'modal-instructions',
        textContent: 'Drag and drop songs to reorder them.'
    });
    modalContent.appendChild(instructions);

    // Song list
    const songList = createElement('ul', {className: 'song-list-reorder', id: 'reorderSongList'});
    modalContent.appendChild(songList);

    // Action buttons
    const modalActions = createElement('div', {className: 'modal-actions'});
    const saveButton = createElement('button', {
        className: 'styled-button',
        id: 'saveOrderButton',
        textContent: 'Save Order'
    });
    const cancelButton = createElement('button', {
        className: 'styled-button styled-button-secondary',
        id: 'cancelOrderButton',
        textContent: 'Cancel'
    });

    modalActions.appendChild(saveButton);
    modalActions.appendChild(cancelButton);
    modalContent.appendChild(modalActions);

    return modalContent;

}

/**
 * Populates the reorder modal's song list with draggable song items.
 * Clears any existing items before adding new ones.
 * @param {Array<Object>} orderedSongs - An array of song objects (SongWithAlbum structure) in their current order.
 * @param {HTMLElement} modalContent - The content element of the modal, expected to contain the '#reorderSongList' ul.
 */
export function populateModal(orderedSongs, modalContent) {

    const songList = modalContent.querySelector('#reorderSongList');

    songList.querySelectorAll('.reorder-song-item').forEach(item => item.remove());

    orderedSongs.forEach(swa => {
        const liEl = createElement('li', {className: 'reorder-song-item', textContent: swa.song.title});
        liEl.setAttribute('data-song-id', swa.song.idSong);
        liEl.setAttribute('draggable', 'true');

        songList.appendChild(liEl);
    })

}
