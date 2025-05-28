import { createFormField } from '../utils/formUtils.js';
import { createSongUploadFormElement } from './sharedComponents.js';
import { getSongImageURL } from '../apiService.js';
import { createHeaderContainer, createParagraphElement, createElement } from '../utils/viewUtils.js';
import { getSongsOrdered } from '../utils/orderUtils.js';

// Helper function to create a playlist element
function createPlaylistArticle(playlist) {
    const article = createElement('article', { className: 'playlist-item' });

    const divInfo = createElement('div', { className: 'playlist-info' });
    divInfo.appendChild(createHeaderContainer(playlist.name, 'h3'));
    divInfo.appendChild(createParagraphElement('Created: ' + formatPlaylistDate(playlist.birthday)));
    article.appendChild(divInfo);

    const divActions = createElement('div', { className: 'playlist-actions' });

    const buttonView = createElement('button', {
        textContent: 'View',
        className: 'styled-button view-playlist-button',
        dataset: { playlistId: playlist.idPlaylist }
    });
    const buttonReorder = createElement('button', {
        textContent: 'Reorder',
        className: 'styled-button reorder-playlist-button',
        dataset: { playlistId: playlist.idPlaylist }
    });

    divActions.appendChild(buttonView);
    divActions.appendChild(buttonReorder);
    article.appendChild(divActions);

    return article;
}


// Helper function to create a song element
function createSongArticle(songWithAlbum) {
    const article = createElement('article', { className: 'song-item' });
    const label = createElement('label', {
        className: 'song-metadata',
        attributes: { htmlFor: `song-select-${songWithAlbum.song.idSong}` }
    });
    article.appendChild(label);

    const inputEl = createElement('input', {
        id: 'song-select-' + songWithAlbum.song.idSong,
        className: 'song-checkbox',
        attributes: {
            type: 'checkbox',
            name: 'selected-songs',
            value: songWithAlbum.song.idSong
        }
    });

    const img = document.createElement('img');
    img.src = getSongImageURL(songWithAlbum.song.idSong);
    img.alt = songWithAlbum.song.title || "Song cover";
    img.onerror = () => {
        img.src = 'images/image_placeholder.png';
    };

    const textDiv = createElement('div', { className: 'song-text' });
    textDiv.appendChild(createHeaderContainer(songWithAlbum.song.title, 'h3'));
    textDiv.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));

    label.appendChild(inputEl);
    label.appendChild(img);
    label.appendChild(textDiv);

    return article;
}

// Function that adds the songs to the song list

export function renderSongs(appContainer, songWithAlbums) {
    const songListContainer = appContainer.querySelector('.song-list');

    songListContainer.innerHTML = '';

    if (songWithAlbums) {
        songWithAlbums = getSongsOrdered(songWithAlbums);
        const songArticles = songWithAlbums.map(createSongArticle);
        songListContainer.append(...songArticles);
    }
}


// Function that adds the playlists to the myPlaylists section
export function renderPlaylists(appContainer, playlists) {

    const playlistListDiv = appContainer.querySelector('.playlist-list');

    playlistListDiv.querySelectorAll('.playlist-item').forEach(item => item.remove());

    if (playlists) {
        playlists.forEach(pl => {
            const article = createPlaylistArticle(pl);
            playlistListDiv.appendChild(article);
        })
    }


}


export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

    // The Home Page will be divides in a 3 section horizontal grid
    const homeGridDiv = createElement('div', { className: 'grid home-grid' });

    // Section 1: User playlist
    const myPlaylistsSection = createElement('section', { id: 'user-playlists-section' });
    myPlaylistsSection.appendChild(createHeaderContainer('My Playlists', 'h2'));
    const myPlaylistList = createElement('div', { className: 'playlist-list' });
    myPlaylistsSection.appendChild(myPlaylistList); // The list is populated in the homeHandler by renderPlaylists()
    homeGridDiv.appendChild(myPlaylistsSection);

    // Section 2: New Song Upload
    const newSongSection = createElement('section', { id: 'add-song-section' });
    newSongSection.appendChild(createHeaderContainer('Upload New Song', 'h2'));
    const loadingP = createParagraphElement('Loading song form...', 'song-form-loader-message');
    newSongSection.appendChild(loadingP);
    homeGridDiv.appendChild(newSongSection);

    // Section 3: New Playlist Creation
    const newPlaylistSection = createElement('section', { id: 'create-playlist-section' });
    newPlaylistSection.appendChild(createHeaderContainer('Create New Playlist', 'h2'));
    const newPlaylistForm = createElement('form', { id: 'create-playlist-form', attributes: { noValidate: true } });
    newPlaylistForm.appendChild(createFormField('new-playlist-title', 'Playlist Title:', 'text', 'new-playlist-title', true));
    newPlaylistForm.appendChild(createHeaderContainer('Select Songs to Add:', 'h3'));
    const songListDiv = createElement('div', { className: 'song-list' });
    newPlaylistForm.appendChild(songListDiv);
    const newPlaylistSendButton = createElement('button', {
        textContent: 'Create Playlist',
        className: 'styled-button',
        attributes: { type: 'submit' }
    });
    newPlaylistForm.appendChild(newPlaylistSendButton);
    const errorDiv = createElement('div', { className: 'general-error-message', id: 'create-playlist-error' });
    newPlaylistSection.appendChild(errorDiv);
    newPlaylistSection.appendChild(newPlaylistForm);
    homeGridDiv.appendChild(newPlaylistSection);

    appContainer.appendChild(homeGridDiv);
	
	// Section 4: Modal Popup
	const modalDiv = createElement('div', { className: 'modal', id:'reorderModal'});
	modalDiv.style.display = 'none';
	modalDiv.style.position = 'fixed';
	modalDiv.style.marginTop = '50px';
	const modalContent = createReorderPopup();
	modalDiv.appendChild(modalContent);
	
	
	document.body.appendChild(modalDiv);
	
}

// Function to specifically render the song upload form section
export function renderSongUploadSection(sectionContainer, genres, error = null, formId = 'add-song-form-home', errorDivId = 'create-song-error') {
    // Clear previous content (e.g., the "Loading form..." message)
    const loaderMessage = sectionContainer.querySelector('#song-form-loader-message');
    if (loaderMessage) loaderMessage.remove();

    // Remove any existing form or error message if re-rendering

    const formElement = createSongUploadFormElement(formId, genres, error);

    const errorDiv = createElement('div', { className: 'general-error-message', id: errorDivId });

    sectionContainer.appendChild(errorDiv);
    sectionContainer.appendChild(formElement);
}

function formatPlaylistDate(isoString) {
    const date = new Date(isoString);
    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return date.toLocaleDateString(undefined, options);
}

export function createReorderPopup() {

    const modalContent = createElement('div', { className: 'modal-content' });

    // Close button
    const closeButton = createElement('span', { className: 'close-button', id: 'closeReorderModal' });
    closeButton.innerHTML = "&times;";
    modalContent.appendChild(closeButton);

    // Title
    const heading = createElement('h2', { textContent: 'Reorder Playlist:' });
    modalContent.appendChild(heading);

    // Instructions
    const instructions = createElement('p', { className: 'modal-instructions', textContent: 'Drag and drop songs to reorder them.' });
    modalContent.appendChild(instructions);

    // Song list
    const songList = createElement('ul', { className: 'song-list-reorder', id: 'reorderSongList' });
    modalContent.appendChild(songList);

    // Action buttons
    const modalActions = createElement('div', { className: 'modal-actions' });
    const saveButton = createElement('button', { className: 'styled-button', id: 'saveOrderButton', textContent: 'Save Order' });
    const cancelButton = createElement('button', { className: 'styled-button styled-button-secondary', id: 'cancelOrderButton', textContent: 'Cancel' });

    modalActions.appendChild(saveButton);
    modalActions.appendChild(cancelButton);
    modalContent.appendChild(modalActions);

    return modalContent;

}

export function populateModal(orderedSongs, modalContent) {

    const songList = modalContent.querySelector('#reorderSongList');
	
	songList.querySelectorAll('.reorder-song-item').forEach(item => item.remove());

    orderedSongs.forEach(swa => {
        const liEl = createElement('li', { className: 'reorder-song-item', textContent: swa.song.title });
        liEl.setAttribute('data-song-id', swa.song.idSong);
        liEl.setAttribute('draggable', 'true');

        songList.appendChild(liEl);
    })

}
