// Helper function to create form fields

/**
 * Creates a div element containing a labeled form input field.
 *
 * @param {string} inputId - The ID of the input element.
 * @param {string} labelText - The label text to display.
 * @param {string} inputType - The type of the input element ('text', 'file', 'select', etc.).
 * @param {string} name - The name attribute of the input element.
 * @param {boolean} [required=true] - Whether the field is required.
 * @param {Array<{value: string, text: string, disabled?: boolean, selected?: boolean}>} [options=[]] - Options for a `<select>` input.
 * @param {Object} [attributes={}] - Additional HTML attributes as key-value pairs.
 * @returns {HTMLElement} A div containing the labeled input element.
 */
function createFormField(inputId, labelText, inputType, name, required=true, options=[], attributes={}) {
    const formFieldDiv = document.createElement('div');
    formFieldDiv.className = 'form-field';

    const innerDiv = document.createElement('div');

    const labelEl = document.createElement('label');
    labelEl.htmlFor = inputId;
    labelEl.textContent = labelText;
    innerDiv.appendChild(labelEl);

    let inputEl;
    if (inputType === 'select') {
        inputEl = document.createElement('select');
        if (options) {
            options.forEach(optConfig => {
                const optionEl = document.createElement('option');
                optionEl.value = optConfig.value;
                optionEl.textContent = optConfig.text;
                if (optConfig.disabled) optionEl.disabled = true;
                if (optConfig.selected) optionEl.selected = true;
                inputEl.appendChild(optionEl);
            });
        }
    } else {
        inputEl = document.createElement('input');
        inputEl.type = inputType;
    }

    inputEl.id = inputId;
    inputEl.name = name;
    if (required) {
        inputEl.required = true;
    }

    if (attributes) {
        for (const attrKey in attributes) {
            inputEl.setAttribute(attrKey, attributes[attrKey]);
        }
    }
    innerDiv.appendChild(inputEl);

    formFieldDiv.appendChild(innerDiv);

    const errorSpan = document.createElement('span');
    errorSpan.className = 'error-message';
    errorSpan.id = inputId + '-error';
    formFieldDiv.appendChild(errorSpan);

    return formFieldDiv;
}


// Helper function to create a title container
function createHeaderContainer(titleText, size) {
    const h1 = document.createElement(size);
    h1.textContent = titleText;
    return h1;
}

// Helper function to create a paragraph
function createParagraphElement(text) {
	return document.createElement('p').textContent = text;
}


// Helper function to create a playlist element
function createPlaylistArticle(playlist){
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

function createSongArticle(songWithAlbum){
	const article = document.createElement('article');
	article.className = 'song-item';
	
	const inputEl = document.createElement('input');
	inputEl.type = 'checkbox';
	inputEl.id = 'song-select-' + songWithAlbum.song.idSong;
	inputEl.name = 'selected-songs';
	inputEl.value = 'songId' + songWithAlbum.song.idSong;
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
	
	label.appendChild(createFormField(songWithAlbum.song.title, 'h3'));
	label.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));
	
	article.appendChild(label);
	
	return article;
}

// Function that adds the songs to the song list
export function renderSongs(appContainer, songWithAlbums){
	
	const songListDiv = appContainer.querySelector('song-item');
	
	if(songWithAlbums){
		songWithAlbums.forEach(swa => {
			const article = createSongArticle(songWithAlbums);
			songListDiv.appendChild(article);
		})
	}
}


// Function that adds the playlists to the myPlaylists section
export function renderPlaylists(appContainer, playlists){
		
	const playlistListDiv = appContainer.querySelector('playlist-list');
	
	if(playlists){
		playlists.forEach(pl => {
			const article = createPlaylistArticle(pl);
			playlistListDiv.appendChild(article);
		})
	} 
	
	
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
		
		// 2.2: Form
		const newSongForm = document.createElement('form');
		newSongForm.id = 'add-song-form-home';
		
			// Inputs:
		newSongForm.appendChild(createFormField('song-title', 'Song Title:', 'text', 'song-title', true));
		newSongForm.appendChild(createFormField('album-title', 'Album Title:', 'text', 'album-title', true));
		newSongForm.appendChild(createFormField('album-artist', 'Album Artist:', 'text', 'album-artist', true));
		newSongForm.appendChild(createFormField('album-year', 'Album Year:', 'number', 'album-year', true));
		newSongForm.appendChild(createFormField('album-image', 'Album Image:', 'file', 'album-image', true, [], {accept: 'image/*'}));
		newSongForm.appendChild(createFormField('song-genre', 'Genre:', 'select', 'song-genre', true, [
		  { value: 'rock', text: 'Rock' },
		  { value: 'jazz', text: 'Jazz' }
		])); //FIXME populate with actual genres.
		newSongForm.appendChild(createFormField('song-autio', 'Audio File:', 'file', 'song-audio', true, [], {accept: 'audio/*'}));	
		
			// Button
		const newSongSendButton = document.createElement('button');
		newSongSendButton.type = 'submit';
		newSongSendButton.className = 'styled-button';
		newSongSendButton.textContent = 'Add Song';
		newSongForm.appendChild(newSongSendButton);
		
		newSongSection.appendChild(newSongForm);
		
	
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
