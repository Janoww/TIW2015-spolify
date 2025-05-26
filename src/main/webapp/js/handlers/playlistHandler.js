import { renderPlaylistView } from '../views/playlistView.js';
import { getPlaylistSongOrder } from '../apiService.js';

export async function initPlaylistPage(appContainer, playlist){
	const navbar = document.getElementById('navbar');
	if (navbar) {
		navbar.style.display = 'inline-block';
	} else {
		console.error("Navbar element not found in initHomePage.");
	}
	
	const playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
	// const orderedSongs = await getOrderedSongs(playlistOrder);	
	
	console.log(playlistOrder);
	
	renderPlaylistView(appContainer);	
	
	//Write title
	const sliderHeader = document.getElementById('sliderHeader');
	console.log(sliderHeader);
	if (sliderHeader){
		sliderHeader.textContent = 'Songs in "' + playlist.name + '"'
	}
	
	//Populate the slider
	
	const loaderMessage = document.querySelector('#all-songs-loader-message');
	if (loaderMessage) {
		loaderMessage.remove();
	}
	
	if(playlistOrder && playlistOrder.length > 0){
		let page = 0;
		let totPages = Math.ceil(playlistOrder.length / 5);
		if (page > totPages - 1) {
			page = totPages - 1;
		}
		let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);

		
		
		
	}
	

	
}

async function getOrderedSongs(songIdList) {
	const orderedSongs = [];

	for (const id of songIdList) {
		try {
			const song = await getSongDetails(id); // Fetch one song at a time
			orderedSongs.push(song);
		} catch (err) {
			console.error(`Failed to fetch song with id ${id}:`, err);
		}
	}

	return orderedSongs;
}