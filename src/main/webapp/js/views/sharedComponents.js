import { createFormField } from '../utils/formUtils.js';

/**
 * Creates and returns an HTML form element for uploading a new song.
 * @param {string} formId - The ID to assign to the form element.
 * @param {Array<Object>|null} genres - An array of genre objects (e.g., { name: 'ROCK', description: 'Rock Music' }), or null if genres failed to load.
 * @param {Object|null} error - An error object if genres failed to load, otherwise null.
 * @returns {HTMLFormElement|HTMLParagraphElement} The generated form element, or a paragraph with an error message.
 */
export function createSongUploadFormElement(formId, genres, error = null) {
    if (error) {
        const errorP = document.createElement('p');
        errorP.className = 'general-error-message';
        errorP.textContent = 'Failed to load song creation form: Genres could not be fetched. Please try refreshing.';
        return errorP;
    }

    const form = document.createElement('form');
    form.id = formId;
    form.noValidate = true;

    // Song Title
    form.appendChild(createFormField('song-title', 'Song Title:', 'text', 'song-title', true));
    // Album Title
    form.appendChild(createFormField('album-title', 'Album Title:', 'text', 'album-title', true));
    // Album Artist
    form.appendChild(createFormField('album-artist', 'Album Artist:', 'text', 'album-artist', true));
    // Album Year
    form.appendChild(createFormField('album-year', 'Album Year:', 'number', 'album-year', true, [], { min: "1000", max: "9999" }));
    // Album Image (Optional based on specification for POST /songs, but home_mockup.html has it as not required, songs_mockup.html has it as not required)
    form.appendChild(createFormField('album-image', 'Album Image:', 'file', 'album-image', false, [], { accept: 'image/*' }));

    // Genre Select
    // Initial default option
    const genreOptions = [{ value: "", text: "Select a genre", disabled: true, selected: true }];
    let genresAvailable = false;

    if (genres && genres.length > 0) {
        genresAvailable = true;
        genres.forEach(genre => {
            // Assuming genre object has 'name' for value and 'description' for text, as per API spec
            genreOptions.push({ value: genre.name, text: genre.description });
        });
    } else if (genres === null && !error) {
        genreOptions.push({ value: '', text: 'Failed to load genres', disabled: true });
    } else if (genres && genres.length === 0) {
        genreOptions.push({ value: '', text: 'No genres available', disabled: true });
    }

    const genreField = createFormField('song-genre', 'Genre:', 'select', 'song-genre', true, genreOptions);
    if (!genresAvailable) {
        genreField.querySelector('select').disabled = true;
    }
    form.appendChild(genreField);

    // Audio File
    form.appendChild(createFormField('song-audio', 'Audio File:', 'file', 'song-audio', true, [], { accept: 'audio/*' }));

    // Submit Button
    const submitButton = document.createElement('button');
    submitButton.type = 'submit';
    submitButton.className = 'styled-button';
    submitButton.textContent = 'Add Song';
    if (!genresAvailable) {
        submitButton.disabled = true;
    }
    form.appendChild(submitButton);

    return form;
}
