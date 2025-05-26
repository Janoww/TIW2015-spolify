
import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection } from "../views/homeView.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";
import { getPlaylists, createPlaylist, getSongs, uploadSong, getSongGenres as apiGetSongGenres } from '../apiService.js';
import { initPlaylistPage } from "./playlistHandler.js"

function validateForm(formId, fieldIds) {
    //TODO
    return true;
}

/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initHomePage(appContainer) {
    const navbar = document.getElementById('navbar');
    if (navbar) {
        navbar.style.display = 'inline-block';
    } else {
        console.error("Navbar element not found in initHomePage.");
    }

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
		renderPlaylists(appContainer, playlists);
	} catch (error) {
		console.error(`Error loading or rendering playlists: Status ${error.status}, Message: ${error.message}`, error.details || '');
		// Display error in playlist section if possible
	}

    // Load and render songs for the "Create New Playlist" form's song selection
    try {
        const songsForPlaylistSelection = await getSongs();
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
                    //TODO update playlist list
                    // Example: refreshPlaylists(appContainer);
                    // or add newPlaylist to the existing list if the view supports it
                } catch (error) {
                    console.error(`Playlist creation failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    //TODO handle errors (e.g., display error.message in the UI)
                }
            } else {
                console.log('NewPlaylist form has errors or no songs selected.');
                //TODO handle errors
            }
        })
    }

    if (newSongForm) {
        newSongForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
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
					// TODO update song list
					// Example: refreshSongsForPlaylistForm(appContainer);
					// and potentially refresh other song lists if they are visible
				} catch (error) {
					console.error(`Upload failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
					// TODO handle error messages (e.g., display error.message in the UI)
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
				const playlistId = parseInt(target.dataset.playlistId, 10); // convert to number
				const playlist = playlists.find(p => p.idPlaylist === playlistId);
				
				initPlaylistPage(appContainer, playlist);
		    }

		    if (target.classList.contains('reorder-playlist-button')) {
		        const playlistId = target.dataset.playlistId;
		        console.log(`Reorder playlist ${playlistId}`); //TODO
		    }
		});
	}
}

export async function initSongPage(appContainer) {
    const navbar = document.getElementById('navbar');
    if (navbar) {
        navbar.style.display = 'inline-block';
    } else {
        console.error("Navbar element not found in initSongPage.");
    }

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
