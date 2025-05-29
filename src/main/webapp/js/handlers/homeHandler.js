import {
    populateModal,
    renderHomeView,
    renderPlaylists,
    renderSongs,
    renderSongUploadSection
} from "../views/homeView.js";
import {
    createPlaylist,
    getPlaylists,
    getPlaylistSongOrder,
    getSongGenres,
    getSongs,
    updatePlaylistOrder
} from '../apiService.js';
import {getOrderedSongs} from './playlistHandler.js';
import {navigate} from '../router.js';
import {validateForm} from '../utils/formUtils.js';
import {handleSongUploadSubmit} from './sharedFormHandlers.js';
import {addAlbumSummaryIfNew, extractUniqueAlbumSummaries} from '../utils/orderUtils.js';

/**
 * Initializes the Home Page.
 * Loads and renders the main view, playlists, songs for selection, and the song upload form.
 * Sets up event listeners for various interactions on the home page.
 * @param {HTMLElement} appContainer - The main container element of the application.
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

    // Setting up event listeners for new playlist form
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

        /**
         * Success callback for song uploads specifically from the home page.
         * Updates the list of songs available for playlist creation and re-renders the song upload form
         * if a new album was effectively created.
         * @param {Object} newSongWithAlbum - The newly created song object, including album details.
         * @param {HTMLElement} appContainerForRender - The application container for re-rendering parts of the view.
         * @param {Array<Object>} currentSongsList - The current list of songs to be updated.
         */
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
                            // Re-attach the same handler to the new form instance
                            newForm.addEventListener('submit', newSongFormSubmitHandler);
                        }
                    }
                }
            }
        }

        /**
         * Event handler for the new song form submission on the home page.
         * Delegates to the shared `handleSongUploadSubmit` function.
         * @param {Event} event - The form submission event.
         */
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

    // Setting up Event listeners for Playlist section
    if (playlistsList) {
        playlistsList.addEventListener('click', (event) => handlePlaylistListClick(event, playlists));
    }
}

// --- Helper Functions for Playlist List Event Handling ---

/**
 * Handles the click event on a "View Playlist" button.
 * Navigates to the specific playlist page.
 * @param {HTMLElement} target - The clicked HTML element (should be the button).
 */
function handleViewPlaylistButtonClick(target) {
    const playlistId = parseInt(target.dataset.playlistId, 10);
    if (playlistId) {
        navigate('playlist-' + playlistId);
    } else {
        console.error("Playlist ID not found for navigation.");
    }
}

/**
 * Sets up event listeners for the reorder playlist modal.
 * This includes listeners for close, cancel, save, and drag-and-drop functionality.
 * @async
 * @param {HTMLElement} modal - The modal element.
 * @param {number} playlistId - The ID of the playlist being reordered.
 * @param {Array<Object>} initialOrderedSongs - The initial list of songs in their current order, used for cancellation.
 */
async function setupReorderModalEventListeners(modal, playlistId, initialOrderedSongs) {
    const closeButton = modal.querySelector('#closeReorderModal');
    const cancelButton = modal.querySelector('#cancelOrderButton');
    const saveButton = modal.querySelector('#saveOrderButton');
    const reorderSongList = document.getElementById('reorderSongList');

    /**
     * Handles closing the reorder modal and cleaning up song items.
     */
    const closeModalHandler = () => {
        modal.style.display = 'none';
        modal.querySelectorAll('.reorder-song-item').forEach(item => item.remove());
    };

    /**
     * Handles the cancel action in the reorder modal.
     * Repopulates the modal with the initial order of songs.
     */
    const cancelOrderHandler = () => {
        populateModal(initialOrderedSongs, modal.querySelector('.modal-content'));
    };

    /**
     * Handles the save action in the reorder modal.
     * Updates the playlist order on the server and closes the modal.
     */
    const saveOrderHandler = async () => {
        const songListItems = Array.from(modal.querySelectorAll('.reorder-song-item'));
        const reorderedIds = songListItems.map(li => li.getAttribute("data-song-id"));

        console.log("New order:", reorderedIds);

        try {
            await updatePlaylistOrder(playlistId, reorderedIds);
            // Optionally, provide user feedback on successful save
        } catch (error) {
            console.error(`Failed to update playlist order: Status ${error.status}, Message: ${error.message}`, error.details || '');
            alert(`Failed to update playlist order: Status ${error.status}, Message: ${error.message}`);
        }

        closeModalHandler();
    };

    // Clear previous listeners if any, or ensure elements are fresh
    // For simplicity here, we assume fresh elements or that listeners are okay to re-add if modal is simple
    closeButton.onclick = closeModalHandler; // Use onclick for simplicity or manage add/removeEventListener
    cancelButton.onclick = cancelOrderHandler;
    saveButton.onclick = saveOrderHandler;


    let draggedItem = null;

    // Drag and Drop event listeners
    /**
     * Handles the dragstart event for a song item in the reorder list.
     * @param {DragEvent} event - The dragstart event.
     */
    const dragStartHandler = (event) => {
        if (event.target.classList.contains('reorder-song-item')) {
            draggedItem = event.target;
            let allItemsButOne = Array.from(reorderSongList.querySelectorAll('.reorder-song-item'));
            allItemsButOne = allItemsButOne.filter(item => item !== draggedItem);
            allItemsButOne.forEach(item => item.classList.add('no-hover'));

            event.dataTransfer.setData('text/plain', event.target.dataset.songId);
            setTimeout(() => {
                event.target.classList.add('dragging');
            }, 0);
        }
    };

    /**
     * Handles the dragend event for a song item.
     * Cleans up styles and resets the dragged item.
     */
    const dragEndHandler = () => {
        if (draggedItem) {
            const allItems = Array.from(reorderSongList.querySelectorAll('.reorder-song-item'));
            allItems.forEach(item => item.classList.remove('no-hover'));

            draggedItem.classList.remove('dragging');
            draggedItem = null;
        }
    };

    /**
     * Handles the dragover event on the reorder list.
     * Allows dropping and reorders items visually.
     * @param {DragEvent} event - The dragover event.
     */
    const dragOverHandler = (event) => {
        event.preventDefault();
        const targetItem = event.target.closest('.reorder-song-item');
        if (targetItem && targetItem !== draggedItem && draggedItem) {
            const bounding = targetItem.getBoundingClientRect();
            const offset = event.clientY - (bounding.top + bounding.height / 2);
            const parent = reorderSongList;

            if (offset > 0) {
                parent.insertBefore(draggedItem, targetItem.nextSibling);
            } else {
                parent.insertBefore(draggedItem, targetItem);
            }
        }
    };

    /**
     * Handles the drop event on the reorder list.
     * Prevents default browser action.
     * @param {DragEvent} event - The drop event.
     */
    const dropHandler = (event) => {
        event.preventDefault();
    };

    // It's good practice to remove old listeners before adding new ones if this function can be called multiple times on the same elements.
    // However, if reorderSongList is recreated or listeners are on modal elements that are recreated, this might not be strictly necessary.
    // For simplicity, direct assignment or addEventListener is shown. Consider a more robust listener management strategy for complex SPAs.
    reorderSongList.removeEventListener('dragstart', dragStartHandler); // Remove previous if any
    reorderSongList.addEventListener('dragstart', dragStartHandler);

    reorderSongList.removeEventListener('dragend', dragEndHandler);
    reorderSongList.addEventListener('dragend', dragEndHandler);

    reorderSongList.removeEventListener('dragover', dragOverHandler);
    reorderSongList.addEventListener('dragover', dragOverHandler);

    reorderSongList.removeEventListener('drop', dropHandler);
    reorderSongList.addEventListener('drop', dropHandler);
}

/**
 * Handles the click event on a "Reorder Playlist" button.
 * Displays the reorder modal, fetches playlist songs, and sets up modal event listeners.
 * @param {HTMLElement} target - The clicked HTML element (should be the button).
 * @param {Array<Object>} playlists - The list of all playlists.
 */
async function handleReorderPlaylistButtonClick(target, playlists) {
    const playlistId = parseInt(target.dataset.playlistId, 10);

    if (playlistId) {
        const modal = document.getElementById('reorderModal');
        if (!modal) {
            console.error("Reorder modal not found");
            return;
        }
        modal.style.display = 'block';
        const playlist = playlists.find(p => p.idPlaylist === playlistId);

        if (!playlist) {
            console.error("Playlist not found for reordering:", playlistId);
            modal.style.display = 'none';
            return;
        }

        try {
            const playlistOrder = await getPlaylistSongOrder(playlist.idPlaylist);
            const orderedSongs = await getOrderedSongs(playlist, playlistOrder);

            // Ensure modal content area is correctly targeted for populateModal
            const modalContentElement = modal.querySelector('.modal-content');
            if (!modalContentElement) {
                console.error("Modal content area not found for populating songs.");
                modal.style.display = 'none';
                return;
            }
            populateModal(orderedSongs, modalContentElement);

            await setupReorderModalEventListeners(modal, playlistId, orderedSongs);
        } catch (error) {
            console.error(`Error preparing reorder modal for playlist ${playlistId}:`, error);
            // Display error to user, potentially hide modal
            modal.style.display = 'none';
        }
    } else {
        console.error("Playlist ID not found for reorder button.");
    }
}

/**
 * Handles click events on the playlist list.
 * Delegates to specific handlers based on the clicked button.
 * @param {Event} event - The click event.
 * @param {Array<Object>} playlists - The list of all playlists.
 */
async function handlePlaylistListClick(event, playlists) {
    const target = event.target;

    if (target.classList.contains('view-playlist-button')) {
        handleViewPlaylistButtonClick(target);
    } else if (target.classList.contains('reorder-playlist-button')) {
        // No appContainer needed here directly, it's handled by view functions if they re-render
        await handleReorderPlaylistButtonClick(target, playlists);
    }
}
