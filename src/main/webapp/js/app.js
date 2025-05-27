import { initHomePage, initSongPage } from './handlers/homeHandler.js';
import { initLoginPage, logoutUser } from './handlers/loginHandler.js';
import { checkAuthStatus } from './apiService.js';

const appContainer = document.getElementById('app');

async function checkUserSessionAndInitialize() {
    if (!appContainer) {
        console.error('App container not found!');
        return;
    }

    const delayPromise = new Promise(resolve => setTimeout(resolve, 300));

    try {
        const [, userData] = await Promise.all([delayPromise, checkAuthStatus()]);
        console.log('Active session found for user:', userData.username);
        sessionStorage.setItem('currentUser', JSON.stringify(userData));
        await initHomePage(appContainer);

    } catch (error) {
        if (error.status === 401) {
            console.log('No active session found or user not authenticated (401). Message:', error.message);
        } else if (error.isNetworkError) {
            console.error('Network error during session check:', error.message);
        } else {
            console.error(`Error checking session: Status ${error.status}, Message: ${error.message}`, error.details || '');
        }
        sessionStorage.removeItem('currentUser');
        initLoginPage(appContainer);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    checkUserSessionAndInitialize().then(() => {
    });

    const logoutButton = document.getElementById('logout-button');
    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            logoutUser(appContainer).then(() => {
            });
        });
    } else {
        console.error("Logout button not found in the DOM on initial load.");
    }
    const homeButton = document.getElementById('home-button');
    if (homeButton) {
        homeButton.addEventListener('click', () => {
            initHomePage(appContainer).then(() => {
            });
        });
    } else {
        console.error("Home button not found in the DOM on initial load.");
    }
    const songButton = document.getElementById('songs-button');
    if (songButton) {
        songButton.addEventListener('click', () => {
            initSongPage(appContainer).then(() => {
            });
        });
    } else {
        console.error("Song button not found in the DOM on initial load.");
    }
});
