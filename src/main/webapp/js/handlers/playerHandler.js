import { getSongAudioURL, getSongDetails, getSongImageURL } from '../apiService.js';


let audioPlayerElement = null;
let mainPlayerUiContainer = null;
let currentQueue = [];
let currentIndex = -1;

/**
 * Shows or hides the entire player UI.
 * @param {boolean} show - True to show, false to hide.
 */
function _setPlayerVisibility(show = true) {
    if (!mainPlayerUiContainer) {
        mainPlayerUiContainer = document.getElementById('main-player-ui-container');
        if (!mainPlayerUiContainer) {
            console.error('Main player UI container (main-player-ui-container) not found.');
            return;
        }
    }
    mainPlayerUiContainer.style.display = show ? 'block' : 'none'; // Or 'flex' if its direct children need it
}

/**
 * Clears the content of the player details section.
 */
function _clearPlayerDetails() {
    const albumCover = document.getElementById('player-album-cover');
    if (albumCover) {
        albumCover.src = 'images/image_placeholder.png';
        albumCover.alt = 'Song cover';
    }
    const songTitle = document.getElementById('player-song-title');
    if (songTitle) songTitle.textContent = 'Song Title';

    const songArtist = document.getElementById('player-song-artist');
    if (songArtist) songArtist.textContent = 'Artist: ';

    const songAlbum = document.getElementById('player-song-album');
    if (songAlbum) songAlbum.textContent = 'Album: ';

    const songYear = document.getElementById('player-song-year');
    if (songYear) songYear.textContent = 'Year: ';

    const songGenre = document.getElementById('player-song-genre');
    if (songGenre) songGenre.textContent = 'Genre: ';
}


/**
 * Initializes the global audio player and sets up event listeners.
 * This should be called once when the application loads.
 */
export function initPlayer() {
    audioPlayerElement = document.getElementById('global-audio-player');
    mainPlayerUiContainer = document.getElementById('main-player-ui-container');
    const closeButton = document.getElementById('player-close-button');

    if (!audioPlayerElement) {
        console.error('Global audio player element not found in the DOM.');
        return;
    }
    if (!mainPlayerUiContainer) {
        console.error('Main player UI container (main-player-ui-container) not found in the DOM.');
    }
    if (!closeButton) {
        console.error('Player close button element not found in the DOM.');
    }

    audioPlayerElement.addEventListener('ended', () => {
        console.log('Song ended, attempting to play next.');
        _playNextInQueue();
    });

    audioPlayerElement.addEventListener('error', (e) => {
        console.error('Error with audio player:', e);
        if (audioPlayerElement) audioPlayerElement.style.display = 'none';
        _setPlayerVisibility(false);
        _clearPlayerDetails();
    });

    if (closeButton) {
        closeButton.addEventListener('click', () => {
            if (audioPlayerElement) {
                audioPlayerElement.style.display = 'none';
                audioPlayerElement.pause();
                audioPlayerElement.currentTime = 0;
            }
            _setPlayerVisibility(false);
            _clearPlayerDetails();
        });
    }
    _setPlayerVisibility(false);
    console.log('Audio player initialized.');
}

function _decoratePlayer(songWithAlbum) {
    const { song, album } = songWithAlbum;

    const img = document.getElementById('player-album-cover');
    const title = document.getElementById('player-song-title');
    const artist = document.getElementById('player-song-artist');
    const albumName = document.getElementById('player-song-album');
    const year = document.getElementById('player-song-year');
    const genre = document.getElementById('player-song-genre');

    if (img) {
        img.src = getSongImageURL(song.idSong);
        img.alt = DOMPurify.sanitize(song.title || "Song cover", { ALLOWED_TAGS: [] });
        img.onerror = () => {
            img.src = 'images/image_placeholder.png';
            img.alt = 'Song cover placeholder';
        };
    }
    if (title) title.textContent = song.title || "Unknown Title";
    if (artist) artist.textContent = `Artist: ${album.artist || "Unknown Artist"}`;
    if (albumName) albumName.textContent = `Album: ${album.name || "Unknown Album"}`;
    if (year) year.textContent = `Year: ${album.year || "----"}`;
    if (genre) genre.textContent = `Genre: ${song.genre || "Unknown Genre"}`;
}

/**
 * Internal helper function to play a song by its ID.
 * @param {string|number} songId The ID of the song to play.
 */
async function _playSongById(songId) {
    if (!audioPlayerElement) {
        console.error('Audio player element not initialized or not found.');
        _setPlayerVisibility(false);
        return;
    }
    if (!songId) {
        console.error('No songId provided to _playSongById.');
        _setPlayerVisibility(false);
        _clearPlayerDetails();
        if (audioPlayerElement) audioPlayerElement.style.display = 'none';
        return;
    }

    try {
        const swa = await getSongDetails(songId);
        const audioURL = getSongAudioURL(songId);

        _decoratePlayer(swa);

        audioPlayerElement.src = audioURL;
        audioPlayerElement.style.display = 'block';
        _setPlayerVisibility(true);


        audioPlayerElement.play().then(_ => {
            console.log(`Playing song: ${songId}, URL: ${audioURL}`);
        }).catch(error => {
            console.error(`Error playing song ${songId}:`, error);
            if (audioPlayerElement) audioPlayerElement.style.display = 'none';
            _setPlayerVisibility(false);
            _clearPlayerDetails();
        });

    } catch (error) {
        console.error(`Failed to get audio URL or play song ${songId}:`, error);
        if (audioPlayerElement) audioPlayerElement.style.display = 'none';
        _setPlayerVisibility(false);
        _clearPlayerDetails();
    }
}

/**
 * Starts playback of a song. If a list of song IDs is provided, it sets up a queue.
 * @param {string|number} songId The ID of the song to start playing.
 * @param {Array<string|number>} [songIdList=[]] Optional array of song IDs for queueing.
 */
export function startPlayback(songId, songIdList = []) {
    if (!audioPlayerElement || !mainPlayerUiContainer) {
        initPlayer();
        if (!audioPlayerElement) {
            console.error("Audio player could not be initialized. Aborting playback.");
            _setPlayerVisibility(false);
            return;
        }
    }

    if (songIdList && songIdList.length > 0) {
        currentQueue = [...songIdList];
        const initialIndex = currentQueue.findIndex(id => String(id) === String(songId));
        currentIndex = (initialIndex !== -1) ? initialIndex : 0;
    } else {
        currentQueue = [songId];
        currentIndex = 0;
    }

    if (currentQueue.length > 0 && currentIndex < currentQueue.length) {
        const firstSongIdToPlay = currentQueue[currentIndex];
        console.log('Starting playback. Queue:', currentQueue, 'Current index:', currentIndex, 'Song ID:', firstSongIdToPlay);
        _playSongById(firstSongIdToPlay);
    } else {
        console.error('Cannot start playback: queue is empty or index is out of bounds.');
        _setPlayerVisibility(false);
        _clearPlayerDetails();
        if (audioPlayerElement) audioPlayerElement.style.display = 'none';
    }
}

/**
 * Plays the next song in the current queue.
 * Called automatically when a song ends.
 */
function _playNextInQueue() {
    if (currentQueue.length === 0 || currentIndex >= currentQueue.length - 1) {
        console.log('End of queue or no queue.');
        if (audioPlayerElement) audioPlayerElement.style.display = 'none';
        _setPlayerVisibility(false);
        _clearPlayerDetails();
        currentQueue = [];
        currentIndex = -1;
        return;
    }

    currentIndex++;
    const nextSongId = currentQueue[currentIndex];
    console.log('Playing next in queue. Index:', currentIndex, 'Song ID:', nextSongId);
    _playSongById(nextSongId);
}

/**
 * Stops the current playback, clears the queue, and hides the player.
 */
export function stopPlayback() {
    if (!audioPlayerElement) {
        console.warn('Audio player element not initialized or not found. Cannot stop playback.');
        if (mainPlayerUiContainer) _setPlayerVisibility(false);
        return;
    }

    audioPlayerElement.pause();
    audioPlayerElement.src = '';
    audioPlayerElement.style.display = 'none';

    _setPlayerVisibility(false);
    _clearPlayerDetails();

    currentQueue = [];
    currentIndex = -1;

    console.log('Playback stopped and player hidden.');
}
