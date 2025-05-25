import { createFormField } from '../utils/formUtils.js';
import { createSongUploadFormElement } from './sharedComponents.js';

// Helper function to create a title container
function createHeaderContainer(titleText, size) { // TODO: Consider moving to a shared viewUtils.js
	const h1 = document.createElement(size);
	h1.textContent = titleText;
	return h1;
}

// Helper function to create a paragraph
function createParagraphElement(text) {
	const node = document.createElement('p');
	node.textContent = text;
	return node;
}


// Helper function to create a playlist element
function createPlaylistArticle(playlist) {
	const article = document.createElement('article');
	article.className = 'playlist-item';

	const divInfo = document.createElement('div');
	divInfo.className = 'playlist-info';

	divInfo.appendChild(createHeaderContainer(playlist.name, 'h3'));
	divInfo.appendChild(createParagraphElement('Created: ' + playlist.birthday));

	article.appendChild(divInfo);

	const divActions = document.createElement('div');
	divActions.className = 'palylist-actions';

	const buttonView = document.createElement('button');
	const buttonReorder = document.createElement('button');

	buttonView.className = 'styled-button view-playlist-button';
	buttonView.dataset.playlistId = playlist.idPlaylist;
	buttonView.textContent = 'View';

	buttonReorder.className = 'styled-button reorder-playlist-button';
	buttonReorder.dataset.playlistId = playlist.idPlaylist;
	buttonReorder.textContent = 'Reorder';

	divActions.appendChild(buttonView);
	divActions.appendChild(buttonReorder);

	article.appendChild(divActions);


	return article;
}


// Helper function to create a song element
function createSongArticle(songWithAlbum){
	const article = document.createElement('article');
	article.className = 'song-item';

	const inputEl = document.createElement('input');
	inputEl.type = 'checkbox';
	inputEl.id = 'song-select-' + songWithAlbum.song.idSong;
	inputEl.name = 'selected-songs';
	inputEl.value = songWithAlbum.song.idSong;
	inputEl.className = 'song-checkbox';

	article.appendChild(inputEl);

	//TODO image
	const img = document.createElement('img');
	// img.src = ...
	img.alt = songWithAlbum.song.title;

	article.appendChild(img);

	const label = document.createElement('label');
	label.htmlFor = 'song-select-' + songWithAlbum.song.idSong;
	label.className = 'song-metadata';

	label.appendChild(createHeaderContainer(songWithAlbum.song.title, 'h3'));
	label.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));

	article.appendChild(label);

	return article;
}

// Function that adds the songs to the song list

export function renderSongs(appContainer, songWithAlbums){	
	const songListDiv = appContainer.querySelector('.song-list');
	
	if(songWithAlbums){
		songWithAlbums.forEach(swa => {
			const article = createSongArticle(swa);
			songListDiv.appendChild(article);
		})
	}
}


// Function that adds the playlists to the myPlaylists section
export function renderPlaylists(appContainer, playlists) {

	const playlistListDiv = appContainer.querySelector('playlist-list');

	if (playlists) {
		playlists.forEach(pl => {
			const article = createPlaylistArticle(pl);
			playlistListDiv.appendChild(article);
		})
	}


}

function listAllGenres(){
	return [
	  { value: 'AFRICAN', text: 'Music of Africa' },
	  { value: 'ALTERNATIVE_ROCK', text: 'Alternative rock' },
	  { value: 'AMBIENT', text: 'Ambient music' },
	  { value: 'AMERICAN_FOLK', text: 'American folk music' },
	  { value: 'ASIAN', text: 'Music of Asia' },
	  { value: 'BLUES', text: 'Blues' },
	  { value: 'CHRISTIAN', text: 'Christian music' },
	  { value: 'CLASSICAL', text: 'Classical music' },
	  { value: 'COMMERCIAL', text: 'Commercial' },
	  { value: 'COUNTRY', text: 'Country music' },
	  { value: 'DANCE', text: 'Dance music' },
	  { value: 'DISCO', text: 'Disco' },
	  { value: 'EASY_LISTENING', text: 'Easy listening' },
	  { value: 'EDM', text: 'Electronic dance music' },
	  { value: 'ELECTRONIC', text: 'Electronic music' },
	  { value: 'EXPERIMENTAL', text: 'Experimental music' },
	  { value: 'FOLK', text: 'Folk music' },
	  { value: 'FUNK', text: 'Funk' },
	  { value: 'GENEALOGY', text: 'Genealogy of musical genres' },
	  { value: 'GOSPEL', text: 'Gospel music' },
	  { value: 'HARDCORE', text: 'Hardcore' },
	  { value: 'HEAVY_METAL', text: 'Heavy metal' },
	  { value: 'HIPHOP', text: 'Hip-hop' },
	  { value: 'HIPHOP_CULTURE', text: 'Hip-hop culture' },
	  { value: 'HOUSE', text: 'House music' },
	  { value: 'INDEPENDENT', text: 'Independent music' },
	  { value: 'INDIE_POP', text: 'Indie pop' },
	  { value: 'INDIE_ROCK', text: 'Indie rock' },
	  { value: 'JAZZ', text: 'Jazz' },
	  { value: 'KPOP', text: 'K-pop' },
	  { value: 'LATIN_AMERICAN', text: 'Music of Latin America' },
	  { value: 'MIDDLE_EASTERN', text: 'Middle Eastern music' },
	  { value: 'MODERNISM', text: 'Modernism' },
	  { value: 'NEW_AGE', text: 'New-age music' },
	  { value: 'NEW_WAVE', text: 'New wave' },
	  { value: 'POP', text: 'Pop music' },
	  { value: 'PSYCHEDELIC', text: 'Psychedelic music' },
	  { value: 'PUNK_ROCK', text: 'Punk rock' },
	  { value: 'REGGAE', text: 'Reggae' },
	  { value: 'ROCK_AND_ROLL', text: 'Rock and roll' },
	  { value: 'SKA', text: 'Ska' },
	  { value: 'SOCA', text: 'Soca music' },
	  { value: 'SOUL', text: 'Soul music' },
	  { value: 'SYNTH_POP', text: 'Synth-pop' },
	  { value: 'TECHNO', text: 'Techno' },
	  { value: 'THRASH_METAL', text: 'Thrash metal' },
	  { value: 'VAPOR_WAVE', text: 'Vapor-wave' },
	  { value: 'VOCAL', text: 'Vocal music' },
	  { value: 'WESTERN', text: 'Western music' },
	  { value: 'WORLD', text: 'World music' }
	];
}

export function renderHomeView(appContainer) {
	appContainer.innerHTML = '';
	appContainer.style.maxWidth = '100%';

	// The Home Page will be divides in a 3 section orizzontal grid
	const homeGridDiv = document.createElement('div');
	homeGridDiv.className = 'grid home-grid';

	// Section 1: User playlist
	const myPlaylistsSection = document.createElement('section');
	myPlaylistsSection.id = 'user-playlists-section';

	// 1.1: Title
	myPlaylistsSection.appendChild(createHeaderContainer('My Playlists', 'h2'));

	// 1.2: List
	const myPlaylistList = document.createElement('div');
	myPlaylistList.className = 'playlist-list';

	// The list is populated in the homeHandler by renderPlaylists()

	myPlaylistsSection.appendChild(myPlaylistList);

	homeGridDiv.appendChild(myPlaylistsSection);

	// Section 2: New Song Upload
	const newSongSection = document.createElement('section');
	newSongSection.id = 'add-song-section';

	// 2.1: Title
	newSongSection.appendChild(createHeaderContainer('Upload New Song', 'h2'));

	// Placeholder for the song form
	const loadingP = document.createElement('p');
	loadingP.id = 'song-form-loader-message';
	loadingP.textContent = 'Loading song form...';
	newSongSection.appendChild(loadingP);

	homeGridDiv.appendChild(newSongSection);

	// Section 3: New Playlist Creation
	const newPlaylistSection = document.createElement('section');
	newPlaylistSection.id = 'create-playlist-section';

	// 3.1: Title
	newPlaylistSection.appendChild(createHeaderContainer('Create New Playlist', 'h2'));

	// 3.2: Form
	const newPlaylistForm = document.createElement('form');
	newPlaylistForm.id = 'create-playlist-form';

	// Input: title
	newPlaylistForm.appendChild(createFormField('new-playlist-title', 'Playlist Title:', 'text', 'new-playlist-title', true));

	// Input: song list
	newPlaylistForm.appendChild(createHeaderContainer('Select Songs to Add:', 'h3'));
	// TODO populate song list

	// Button
	const newPlaylistSendButton = document.createElement('button');
	newPlaylistSendButton.type = 'submit';
	newPlaylistSendButton.className = 'styled-button';
	newPlaylistSendButton.textContent = 'Create Playlist';
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
