
import { createFormField } from '../utils/formUtils.js';

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

// Navigation buttons
function createNavButton(className, imgSrc, altText) {
	const btn = document.createElement('button');
	btn.className = className;

	const wrapper = document.createElement('div');
	wrapper.className = 'arrow-wrapper';

	const img = document.createElement('img');
	img.className = 'arrow';
	img.src = imgSrc;
	img.alt = altText;

	wrapper.appendChild(img);
	btn.appendChild(wrapper);

	return btn;
}

export function renderPlaylistView(appContainer){
	appContainer.innerHTML = '';
	appContainer.style.maxWidth = '100%';
	
	// Section 1: slider
	const sliderSection = document.createElement('section');
	sliderSection.className = 'slider';
	
	// 1.1 Header
	const sliderHeader = createHeaderContainer('', 'h2');
	sliderHeader.id = 'sliderHeader';
	sliderSection.appendChild(sliderHeader);
	
	// 1.2 SlideShow
	const sliderContainer = document.createElement('div');
	sliderContainer.className = 'slider-container';
	

	// Placeholder for song items
	const loadingSongsP = document.createElement('p');
	loadingSongsP.id = 'all-songs-loader-message';
	loadingSongsP.textContent = 'Loading songs...';
	sliderContainer.appendChild(loadingSongsP);
	
	sliderContainer.appendChild(createNavButton('pre-carouselButton', 'images/circle-left-regular.svg', '<'));
	sliderContainer.appendChild(createNavButton('next-carouselButton', 'images/circle-right-regular.svg', '>'));
	
	sliderSection.appendChild(sliderContainer);
	appContainer.appendChild(sliderSection);

	// Section 2: Add song
	const addSongSection = document.createElement('section');
	addSongSection.className = 'addSong';

	// 2.1 Header
	addSongSection.appendChild(createHeaderContainer('Add Song', 'h2'));
	
	// 2.2: Form
	const addSongsForm = document.createElement('form');
	addSongsForm.id = 'add-song-form';

	// Input: song list
	addSongsForm.appendChild(createHeaderContainer('Select Songs to Add:', 'h3'));
	const songListDiv = document.createElement('div');
	songListDiv.className = 'song-list';
	addSongsForm.appendChild(songListDiv);
	
	appContainer.appendChild(addSongsForm);

}