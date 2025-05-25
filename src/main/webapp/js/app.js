import { initHomePage, initSongPage } from './handlers/homeHandler.js';
import { initLoginPage, logoutUser } from './handlers/loginHandler.js';

const appContainer = document.getElementById('app');

async function checkUserSessionAndInitialize() {
    if (!appContainer) {
        console.error('App container not found!');
        return;
    }

    const delayPromise = new Promise(resolve => setTimeout(resolve, 300));
    const sessionCheckPromise = fetch('api/v1/auth/me', {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    });

    try {
        const [, response] = await Promise.all([delayPromise, sessionCheckPromise]);

        if (response.ok) {
            const userData = await response.json();
            console.log('Active session found for user:', userData.username);
            sessionStorage.setItem('currentUser', JSON.stringify(userData));
            initHomePage(appContainer);
        } else if (response.status === 401) {
            console.log('No active session found or user not authenticated.');
            sessionStorage.removeItem('currentUser');
            initLoginPage(appContainer);
        } else {
            // Other unexpected errors
            console.error('Error checking session:', response.status, await response.text());
            sessionStorage.removeItem('currentUser');
            initLoginPage(appContainer);
        }
    } catch (error) {
        console.error('Network or other error during session check or delay:', error);
        sessionStorage.removeItem('currentUser');
        initLoginPage(appContainer);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    checkUserSessionAndInitialize();

    const logoutButton = document.getElementById('logout-button');
    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            logoutUser(appContainer);
        });
    } else {
        console.error("Logout button not found in the DOM on initial load.");
    }
    const homeButton = document.getElementById('home-button');
    if (homeButton) {
        homeButton.addEventListener('click', () => {
            initHomePage(appContainer);
        });
    } else {
        console.error("Home button not found in the DOM on initial load.");
    }
    const songButton = document.getElementById('songs-button');
    if (songButton) {
        songButton.addEventListener('click', () => {
            initSongPage(appContainer);
        });
    } else {
        console.error("Song button not found in the DOM on initial load.");
    }
});
