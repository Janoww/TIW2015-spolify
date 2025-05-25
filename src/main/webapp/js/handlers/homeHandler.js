import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection } from "../views/homeView.js";
import { renderSongsView, renderSongUploadSectionOnSongsPage, renderAllUserSongsList } from "../views/songsView.js";

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
