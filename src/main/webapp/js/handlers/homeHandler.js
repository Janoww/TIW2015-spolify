import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection } from "../views/homeView.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";


function validateForm(formId, fieldIds){
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
		genres = await loadSongGenres();
	} catch (error) {
		console.error("Failed to load genres for Home Page song form:", error);
		genreError = error;
	}
	renderSongUploadSection(songFormSectionContainer, genres, genreError);
	// TODO: Add event listener for 'add-song-form-home' submission here

	// Load and render playlists
	try {
		const playlists = await loadPlaylists();
		renderPlaylists(appContainer, playlists);
	} catch (error) {
		console.error("Error loading or rendering playlists:", error);
		// Display error in playlist section if possible
	}

	// Load and render songs for the "Create New Playlist" form's song selection
	try {
		const songsForPlaylistSelection = await loadSongs();
		renderSongs(appContainer, songsForPlaylistSelection);
	} catch (error) {
		console.error("Error loading or rendering songs for playlist creation:", error);
	}
	
	//events

	const newSongForm = document.getElementById('add-song-form-home');
	const newPlaylistForm = document.getElementById('crate-playlist-form');

	if (newPlaylistForm){
		newPlaylistForm.addEventListener('submit', async (event) => {
			event.preventDefault();
			const fieldIds = ['new-playlist-title'];
			const selectedCheckboxes = document.querySelectorAll('input[name="selected-songs"]:checked');
			
			if(validateForm('create-playlist-form', fieldIds) && selectedCheckboxes.length > 0){
				const form = event.target;
				
				const plName = form['new-playlist-title'].value;
				const songIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.value));
				
				const payload = {
					name,
					songIds
				};
				
				try {
					const res = await fetch('api/v1/playlists', {
						method: 'POST',
						headers: {
							'Content-Type': 'application/json'
						},
						body: JSON.stringify(payload)
					});

					const data = await res.json();
					
					if (res.ok) {
						console.log("Playlist created:", result);
						//TODO update playlist lits
					} else {
						console.error('Playlist creation failed failed:', data.error || response.statusText);
						//TODO handle errors
					}
				} catch (err) {
					console.error("Error while creating playlist:", err);
					//TODO handle errors
				}				
			} else {
				console.log('NewPlaylist form has errors.');
				//TODO handle errors
			}
		})
	}

	if (newSongForm){
		newSongForm.addEventListener('submit', async (event) => {
			event.preventDefault();
			const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
			if (validateForm('add-song-form-home', fieldIds)){
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
					const response = await fetch('api/v1/songs', {
						method: 'POST',
						body: formData,
					});
					
					const data = await response.json();
					
					if (response.ok) {
						console.log('Upload successful');
						// TODO update song list
					} else {
						console.error('Upload failed:', data.error || response.statusText);
						// TODO handle error messages
					}
				} catch (err) {
					console.error('Error during upload:', err);
					
					// TODO handle general error
				}
			} else {
				console.log('NewSong form has errors.');
				// TODO handle general error
			}
		})
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
	let genres = null;
	let genreError = null;
	try {
		genres = await loadSongGenres();
	} catch (error) {
		console.error("Failed to load genres for Songs Page song form:", error);
		genreError = error;
	}

	renderSongUploadSectionOnSongsPage(songFormSectionOnSongsPage, genres, genreError);
	// TODO: Add event listener for the 'add-song-form' on this page.

	// Load and render all user songs in the '#songs .song-list'
	const songListContainer = appContainer.querySelector('#songs .song-list');
	let allUserSongs = null;
	let songsError = null;
	try {
		allUserSongs = await loadSongs();
	} catch (error) {
		console.error("Error loading all user songs for Songs page:", error);
		songsError = error;
	}
	renderAllUserSongsList(songListContainer, allUserSongs, songsError);

}

async function loadPlaylists() {
	try {
		const res = await fetch('api/v1/playlists', {
			method: 'GET',
			headers: {
				'Accept': 'application/json'
			}
		});
		if (!res.ok) {
			const errorMsg = `Failed to fetch playlists: ${res.status} ${res.statusText}`;
			console.error(errorMsg);
			throw new Error(errorMsg);
		}
		return await res.json();
	} catch (error) {
		console.error('Error loading playlists:', error.message);
		throw error;
	}
}

async function loadSongs() {
	try {
		const res = await fetch('api/v1/songs', {
			method: 'GET',
			headers: {
				'Accept': 'application/json'
			}
		});
		if (!res.ok) {
			const errorMsg = `Failed to fetch songs: ${res.status} ${res.statusText}`;
			console.error(errorMsg);
			throw new Error(errorMsg);
		}
		return await res.json();
	} catch (error) {
		console.error('Error loading songs:', error.message);
		throw error;
	}
}

export async function loadSongGenres() {
	try {
		const res = await fetch('api/v1/songs/genres', {
			method: 'GET',
			headers: {
				'Accept': 'application/json'
			}
		});
		if (!res.ok) {
			const errorMsg = `Failed to fetch song genres: ${res.status} ${res.statusText}`;
			console.error(errorMsg);
			throw new Error(errorMsg);
		}
		return await res.json();
	} catch (error) {
		console.error('Error loading song genres:', error.message);
		throw error;
	}
}
