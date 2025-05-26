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
	let orderedSongs = undefined;
	if(playlistOrder && playlistOrder.length > 0){
		orderedSongs = await getOrderedSongs(playlistOrder);	
	} else {
		orderedSongs = await getOrderedSongs(playlist.songs);
		
		orderedSongs.sort((a, b) => {
		  // Compare artist names case-insensitive
		  const artistA = a.album.artist.toLowerCase();
		  const artistB = b.album.artist.toLowerCase();
		  
		  if (artistA < artistB) return -1;
		  if (artistA > artistB) return 1;
		  
		  // If artist names are equal, compare album year
		  return a.album.year - b.album.year;
		});
	}
	
	renderPlaylistView(appContainer);	
	
	//Write title
	const sliderHeader = document.getElementById('sliderHeader');
	if (sliderHeader){
		sliderHeader.textContent = 'Songs in "' + playlist.name + '"'
	}
	
	//Populate the slider
	const loaderMessage = document.querySelector('#all-songs-loader-message');
	if (loaderMessage) {
		loaderMessage.remove();
	}
	
	if(orderedSongs){
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