
import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection } from "../views/homeView.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";
import { getPlaylists, createPlaylist, getSongs, uploadSong, getSongGenres as apiGetSongGenres } from '../apiService.js';
import { navigate } from '../router.js';


/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initHomePage(appContainer) {

    renderHomeView(appContainer);


    const songFormSectionContainer = appContainer.querySelector('#add-song-section');
    let genres = null;
    let genreError = null;
    try {
        genres = await apiGetSongGenres();
    } catch (error) {
        console.error(`Failed to load genres for Home Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreError = error;
    }
    renderSongUploadSection(songFormSectionContainer, genres, genreError);
    // TODO: Add event listener for 'add-song-form-home' submission here


    // Load and render playlists
    let playlists;
    try {
        playlists = await getPlaylists();
        playlists.sort((a, b) => new Date(b.birthday) - new Date(a.birthday));
        renderPlaylists(appContainer, playlists);
    } catch (error) {
        console.error(`Error loading or rendering playlists: Status ${error.status}, Message: ${error.message}`, error.details || '');
        // Display error in playlist section if possible
    }

    // Load and render songs for the "Create New Playlist" form's song selection
    let songsForPlaylistSelection;
    try {
        songsForPlaylistSelection = await getSongs();
        renderSongs(appContainer, songsForPlaylistSelection);
    } catch (error) {
        console.error(`Error loading or rendering songs for playlist creation: Status ${error.status}, Message: ${error.message}`, error.details || '');
    }

    //events


    const newSongForm = document.getElementById('add-song-form-home');
    const newPlaylistForm = document.getElementById('create-playlist-form');
    const playlistsList = document.querySelector('.playlist-list')


    if (newPlaylistForm) {
        newPlaylistForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fieldIds = ['new-playlist-title'];
            const selectedCheckboxes = document.querySelectorAll('input[name="selected-songs"]:checked');


            const errorDiv = document.getElementById('create-playlist-error');
            if (errorDiv) {
                errorDiv.style.display = 'none';
                errorDiv.textContent = '';
            }

            if (selectedCheckboxes.length === 0) {
                if (errorDiv) {
                    errorDiv.textContent = 'Please select at least one song.';
                    errorDiv.style.display = 'block';
                }
                return;
            }

            if (validateForm('create-playlist-form', fieldIds) && selectedCheckboxes.length > 0) {
                const form = event.target;


                const name = form['new-playlist-title'].value;
                const songIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.value));

                const payload = {
                    name,
                    songIds
                };

                try {
                    const newPlaylist = await createPlaylist(payload);
                    console.log("Playlist created:", newPlaylist);

                    playlists.unshift(newPlaylist);
                    renderPlaylists(appContainer, playlists);
                    newPlaylistForm.reset();

                } catch (error) {
                    console.error(`Playlist creation failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    if (errorDiv) {
                        errorDiv.textContent = 'An error occurred while adding songs. Please try again.';
                        errorDiv.style.display = 'block';
                    }
                }

            } else {
                console.log('NewPlaylist form has errors or no songs selected.');
            }
        })
    }

    if (newSongForm) {
        newSongForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];

            const errorDiv = document.getElementById('create-song-error');
            if (errorDiv) {
                errorDiv.style.display = 'none';
                errorDiv.textContent = '';
            }

            if (validateForm('add-song-form-home', fieldIds)) {
                const form = event.target;
                const formData = new FormData();

                // Text fields
                formData.append('title', form['song-title'].value);
                formData.append('albumTitle', form['album-title'].value);
                formData.append('albumArtist', form['album-artist'].value);
                formData.append('albumYear', form['album-year'].value);
                formData.append('genre', form['song-genre'].value);

                // File fields
                formData.append('albumImage', form['album-image'].files[0]);
                formData.append('audioFile', form['song-audio'].files[0]);
                // Submit via fetch
                try {
                    const newSong = await uploadSong(formData);
                    console.log('Upload successful:', newSong);


                    songsForPlaylistSelection.unshift(newSong);
                    renderSongs(appContainer, songsForPlaylistSelection);

                    newSongForm.reset();

                } catch (error) {
                    console.error(`Upload failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    if (errorDiv) {
                        errorDiv.textContent = error.message;
                        errorDiv.style.display = 'block';
                    }
                }
            } else {
                console.log('NewSong form has errors.');
                // TODO handle general error
            }
        })
    }

    if (playlistsList) {
        playlistsList.addEventListener('click', event => {
            const target = event.target;

            if (target.classList.contains('view-playlist-button')) {
                const playlistId = parseInt(target.dataset.playlistId, 10);
                if (playlistId) {
                    navigate('playlist-' + playlistId);
                } else {
                    console.error("Playlist ID not found for navigation.");
                }
            }

            if (target.classList.contains('reorder-playlist-button')) {
                const playlistId = target.dataset.playlistId;
                console.log(`Reorder playlist ${playlistId}`); //TODO
            }
        });
    }
}

export async function initSongPage(appContainer) {
    // Render the basic songs view structure, including a placeholder for its song form
    renderSongsView(appContainer);

    const songFormSectionOnSongsPage = appContainer.querySelector('#add-song');
    let genresForSongPage = null;
    let genreErrorForSongPage = null;
    try {
        genresForSongPage = await apiGetSongGenres();
    } catch (error) {
        console.error(`Failed to load genres for Songs Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreErrorForSongPage = error;
    }

    renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genresForSongPage, genreErrorForSongPage);
    // TODO: Add event listener for the 'add-song-form' on this page.

    // Load and render all user songs in the '#songs .song-list'
    const songListContainer = appContainer.querySelector('#songs .song-list');
    let allUserSongs = null;
    let songsError = null;
    try {
        allUserSongs = await getSongs();
    } catch (error) {
        console.error(`Error loading all user songs for Songs page: Status ${error.status}, Message: ${error.message}`, error.details || '');
        songsError = error;
    }
    renderAllUserSongsList(songListContainer, allUserSongs, songsError);

}

function validateForm(formId, fieldIds) {
    //TODO regex validation
    const form = document.getElementById(formId);
    if (!form) {
        console.error(`Form with id "${formId}" not found.`);
        return false;
    }

    let isValid = true;

    for (const fieldId of fieldIds) {
        const input = form.querySelector(`#${fieldId}`);
        const errorSpan = form.querySelector(`#${fieldId}-error`);

        if (!input || !errorSpan) {
            console.warn(`Field or error span for id "${fieldId}" not found.`);
            continue;
        }

        let value = input.value;

        // Special handling for file inputs
        if (input.type === "file") {
            if (input.files.length === 0) {
                errorSpan.textContent = "Please select a file.";
                isValid = false;
                continue;
            } else {
                errorSpan.textContent = "";
            }
            continue;
        }

        // Special handling for select dropdowns
        if (input.tagName.toLowerCase() === "select") {
            if (!value) {
                errorSpan.textContent = "Please select an option.";
                isValid = false;
                continue;
            } else {
                errorSpan.textContent = "";
            }
            continue;
        }

        // For text and number inputs
        if (!value || value.trim() === "") {
            errorSpan.textContent = "This field is required.";
            isValid = false;
        } else if (input.type === "number") {
            const num = Number(value);
            const min = input.min ? Number(input.min) : null;
            const max = input.max ? Number(input.max) : null;
            if ((min !== null && num < min) || (max !== null && num > max)) {
                errorSpan.textContent = `Value must be between ${min} and ${max}.`;
                isValid = false;
            } else {
                errorSpan.textContent = "";
            }
        } else {
            errorSpan.textContent = "";
        }
    }

    return isValid;
}
