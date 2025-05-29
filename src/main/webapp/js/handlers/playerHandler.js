import { getSongAudioURL } from '../apiService.js';

let audioPlayerElement = null;
let currentQueue = [];
let currentIndex = -1;

/**
 * Initializes the global audio player and sets up event listeners.
 * This should be called once when the application loads.
 */
export function initPlayer() {
    audioPlayerElement = document.getElementById('global-audio-player');

    if (!audioPlayerElement) {
        console.error('Global audio player element not found in the DOM. Make sure it exists in index.html.');
        return;
    }

    audioPlayerElement.addEventListener('ended', () => {
        console.log('Song ended, attempting to play next.');
        playNextInQueue();
    });

    audioPlayerElement.addEventListener('error', (e) => {
        console.error('Error with audio player:', e);

        audioPlayerElement.style.display = 'none';
    });

    console.log('Audio player initialized.');
}

/**
 * Internal helper function to play a song by its ID.
 * @param {string|number} songId The ID of the song to play.
 */
function _playSongById(songId) {
    if (!audioPlayerElement) {
        console.error('Audio player element not initialized or not found.');
        return;
    }
    if (!songId) {
        console.error('No songId provided to _playSongById.');
        return;
    }

    try {
        const audioURL = getSongAudioURL(songId);
        audioPlayerElement.src = audioURL;
        audioPlayerElement.style.display = 'block';

        const playPromise = audioPlayerElement.play();
        if (playPromise !== undefined) {
            playPromise.then(_ => {
                console.log(`Playing song: ${songId}, URL: ${audioURL}`);
            }).catch(error => {
                console.error(`Error playing song ${songId}:`, error);
                audioPlayerElement.style.display = 'none';
            });
        }
    } catch (error) {
        console.error(`Failed to get audio URL or play song ${songId}:`, error);
        audioPlayerElement.style.display = 'none';
    }
}

/**
 * Starts playback of a song. If a list of song IDs is provided, it sets up a queue.
 * @param {string|number} songId The ID of the song to start playing.
 * @param {Array<string|number>} [songIdList=[]] Optional array of song IDs for queueing.
 */
export function startPlayback(songId, songIdList = []) {
    if (!audioPlayerElement) {
        initPlayer();
        if (!audioPlayerElement) return;
    }

    if (songIdList && songIdList.length > 0) {
        currentQueue = [...songIdList];
        const initialIndex = currentQueue.findIndex(id => id === songId || String(id) === String(songId));
        currentIndex = (initialIndex !== -1) ? initialIndex : 0; // Default to first if songId not in list
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
    }
}

/**
 * Plays the next song in the current queue.
 * Called automatically when a song ends.
 */
function playNextInQueue() {
    if (currentQueue.length === 0 || currentIndex >= currentQueue.length - 1) {
        console.log('End of queue or no queue.');
        audioPlayerElement.style.display = 'none';
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
        return;
    }

    audioPlayerElement.pause();
    audioPlayerElement.src = '';
    audioPlayerElement.style.display = 'none';

    currentQueue = [];
    currentIndex = -1;

    console.log('Playback stopped and player hidden.');
}
