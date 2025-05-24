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
		
			// TODO dynamically populate the list
		
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
