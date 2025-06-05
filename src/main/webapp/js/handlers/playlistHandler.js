import {
    renderButtons,
    renderPlaylistView,
    renderSliderItem,
    renderSongs,
    writeSliderHeader
} from '../views/playlistView.js';
import { addSongsToPlaylist, getPlaylists, getPlaylistSongOrder, getSongDetails, getSongs } from '../apiService.js';
import { getSongsOrdered } from '../utils/orderUtils.js';
import { startPlayback } from './playerHandler.js';


/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initPlaylistPage(appContainer, params) {

    const {idplaylist} = params;

    if (!idplaylist) {
        console.error("Playlist ID is missing from params.");
        navigate('home');
        return;
    }

    // Fetch all playlists and find the current one by ID
    const allPlaylists = await getPlaylists();
    const currentPlaylistId = parseInt(idplaylist, 10);
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

    const sliderContainer = appContainer.querySelector('.slider-container');
    if (sliderContainer) {
        sliderContainer.addEventListener('click', (event) => {
            const playButton = event.target.closest('.card-btn');
            if (playButton) {
                const songIdToPlay = playButton.dataset.songId;
                if (songIdToPlay && orderedSongs && orderedSongs.length > 0) {
                    const songIdListForQueue = orderedSongs.map(swa => swa.song.idSong);
                    console.log(`Play button clicked for songId: ${songIdToPlay}. Queue:`, songIdListForQueue);
                    startPlayback(songIdToPlay, songIdListForQueue);
                } else {
                    console.warn('Could not start playback. Missing songId or orderedSongs list.');
                }
            }
        });
    }

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
                    sliderContainer.append(renderSliderItem(swa));
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
                    sliderContainer.append(renderSliderItem(swa));
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

            if (selectedCheckboxes.length > 0) {

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

// Function that fills the slider with the song's cards
function populateSlider(orderedSongs, page) {

    const sliderContainer = document.querySelector('.slider-container');
    // Remove all elements with class 'slider-item' inside sliderContainer
    if (sliderContainer) {
        const sliderItems = sliderContainer.querySelectorAll('.slider-item');
        sliderItems.forEach(item => item.remove());
    }

    if (orderedSongs) {
        let totPages = Math.ceil(orderedSongs.length / 5);
        if (page > totPages - 1) {
            page = totPages - 1;
        }

        renderButtons(page, totPages);

        let songWithAlbumDisplayed = orderedSongs.slice(page * 5, (page + 1) * 5);

        songWithAlbumDisplayed.forEach(swa => {
            sliderContainer.append(renderSliderItem(swa));
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
