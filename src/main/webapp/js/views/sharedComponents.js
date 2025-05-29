import {createFormField} from '../utils/formUtils.js';
import {createElement, createHeaderContainer, createParagraphElement} from '../utils/viewUtils.js';
import {getSongImageURL} from '../apiService.js';

/**
 * Creates and returns an HTML form element for uploading a new song.
 * @param {string} formId - The ID to assign to the form element.
 * @param {Array<Object>|null} genres - An array of genre objects (e.g., { name: 'ROCK', description: 'Rock Music' }), or null if genres failed to load.
 * @param {Array<Object>|null} albumSummaries - An array of album summary objects (e.g., { title: 'Album Name', artist: 'Artist Name', year: 2000 }), or null.
 * @param {Object|null} error - An error object if genres failed to load, otherwise null.
 * @returns {HTMLFormElement|HTMLParagraphElement} The generated form element, or a paragraph with an error message.
 */
export function createSongUploadFormElement(formId, genres, albumSummaries, error = null) {
    if (error && !genres) { // Prioritize genre loading error if both fail, or if only genres fail
        const errorP = createParagraphElement(
            'Failed to load song creation form: Genres could not be fetched. Please try refreshing.',
            null,
            'general-error-message'
        );
        return errorP;
    }

    const form = createElement('form', {id: formId, attributes: {noValidate: true}});

    // Song Title
    form.appendChild(createFormField('song-title', 'Song Title:', 'text', 'song-title', true));

    // Album Title with Datalist
    const albumTitleField = createFormField('album-title', 'Album Title:', 'text', 'album-title', true);
    const albumTitleInput = albumTitleField.querySelector('#album-title');

    if (albumSummaries && albumSummaries.length > 0) {
        const datalistId = 'album-titles-list-' + formId;
        albumTitleInput.setAttribute('list', datalistId);
        const datalist = createElement('datalist', {id: datalistId});
        const albumElements = albumSummaries.map(album => createElement('option', {attributes: {value: album.title}}));
        datalist.append(...albumElements);

        form.appendChild(datalist);
    }
    form.appendChild(albumTitleField);


    // Album Artist
    const albumArtistField = createFormField('album-artist', 'Album Artist:', 'text', 'album-artist', true);
    form.appendChild(albumArtistField);

    // Album Year
    const albumYearField = createFormField('album-year', 'Album Year:', 'number', 'album-year', true, [], {
        min: "1000",
        max: "9999"
    });
    form.appendChild(albumYearField);

    // Album Image
    form.appendChild(createFormField('album-image', 'Album Image:', 'file', 'album-image', false, [], {accept: 'image/*'}));

    // Event listener for album title input
    console.log('Creating album title event listener');
    albumTitleInput.addEventListener('input', (event) => {
        const enteredTitle = event.target.value;

        const artistInput = form.querySelector('#album-artist');
        const yearInput = form.querySelector('#album-year');
        const imageInput = form.querySelector('#album-image');

        const matchedAlbum = albumSummaries?.find(album => album.title.toLowerCase() === enteredTitle.toLowerCase());

        if (matchedAlbum) {
            artistInput.value = matchedAlbum.artist;
            yearInput.value = matchedAlbum.year;
            artistInput.disabled = true;
            yearInput.disabled = true;
            imageInput.disabled = true;
        } else {
            artistInput.disabled = false;
            yearInput.disabled = false;
            imageInput.disabled = false;
        }
    });

    // Genre Select
    const genreOptions = [{value: "", text: "Select a genre", disabled: true, selected: true}];
    let genresAvailable = false;

    if (genres && genres.length > 0) {
        genresAvailable = true;
        genres.forEach(genre => {
            genreOptions.push({value: genre.name, text: genre.description});
        });
    } else if (genres === null && !error) {
        genreOptions.push({value: '', text: 'No genres available or failed to load', disabled: true});
    }

    const genreField = createFormField('song-genre', 'Genre:', 'select', 'song-genre', true, genreOptions);
    if (!genresAvailable && (!genres || genres.length === 0)) {
        genreField.querySelector('select').disabled = true;
    }
    form.appendChild(genreField);

    // Audio File
    form.appendChild(createFormField('song-audio', 'Audio File:', 'file', 'song-audio', true, [], {accept: 'audio/*'}));

    // Submit Button
    const buttonAttributes = {type: 'submit'};
    if (!genresAvailable && (!genres || genres.length === 0)) {
        buttonAttributes.disabled = true;
    }
    const submitButton = createElement('button', {
        textContent: 'Add Song',
        className: 'styled-button',
        attributes: buttonAttributes
    });
    form.appendChild(submitButton);

    return form;
}

/**
 * Creates an article element with a checkbox for a single song.
 * @param {Object} songWithAlbum - An object containing song and album details.
 * @returns {HTMLElement} The created article element for the song.
 */
export function createSongArticleWithCheckboxElement(songWithAlbum) {
    const article = createElement('article', {className: 'song-item'});
    const label = createElement('label', {
        className: 'song-metadata',
        attributes: {htmlFor: `song-select-${songWithAlbum.song.idSong}`}
    });

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

    const textDiv = createElement('div', {className: 'song-text'});
    textDiv.appendChild(createHeaderContainer(songWithAlbum.song.title, 'h3'));
    textDiv.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));

    label.appendChild(inputEl);
    label.appendChild(img);
    label.appendChild(textDiv);
    article.appendChild(label);

    return article;
}
