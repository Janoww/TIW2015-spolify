import { renderPlaylistView, writeSliderHeader, renderButtons } from '../views/playlistView.js';
import { getPlaylists, getPlaylistSongOrder, getSongDetails, getSongImageURL, getSongs, addSongsToPlaylist } from '../apiService.js';
import { getSongsOrdered } from '../utils/orderUtils.js';


/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initPlaylistPage(appContainer, params) {

	const { idplaylist } = params;

	if (!idplaylist) {
		console.error("Playlist ID is missing from params.");
		navigate('home');
		return;
	}

	// Navbar display is handled by the router
	appContainer.innerHTML = '<div class="loader">Loading playlist details...</div>';

	// Fetch all playlists and find the current one by ID
	const allPlaylists = await getPlaylists();
	const currentPlaylistId = parseInt(idplaylist, 10); // Ensure ID is a number for comparison
	const playlist = allPlaylists.find(p => p.idPlaylist === currentPlaylistId);

	if (!playlist) {
		console.error(`Playlist with ID ${currentPlaylistId} not found.`);
		appContainer.innerHTML = `<p>Playlist not found.</p>`;
		return;
	}

	// Retrieve resources
	let playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
	let orderedSongs = await getOrderedSongs(playlist, playlistOrder);
	let allSongs = await getSongs();

	let page = 0;

	// Render the skeleton of the page
	renderPlaylistView(appContainer);

	//Write title
	writeSliderHeader(playlist.name);

	//Populate the slider
	populateSlider(orderedSongs, page);

	// Form to Add songs
	generateAndPopulateForm(orderedSongs, allSongs, appContainer);



	// Events


	const addSongForm = document.getElementById('add-song-form');
	const preButton = appContainer.querySelector('.pre-carouselButton');
	const nxtButton = appContainer.querySelector('.next-carouselButton');
	console.log(nxtButton);

	if (preButton) {
		preButton.addEventListener('click', () => {
			let totPages = Math.ceil(orderedSongs.length / 5);
			if (page > 0) {
				page--;
				let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);
				const sliderContainer = appContainer.querySelector('.slider-container');
				// Remove all elements with class 'slider-item' inside sliderContainer
				if (sliderContainer) {
					const sliderItems = sliderContainer.querySelectorAll('.slider-item');
					sliderItems.forEach(item => item.remove());
				}

				// Add new songs
				songWithAlbumDisplayed.forEach(swa => {
					sliderContainer.append(createSliderItem(swa));
				})

				renderButtons(page, totPages);
			}
		})
	}

	if (nxtButton) {
		nxtButton.addEventListener('click', () => {
			let totPages = Math.ceil(orderedSongs.length / 5);

			if (page < totPages - 1) {
				page++;
				let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);
				const sliderContainer = appContainer.querySelector('.slider-container');
				// Remove all elements with class 'slider-item' inside sliderContainer
				if (sliderContainer) {
					const sliderItems = sliderContainer.querySelectorAll('.slider-item');
					sliderItems.forEach(item => item.remove());
				}

				// Add new songs
				songWithAlbumDisplayed.forEach(swa => {
					sliderContainer.append(createSliderItem(swa));
				})

				renderButtons(page, totPages);

			}
		})
	}

	if (addSongForm) {
		addSongForm.addEventListener('submit', async (event) => {
			event.preventDefault();
			const selectedCheckboxes = appContainer.querySelectorAll('input[name="selected-songs"]:checked');

			const errorDiv = document.getElementById('add-song-error');
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

			if (selectedCheckboxes.length > 0) { }
			const songIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.value));

			const payload = {
				songIds
			}



			try {
				const response = await addSongsToPlaylist(playlist.idPlaylist, payload)
				playlist.songs.push(...response.addedSongIds);

				playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
				orderedSongs = await getOrderedSongs(playlist, playlistOrder);
				allSongs = await getSongs();

				//Populate the slider
				populateSlider(orderedSongs, page);

				// Form to Add songs
				generateAndPopulateForm(orderedSongs, allSongs, appContainer);

			} catch (error) {
				console.error("Song adding failed: ", error);
				if (errorDiv) {
					errorDiv.textContent = 'An error occurred while adding songs. Please try again.';
					errorDiv.style.display = 'block';
				}
			}
		})

	}


}

export async function getOrderedSongs(playlist, playlistOrder) {
	let orderedSongs;

	if (playlistOrder && playlistOrder.length > 0) {
		orderedSongs = await getListOfSongs(playlistOrder);

		if (playlistOrder.length < playlist.songs.length) {
			const allTheSongs = await getListOfSongs(playlist.songs);
			const filteredSongs = getSongsOrdered(allTheSongs.filter(swa => !playlistOrder.includes(swa.song.idSong)));
			orderedSongs.push(...filteredSongs);
		}
	} else {
		orderedSongs = await getListOfSongs(playlist.songs);
		orderedSongs = getSongsOrdered(orderedSongs);
	}
	return orderedSongs;
}

function renderSongs(appContainer, songWithAlbums) {
	const songListDiv = appContainer.querySelector('.song-list');

	// Remove all elements with class 'slider-item' inside sliderContainer
	if (songListDiv) {
		const songItems = songListDiv.querySelectorAll('.song-item');
		songItems.forEach(item => item.remove());
	}

	if (songWithAlbums) {
		songWithAlbums.forEach(swa => {
			const article = createSongArticle(appContainer, swa);
			songListDiv.appendChild(article);
		})
	}
}

function createSongArticle(appContainer, songWithAlbum) {
	const article = document.createElement('article');
	article.className = 'song-item';

	const inputEl = document.createElement('input');
	inputEl.type = 'checkbox';
	inputEl.id = 'song-select-' + songWithAlbum.song.idSong;
	inputEl.name = 'selected-songs';
	inputEl.value = songWithAlbum.song.idSong;
	inputEl.className = 'song-checkbox';

	article.appendChild(inputEl);

	const img = document.createElement('img');

	img.src = getSongImageURL(songWithAlbum.song.idSong);
	img.alt = songWithAlbum.song.title || "Song cover";
	img.onerror = () => {
		img.src = 'images/image_placeholder.png';
	};


	article.appendChild(img);

	const label = document.createElement('label');
	label.htmlFor = 'song-select-' + songWithAlbum.song.idSong;
	label.className = 'song-metadata';

	label.appendChild(createHeaderContainer(songWithAlbum.song.title, 'h3'));
	label.appendChild(createParagraphElement(songWithAlbum.album.artist + ' • ' + songWithAlbum.album.name));

	article.appendChild(label);

	return article;
}


async function getListOfSongs(songIdList) {
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

// Helper function to create a title container
function createHeaderContainer(titleText, size) { // TODO: Consider moving to a shared viewUtils.js
	const h1 = document.createElement(size);
	h1.textContent = titleText;
	return h1;
}

// Helper function to create a paragraph
function createParagraphElement(text) {
	const node = document.createElement('p');
	node.textContent = text;
	return node;
}

// Function that fills the slider with the song's cards
function populateSlider(orderedSongs, page) {
	const loaderMessage = document.querySelector('#all-songs-loader-message');
	if (loaderMessage) {
		loaderMessage.remove();
	}

	const sliderContainer = document.querySelector('.slider-container');
	// Remove all elements with class 'slider-item' inside sliderContainer
	if (sliderContainer) {
		const sliderItems = sliderContainer.querySelectorAll('.slider-item');
		sliderItems.forEach(item => item.remove());
	}

	const oldItems = document.querySelect

	if (orderedSongs) {
		let totPages = Math.ceil(orderedSongs.length / 5);
		if (page > totPages - 1) {
			page = totPages - 1;
		}

		renderButtons(page, totPages);

		let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);

		songWithAlbumDisplayed.forEach(swa => {
			sliderContainer.append(createSliderItem(swa));
		})

	}
}

function generateAndPopulateForm(orderedSongs, allSongs, appContainer) {
	const orderedSongIds = new Set(orderedSongs.map(swa => swa.song.idSong));
	const filteredSongs = allSongs.filter(swa => !orderedSongIds.has(swa.song.idSong));

	if (!filteredSongs || filteredSongs.length == 0) {
		appContainer.querySelector('.addSong').style.display = 'none';
	} else {
		appContainer.querySelector('.addSong').style.display = '';
		renderSongs(appContainer, filteredSongs);
	}
}

// Helper function to create an element of the slider
function createSliderItem(songWithAlbum) {
	const { song, album } = songWithAlbum;

	// <article class="slider-item">
	const article = document.createElement('article');
	article.classList.add('slider-item');

	// <div class="slider-image">
	const imageDiv = document.createElement('div');
	imageDiv.classList.add('slider-image');

	// <img src="images/image_placeholder.png" class="slider-thumbnail" alt="song title">
	const img = document.createElement('img');
	img.classList.add('slider-thumbnail');
	img.src = getSongImageURL(songWithAlbum.song.idSong);
	img.alt = songWithAlbum.song.title || "Song cover";
	img.onerror = () => {
		img.src = 'images/image_placeholder.png';
	};

	// <button class="card-btn">Open</button>
	const button = document.createElement('button');
	button.classList.add('card-btn');
	button.dataset.songId = songWithAlbum.song.idSong;
	button.textContent = 'Open';

	// Add img and button to imageDiv
	imageDiv.appendChild(img);
	imageDiv.appendChild(button);

	// <div class="slider-metadata">
	const metadataDiv = document.createElement('div');
	metadataDiv.classList.add('slider-metadata');

	// <h3>[Song Title Placeholder]</h3>
	const h3 = document.createElement('h3');
	h3.textContent = song.title;

	// <p>[Artist Name Placeholder] • [Album Name Placeholder]</p>
	const p1 = document.createElement('p');
	p1.textContent = `${album.artist} • ${album.name}`;

	// <p>[Genre Placeholder] • [Year Placeholder]</p>
	const p2 = document.createElement('p');
	p2.textContent = `${song.genre} • ${album.year}`;

	// Assemble metadata
	metadataDiv.appendChild(h3);
	metadataDiv.appendChild(p1);
	metadataDiv.appendChild(p2);

	// Add image and metadata to article
	article.appendChild(imageDiv);
	article.appendChild(metadataDiv);

	return article;
}
