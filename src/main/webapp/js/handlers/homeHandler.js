
import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection, createReorderPopup, populateModal } from "../views/homeView.js";
import { getPlaylists, createPlaylist, getSongs, getSongGenres, updatePlaylistOrder, getPlaylistSongOrder } from '../apiService.js';
import { getOrderedSongs } from './playlistHandler.js';
import { navigate } from '../router.js';
import { validateForm } from '../utils/formUtils.js';
import { handleSongUploadSubmit } from './sharedFormHandlers.js';
import { extractUniqueAlbumSummaries, addAlbumSummaryIfNew } from '../utils/orderUtils.js';


/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initHomePage(appContainer) {

    // Load basic elements
    renderHomeView(appContainer);

    const songFormSectionContainer = appContainer.querySelector('#add-song-section');
    let genres = null;
    let genreError = null;
    let albumSummaries = [];

    // Load and render playlists
    let playlists;
    try {
        playlists = await getPlaylists();
        playlists.sort((a, b) => new Date(b.birthday) - new Date(a.birthday));
        renderPlaylists(appContainer, playlists);
    } catch (error) {
        console.error(`Error loading or rendering playlists: Status ${error.status}, Message: ${error.message}`, error.details || '');
        // Display error in playlist section if possible
    }

    // Load and render songs for the "Create New Playlist" form's song selection
    // Also derive albumSummaries from these songs
    let songsForPlaylistSelection;
    try {
        songsForPlaylistSelection = await getSongs();
        renderSongs(appContainer, songsForPlaylistSelection);

        albumSummaries = extractUniqueAlbumSummaries(songsForPlaylistSelection);
    } catch (error) {
        console.error(`Error loading or rendering songs for playlist creation: Status ${error.status}, Message: ${error.message}`, error.details || '');
    }
	//Modal


    try {
        genres = await getSongGenres();
    } catch (error) {
        console.error(`Failed to load genres for Home Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreError = error;
    }

    renderSongUploadSection(songFormSectionContainer, genres, albumSummaries, genreError);

    // Events


    const newSongForm = document.getElementById('add-song-form-home');
    const newPlaylistForm = document.getElementById('create-playlist-form');
    const playlistsList = document.querySelector('.playlist-list')


    if (newPlaylistForm) {
        newPlaylistForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fieldIds = ['new-playlist-title'];
            const selectedCheckboxes = document.querySelectorAll('input[name="selected-songs"]:checked');


            const errorDiv = document.getElementById('create-playlist-error');
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

            if (validateForm(newPlaylistForm, fieldIds) && selectedCheckboxes.length > 0) {
                const form = event.target;


                const name = form['new-playlist-title'].value;
                const songIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.value));

                const payload = {
                    name,
                    songIds
                };

                try {
                    const newPlaylist = await createPlaylist(payload);
                    console.log("Playlist created:", newPlaylist);

                    playlists instanceof Array && playlists.unshift(newPlaylist);
                    renderPlaylists(appContainer, playlists);
                    newPlaylistForm.reset();

                } catch (error) {
                    console.error(`Playlist creation failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    if (errorDiv) {
                        errorDiv.textContent = 'An error occurred while adding songs. Please try again.';
                        errorDiv.style.display = 'block';
                    }
                }

            } else {
                console.log('NewPlaylist form has errors or no songs selected.');
            }
        })
    }

    // Setting up Events listeners for SongForm
    if (newSongForm) {
        const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
        const errorDivId = 'create-song-error';

        // Success callback specific to the home page
        function homePageSongUploadSuccess(newSongWithAlbum, appContainerForRender, currentSongsList) {
            // Update currentSongsList for the playlist creation section
            if (currentSongsList && Array.isArray(currentSongsList)) {
                currentSongsList.unshift(newSongWithAlbum);
            }
            if (appContainerForRender && currentSongsList) {
                renderSongs(appContainerForRender, currentSongsList);
            }

            // Update albumSummaries and re-render song upload form
            if (newSongWithAlbum.album.name) {
                const result = addAlbumSummaryIfNew(albumSummaries, newSongWithAlbum.album);
                if (result.wasAdded) {
                    albumSummaries = result.updatedSummaries;
                    // Re-render the song upload section with updated albumSummaries
                    const formSection = appContainerForRender.querySelector('#add-song-section');
                    if (formSection) {
                        renderSongUploadSection(formSection, genres, albumSummaries, genreError);

                        const newForm = document.getElementById('add-song-form-home');
                        if (newForm) {
                            newForm.addEventListener('submit', newSongFormSubmitHandler);
                        }
                    }
                }
            }
        }

        // Define the event handler separately to re-attach if the form is re-rendered
        async function newSongFormSubmitHandler(event) {
            await handleSongUploadSubmit(
                event,
                fieldIds,
                errorDivId,
                homePageSongUploadSuccess,
                appContainer,
                songsForPlaylistSelection
            );
        }

        newSongForm.addEventListener('submit', newSongFormSubmitHandler);
    }

    if (playlistsList) {
        playlistsList.addEventListener('click', async event => {
            const target = event.target;

            if (target.classList.contains('view-playlist-button')) {
                const playlistId = parseInt(target.dataset.playlistId, 10);
                if (playlistId) {
                    navigate('playlist-' + playlistId);
                } else {
                    console.error("Playlist ID not found for navigation.");
                }
            }

            if (target.classList.contains('reorder-playlist-button')) {
                const playlistId = parseInt(target.dataset.playlistId, 10);

                if (playlistId) {
                    const modal = document.getElementById('reorderModal'); 
					modal.style.display = 'block';
                    const playlist = playlists.find(playlist => playlist.idPlaylist === playlistId);

                    let playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
                    let orderedSongs = await getOrderedSongs(playlist, playlistOrder);
                    populateModal(orderedSongs, modal.firstChild);
					
                    // 🔗 Add event listeners
                    const closeButton = modal.querySelector('#closeReorderModal');
                    const cancelButton = modal.querySelector('#cancelOrderButton');
                    const saveButton = modal.querySelector('#saveOrderButton');

                    closeButton.addEventListener("click", () => {
                        modal.style.display = 'none';
						modal.querySelectorAll('.reorder-song-item').forEach(item => item.remove());
                    });
                    cancelButton.addEventListener("click", () => {
                        populateModal(orderedSongs, modal.firstChild);
                    });
                    saveButton.addEventListener("click", async () => {
						const songListItems = Array.from(modal.querySelectorAll('.reorder-song-item'));
						console.log(songListItems);
                        const reorderedIds = songListItems.map(li => li.getAttribute("data-song-id"));
						
						console.log("New order:", reorderedIds);

                        try {
                            const response = await updatePlaylistOrder(playlistId, reorderedIds);
                        } catch (error) {
                            //TODO to handle
                        }

						modal.style.display = 'none';
						modal.querySelectorAll('.reorder-song-item').forEach(item => item.remove());
                    });

                    const reorderSongList = document.getElementById('reorderSongList');
                    let draggedItem = null;

                    reorderSongList.addEventListener('dragstart', (event) => {
                        if (event.target.classList.contains('reorder-song-item')) {
                            draggedItem = event.target;
                            let allItemsButOne = Array.from(document.querySelectorAll('.reorder-song-item'));
                            allItemsButOne = allItemsButOne.filter(item => item !== draggedItem);
                            allItemsButOne.forEach(item => item.classList.add('no-hover'));

                            event.dataTransfer.setData('text/plain', event.target.dataset.songId);
                            setTimeout(() => {
                                event.target.classList.add('dragging');
                            }, 0);
                        }
                    });

                    reorderSongList.addEventListener('dragend', () => {
                        if (draggedItem) {
                            const allItems = Array.from(document.querySelectorAll('.reorder-song-item'));
                            allItems.forEach(item => item.classList.remove('no-hover'));

                            draggedItem.classList.remove('dragging');
                            draggedItem = null;
                        }
                    });


                    reorderSongList.addEventListener('dragover', (event) => {
                        event.preventDefault();
                        const targetItem = event.target.closest('.reorder-song-item');
                        if (targetItem && targetItem !== draggedItem) {
                            const bounding = targetItem.getBoundingClientRect();
                            const offset = event.clientY - (bounding.top + bounding.height / 2);
                            const parent = reorderSongList;

                            if (offset > 0) {
                                parent.insertBefore(draggedItem, targetItem.nextSibling);
                            } else {
                                parent.insertBefore(draggedItem, targetItem);
                            }
                        }
                    });

                    reorderSongList.addEventListener('drop', (event) => {
                        event.preventDefault(); // Prevent default action (open as link for some elements)
                        // The reordering is handled in dragover for immediate visual feedback.
                        // If an item was being dragged, its 'dragging' class is removed in dragend.
                    });



                }











            }
        });
    }
}
