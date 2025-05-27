import { createFormField } from '../utils/formUtils.js';
import { createSongUploadFormElement } from './sharedComponents.js';
import { getSongImageURL } from '../apiService.js';
import { createHeaderContainer, createParagraphElement, createElement } from '../utils/viewUtils.js';

// Helper function to create a playlist element
function createPlaylistArticle(playlist) {
    const article = createElement('article', { className: 'playlist-item' });

    const divInfo = createElement('div', { className: 'playlist-info' });
    divInfo.appendChild(createHeaderContainer(playlist.name, 'h3'));
    divInfo.appendChild(createParagraphElement('Created: ' + playlist.birthday));
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

    const inputEl = createElement('input', {
        id: 'song-select-' + songWithAlbum.song.idSong,
        className: 'song-checkbox',
        attributes: {
            type: 'checkbox',
            name: 'selected-songs',
            value: songWithAlbum.song.idSong
        }
    });
    article.appendChild(inputEl);

    //TODO image
    const img = document.createElement('img');
    img.src = getSongImageURL(songWithAlbum.song.idSong);
    img.alt = songWithAlbum.song.title || "Song cover";
    img.onerror = () => {
        img.src = 'images/image_placeholder.png';
    };
    article.appendChild(img);

    const label = createElement('label', {
        className: 'song-metadata',
        attributes: { htmlFor: 'song-select-' + songWithAlbum.song.idSong }
    });
    label.appendChild(createHeaderContainer(songWithAlbum.song.title, 'h3'));
    label.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));
    article.appendChild(label);

    return article;
}

// Function that adds the songs to the song list

export function renderSongs(appContainer, songWithAlbums) {
    const songListDiv = appContainer.querySelector('.song-list');

    if (songWithAlbums) {
        songWithAlbums.forEach(swa => {
            const article = createSongArticle(swa);
            songListDiv.appendChild(article);
        })
    }
}


// Function that adds the playlists to the myPlaylists section
export function renderPlaylists(appContainer, playlists) {

    const playlistListDiv = appContainer.querySelector('.playlist-list');

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
    newPlaylistSection.appendChild(newPlaylistForm);
    homeGridDiv.appendChild(newPlaylistSection);

    appContainer.appendChild(homeGridDiv);
}

// Function to specifically render the song upload form section
export function renderSongUploadSection(sectionContainer, genres, error = null) {
    // Clear previous content (e.g., the "Loading form..." message)
    const loaderMessage = sectionContainer.querySelector('#song-form-loader-message');
    if (loaderMessage) loaderMessage.remove();

    // Remove any existing form or error message if re-rendering
    const existingFormOrError = sectionContainer.querySelector('form, p.general-error-message');
    if (existingFormOrError) existingFormOrError.remove();

    const formElement = createSongUploadFormElement('add-song-form-home', genres, error);
    sectionContainer.appendChild(formElement);
}
