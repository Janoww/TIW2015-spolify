import { renderPlaylistView } from '../views/playlistView.js';
import { getPlaylistSongOrder, getSongDetails, getSongImageURL, getSongs } from '../apiService.js';

export async function initPlaylistPage(appContainer, playlist) {
	const navbar = document.getElementById('navbar');
	if (navbar) {
		navbar.style.display = 'inline-block';
	} else {
		console.error("Navbar element not found in initHomePage.");
	}

	const playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
	let orderedSongs = undefined;
	if (playlistOrder && playlistOrder.length > 0) {
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

	const allSongs = await getSongs();

	renderPlaylistView(appContainer);

	//Write title
	const sliderHeader = document.getElementById('sliderHeader');
	if (sliderHeader) {
		sliderHeader.textContent = 'Songs in "' + playlist.name + '"'
	}

	//Populate the slider
	const loaderMessage = document.querySelector('#all-songs-loader-message');
	if (loaderMessage) {
		loaderMessage.remove();
	}

	const sliderContainer = document.querySelector('.slider-container');
	let page = 0;
	if (orderedSongs) {
		let totPages = Math.ceil(orderedSongs.length / 5);
		if (page > totPages - 1) {
			page = totPages - 1;
		}

		if (page === 0) {
			const button = document.querySelector('.pre-carouselButton');
			button.style.display = 'none';

		} else {
			const button = document.querySelector('.pre-carouselButton');
			button.style.display = '';

		}
		if (page === totPages - 1) {
			const button = document.querySelector('.next-carouselButton');
			button.style.display = 'none';

		} else {
			const button = document.querySelector('.pre-carouselButton');
			button.style.display = '';
		}



		let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);


		songWithAlbumDisplayed.forEach(swa => {
			sliderContainer.append(createSliderItem(swa));
		})

	}

	// Form to Add songs

	const orderedSongIds = new Set(orderedSongs.map(swa => swa.song.idSong));
	const filteredSongs = allSongs.filter(swa => !orderedSongIds.has(swa.song.idSong));

	if (!filteredSongs || filteredSongs.length == 0) {
		document.querySelector('.addSong').style.display = 'none';
	} else {
		document.querySelector('.addSong').style.display = '';
		renderSongs(appContainer, filteredSongs);
	}


}

function renderSongs(appContainer, songWithAlbums) {

	console.log();

	const songListDiv = appContainer.querySelector('.song-list');

	if (songWithAlbums) {
		songWithAlbums.forEach(swa => {
			const article = createSongArticle(swa);
			songListDiv.appendChild(article);
		})
	}
}

function createSongArticle(songWithAlbum) {
	const article = document.createElement('article');
	article.className = 'song-item';

	const inputEl = document.createElement('input');
	inputEl.type = 'checkbox';
	inputEl.id = 'song-select-' + songWithAlbum.song.idSong;
	inputEl.name = 'selected-songs';
	inputEl.value = songWithAlbum.song.idSong;
	inputEl.className = 'song-checkbox';

	article.appendChild(inputEl);

	//TODO image
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
	p2.textContent = `${song.genre} • ${song.year}`;

	// Assemble metadata
	metadataDiv.appendChild(h3);
	metadataDiv.appendChild(p1);
	metadataDiv.appendChild(p2);

	// Add image and metadata to article
	article.appendChild(imageDiv);
	article.appendChild(metadataDiv);

	return article;
}