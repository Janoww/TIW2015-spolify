import { renderHomeView, renderPlaylists, renderSongs } from "../views/homeView.js";
import { logoutUser } from "./loginHandler.js";

export async function initHomePage(appContainer) {
    const navbar = document.getElementById('navbar');
    if (navbar) {
        navbar.style.display = 'inline-block';
    } else {
        console.error("Navbar element not found in initHomePage.");
    }
    renderHomeView(appContainer);
	
	const playlists = await loadPlaylists();
	
	renderPlaylists(appContainer, playlists);
	
	const songs = await loadSongs();
	
	renderSongs(appContainer, songs);
	
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
			console.error('Failed to fetch playlists:', res.status);
			return;
		}
		return await res.json();
	} catch (error) {
		console.error('Error loading playlists:', error);
	}
}

async function loadSongs() {
	try {
		const res = await fettch('api/api/v1/songs', {
			method: 'GET',
			headers: {
				'Accept': 'application/json'
			}
		});
		if (!res.ok) {
			console.error('Failed to fetch songs:', res.status);
			return;
		}
		return await res.json();
	} catch (error) {
		console.error('Error loading songs:', error);
	}
}
