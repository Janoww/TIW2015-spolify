// Helper function to create form fields
function createFormField({ id, labelText, inputType, name, required, options, attributes }) {
    const formFieldDiv = document.createElement('div');
    formFieldDiv.className = 'form-field';

    const innerDiv = document.createElement('div');

    const labelEl = document.createElement('label');
    labelEl.htmlFor = id;
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

    inputEl.id = id;
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
    errorSpan.id = id + '-error';
    formFieldDiv.appendChild(errorSpan);

    return formFieldDiv;
}

export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';
    document.getElementById('navbar').style.display = 'inline-block';

    const songsSection = document.createElement('section');
    songsSection.id = 'songs';

    const songsH2 = document.createElement('h2');
    songsH2.textContent = 'All Songs';

    songsSection.appendChild(songsH2);
    // TODO: Populate songsSection with actual songs

    appContainer.appendChild(songsSection);

    const addSongSection = document.createElement('section');
    addSongSection.id = 'add-song';

    const addSongH2 = document.createElement('h2');
    addSongH2.textContent = 'Add Song';

    addSongSection.appendChild(addSongH2);

    // Create the form for adding a new song
    const addSongForm = document.createElement('form');
    addSongForm.id = 'add-song-form';
    // TODO: Add event listener for form submission, e.g.:
    // import { handleAddSongSubmit } from '../handlers/homeHandler.js';
    // addSongForm.addEventListener('submit', handleAddSongSubmit);

    const formFieldsConfig = [
        { id: 'song-title', labelText: 'Song Title:', inputType: 'text', name: 'song-title', required: true },
        { id: 'album-title', labelText: 'Album Title:', inputType: 'text', name: 'album-title', required: true },
        { id: 'album-artist', labelText: 'Album Artist:', inputType: 'text', name: 'album-artist', required: true },
        { id: 'album-year', labelText: 'Album Year:', inputType: 'number', name: 'album-year', required: true, attributes: { min: '1000', max: '9999' } },
        { id: 'album-image', labelText: 'Album Image:', inputType: 'file', name: 'album-image', attributes: { accept: 'image/*' } },
        {
            id: 'song-genre', labelText: 'Genre:', inputType: 'select', name: 'song-genre', required: true,
            options: [
                { value: "", text: "Select a genre", disabled: true, selected: true },
                { value: "AFRICAN", text: "Music of Africa" },
                { value: "ALTERNATIVE_ROCK", text: "Alternative rock" },
                { value: "AMBIENT", text: "Ambient music" },
                { value: "AMERICAN_FOLK", text: "American folk music" },
                { value: "ASIAN", text: "Music of Asia" },
                { value: "BLUES", text: "Blues" },
                { value: "CHRISTIAN", text: "Christian music" },
                { value: "CLASSICAL", text: "Classical music" },
                { value: "COMMERCIAL", text: "Commercial" },
                { value: "COUNTRY", text: "Country music" },
                { value: "DANCE", text: "Dance music" },
                { value: "DISCO", text: "Disco" },
                { value: "EASY_LISTENING", text: "Easy listening" },
                { value: "EDM", text: "Electronic dance music" },
                { value: "ELECTRONIC", text: "Electronic music" },
                { value: "EXPERIMENTAL", text: "Experimental music" },
                { value: "FOLK", text: "Folk music" },
                { value: "FUNK", text: "Funk" },
                { value: "GENEALOGY", text: "Genealogy of musical genres" },
                { value: "GOSPEL", text: "Gospel music" },
                { value: "HARDCORE", text: "Hardcore" },
                { value: "HEAVY_METAL", text: "Heavy metal" },
                { value: "HIPHOP", text: "Hip-hop" },
                { value: "HIPHOP_CULTURE", text: "Hip-hop culture" },
                { value: "HOUSE", text: "House music" },
                { value: "INDEPENDENT", text: "Independent music" },
                { value: "INDIE_POP", text: "Indie pop" },
                { value: "INDIE_ROCK", text: "Indie rock" },
                { value: "JAZZ", text: "Jazz" },
                { value: "KPOP", text: "K-pop" },
                { value: "LATIN_AMERICAN", text: "Music of Latin America" },
                { value: "MIDDLE_EASTERN", text: "Middle Eastern music" },
                { value: "MODERNISM", text: "Modernism" },
                { value: "NEW_AGE", text: "New-age music" },
                { value: "NEW_WAVE", text: "New wave" },
                { value: "POP", text: "Pop music" },
                { value: "PSYCHEDELIC", text: "Psychedelic music" },
                { value: "PUNK_ROCK", text: "Punk rock" },
                { value: "REGGAE", text: "Reggae" },
                { value: "ROCK_AND_ROLL", text: "Rock and roll" },
                { value: "SKA", text: "Ska" },
                { value: "SOCA", text: "Soca music" },
                { value: "SOUL", text: "Soul music" },
                { value: "SYNTH_POP", text: "Synth-pop" },
                { value: "TECHNO", text: "Techno" },
                { value: "THRASH_METAL", text: "Thrash metal" },
                { value: "VAPOR_WAVE", text: "Vapor-wave" },
                { value: "VOCAL", text: "Vocal music" },
                { value: "WESTERN", text: "Western music" },
                { value: "WORLD", text: "World music" }
            ]
        },
        { id: 'song-audio', labelText: 'Audio File:', inputType: 'file', name: 'song-audio', required: true, attributes: { accept: 'audio/*' } }
    ];

    formFieldsConfig.forEach(config => {
        addSongForm.appendChild(createFormField(config));
    });

    const submitButton = document.createElement('button');
    submitButton.className = 'styled-button';
    submitButton.type = 'submit';
    submitButton.textContent = 'Add Song';
    addSongForm.appendChild(submitButton);

    addSongSection.appendChild(addSongForm);
    appContainer.appendChild(addSongSection);
}
