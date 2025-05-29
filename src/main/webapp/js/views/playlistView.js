import { createElement, createHeaderContainer, createParagraphElement } from '../utils/viewUtils.js';
import { createSongArticleWithCheckboxElement } from './sharedComponents.js';

// Navigation buttons
function createNavButton(className, imgSrc, altText) {
    const btn = createElement('button', { className: className });
    const wrapper = createElement('div', { className: 'arrow-wrapper' });
    const img = createElement('img', { className: 'arrow', attributes: { src: imgSrc, alt: altText } });

    wrapper.appendChild(img);
    btn.appendChild(wrapper);

    return btn;
}

export function renderPlaylistView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

    // Section 1: slider
    const sliderSection = createElement('section', { className: 'slider' });

    // 1.1 Header
    const sliderHeader = createHeaderContainer('', 'h2');
    sliderHeader.id = 'sliderHeader';
    sliderSection.appendChild(sliderHeader);

    // 1.2 SlideShow
    const sliderContainer = createElement('div', { className: 'slider-container' });

    // Placeholder for song items
    sliderContainer.appendChild(createNavButton('pre-carouselButton', 'images/circle-left-regular.svg', '<'));
    sliderContainer.appendChild(createNavButton('next-carouselButton', 'images/circle-right-regular.svg', '>'));

    sliderSection.appendChild(sliderContainer);
    appContainer.appendChild(sliderSection);

    // Section 2: Add song
    const addSongSection = createElement('section', { className: 'addSong' });

    // 2.1 Header
    addSongSection.appendChild(createHeaderContainer('Add Song', 'h2'));

    // 2.2: Form
    const addSongsForm = createElement('form', { id: 'add-song-form' });

    // Input: song list
    addSongsForm.appendChild(createHeaderContainer('Select Songs to Add:', 'h3'));
    const songListDiv = createElement('div', { className: 'song-list' });
    songListDiv.style.maxHeight = '20vh';
    addSongsForm.appendChild(songListDiv);

    // Button
    const addSongSendButton = createElement('button', {
        textContent: 'Add Song',
        className: 'styled-button',
        attributes: { type: 'submit' }
    });
    addSongsForm.appendChild(addSongSendButton);

    // Error
    const errorDiv = createElement('div', { className: 'error-message' });
    errorDiv.id = 'add-song-error';
    addSongsForm.appendChild(errorDiv);

    addSongSection.appendChild(addSongsForm);
    appContainer.appendChild(addSongSection);
}

// Function that writes the text on the header of the slider
export function writeSliderHeader(text) {
    const sliderHeader = document.getElementById('sliderHeader');
    if (sliderHeader) {
        sliderHeader.textContent = 'Songs in "' + text + '"'
    }
}

// Function that hide or render the buttons when needed
export function renderButtons(page, totPages) {

    if (page === 0) {
        const button = document.querySelector('.pre-carouselButton');
        button.classList.add('hidden');
        console.log('pre-Page:', page);

    } else {
        const button = document.querySelector('.pre-carouselButton');
        button.classList.remove('hidden');
        console.log('preA-Page:', page);

    }
    if (page === totPages - 1) {
        const button = document.querySelector('.next-carouselButton');
        button.classList.add('hidden');
        console.log('nxt-Page:', page);


    } else {
        const button = document.querySelector('.next-carouselButton');
        button.classList.remove('hidden');
        console.log('nxtA-Page:', page);

    }

}

/**
 *
 * @param {HTMLElement} appContainer - The container of the entire app
 * @param {Object} songWithAlbums - List of songs to render
 */
export function renderSongs(appContainer, songWithAlbums) {
    const songListDiv = appContainer.querySelector('.song-list');

    // Remove all elements with class 'slider-item' inside sliderContainer
    if (songListDiv) {
        const songItems = songListDiv.querySelectorAll('.song-item');
        songItems.forEach(item => item.remove());
    }

    if (songWithAlbums) {
        songWithAlbums.forEach(swa => {
            const article = createSongArticleWithCheckboxElement(swa);
            songListDiv.appendChild(article);
        })
    }
}
