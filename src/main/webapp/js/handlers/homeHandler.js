
import { renderHomeView, renderPlaylists, renderSongs, renderSongUploadSection, createReorderPopup, populateModal } from "../views/homeView.js";
import { getPlaylists, createPlaylist, getSongs, uploadSong, getSongGenres as apiGetSongGenres, updatePlaylistOrder, getPlaylistSongOrder } from '../apiService.js';
import { getOrderedSongs } from './playlistHandler.js';
import { navigate } from '../router.js';
import { validateForm } from '../utils/formUtils.js';
import { handleSongUploadSubmit } from './sharedFormHandlers.js';


/**
 *
 * @param {HTMLElement} appContainer
 */
export async function initHomePage(appContainer) {

    renderHomeView(appContainer);


    const songFormSectionContainer = appContainer.querySelector('#add-song-section');
    let genres = null;
    let genreError = null;
    try {
        genres = await apiGetSongGenres();
    } catch (error) {
        console.error(`Failed to load genres for Home Page song form: Status ${error.status}, Message: ${error.message}`, error.details || '');
        genreError = error;
    }
    renderSongUploadSection(songFormSectionContainer, genres, genreError);


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
    let songsForPlaylistSelection;
    try {
        songsForPlaylistSelection = await getSongs();
        renderSongs(appContainer, songsForPlaylistSelection);
    } catch (error) {
        console.error(`Error loading or rendering songs for playlist creation: Status ${error.status}, Message: ${error.message}`, error.details || '');
    }

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

    if (newSongForm) {
        const fieldIds = ['song-title', 'album-title', 'album-artist', 'album-year', 'album-image', 'song-genre', 'song-audio'];
        const errorDivId = 'create-song-error';

        // Success callback specific to the home page
        function homePageSongUploadSuccess(newSong, appContainerForRender, currentSongsList) {
            if (currentSongsList && Array.isArray(currentSongsList)) {
                currentSongsList.unshift(newSong);
            }
            // Ensure appContainerForRender and currentSongsList are valid before rendering
            if (appContainerForRender && currentSongsList) {
                renderSongs(appContainerForRender, currentSongsList);
            }
        }

        newSongForm.addEventListener('submit', async (event) => {
            await handleSongUploadSubmit(
                event,
                fieldIds,
                errorDivId,
                homePageSongUploadSuccess,
                appContainer,
                songsForPlaylistSelection
            );
        });
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
                    const modal = document.getElementById('reorderModal'); //TODO to add
                    const modalContent = createReorderPopup();

                    const playlist = playlists.find(playlist => playlist.idPlaylist === playlistId);

                    //TODO populate modalContent
                    let playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
                    let orderedSongs = await getOrderedSongs(playlist, playlistOrder);

                    console.log(modalContent.querySelector('#reorderSongList'));


                    populateModal(orderedSongs, modalContent);

                    modal.appendChild(modalContent);

                    // 🔗 Add event listeners
                    const closeButton = modalContent.querySelector('#closeReorderModal');
                    const cancelButton = modalContent.querySelector('#cancelOrderButton');
                    const saveButton = modalContent.querySelector('#saveOrderButton');

                    closeButton.addEventListener("click", () => {
                        modalContent.remove()
                        modal.style.display = 'hidden';
                    });
                    cancelButton.addEventListener("click", () => {
                        modalContent.remove();
                        modalContent = createReorderPopup();
                        modal.appendChild(modalContent);
                    });
                    saveButton.addEventListener("click", async () => {
                        const reorderedIds = Array.from(songList.children).map(li => li.getAttribute("data-song-id"));
                        try {
                            const response = await updatePlaylistOrder(playlistId, reorderedIds);

                        } catch (error) {
                            //TODO to handle
                        }

                        console.log("New order:", reorderedIds);
                        modalContent.remove();
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
