import { validateForm } from '../utils/formUtils.js';
import { uploadSong } from '../apiService.js';

/**
 * Handles the submission of a song upload form.
 * @param {Event} event - The submit event.
 * @param {string[]} fieldIds - Array of field IDs for validation (e.g., ['song-title', 'album-title', ...]).
 * @param {string} errorDivId - The ID of the div to display general submission errors.
 * @param {function(object, HTMLElement, Array<object>)} onSuccessCallback - Callback to run on successful upload.
 *        It receives the newSong object, the appContainer (optional, for rendering context), and the songsList to update.
 * @param {HTMLElement} [appContainer] - The main application container, passed to onSuccessCallback.
 * @param {Array<object>} [songsList] - The list of songs to update, passed to onSuccessCallback.
 */
export async function handleSongUploadSubmit(event, fieldIds, errorDivId, onSuccessCallback, appContainer, songsList) {
    event.preventDefault();
    const form = event.target;

    const errorDiv = document.getElementById(errorDivId);
    if (errorDiv) {
        errorDiv.style.display = 'none';
        errorDiv.textContent = '';
    }

    if (validateForm(form, fieldIds)) {
        try {
            const newSong = await uploadSong(form);
            console.log('Upload successful:', newSong);

            if (typeof onSuccessCallback === 'function') {
                onSuccessCallback(newSong, appContainer, songsList);
            }

            if (form instanceof HTMLFormElement) {
                form.reset();
            }
            alert('Song uploaded successfully!');

        } catch (error) {
            console.error(`Upload failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
            if (errorDiv) {
                errorDiv.textContent = error.message || 'An unexpected error occurred while uploading the song.';
                errorDiv.style.display = 'block';
            }
        }
    } else {
        console.log('Song form has validation errors. Individual field errors should be displayed by validateForm.');
    }
}
