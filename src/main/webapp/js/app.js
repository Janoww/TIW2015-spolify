import { initRouter, navigate } from './router.js';
import { initHomePage, initSongPage } from './handlers/homeHandler.js';
import { initLoginPage, logoutUser, initSignupPage } from './handlers/loginHandler.js';
import { initPlaylistPage } from './handlers/playlistHandler.js';
import { checkAuthStatus } from './apiService.js';

const appContainer = document.getElementById('app');

const publicRoutes = ['login', 'signup'];

const routeDefinitions = {
    'home': initHomePage,
    'login': initLoginPage,
    'signup': initSignupPage,
    'songs': initSongPage,
    'playlist-:idplaylist': initPlaylistPage
    // Add other routes here as needed
};

async function checkUserSessionAndInitialize() {
    if (!appContainer) {
        console.error('App container not found!');
        return false;
    }

    try {
        const userData = await checkAuthStatus();
        console.log('Active session found for user:', userData.username);
        sessionStorage.setItem('currentUser', JSON.stringify(userData));

        // User is authenticated.
        const currentHash = location.hash;
        if (currentHash === '#login' || currentHash === '') {
            navigate('home');
        }
        return true;

    } catch (error) {
        sessionStorage.removeItem('currentUser');

        // User is not authenticated.
        const currentHash = location.hash;
        let routeKeyFromHash = currentHash.substring(1);

        if (currentHash === '' || currentHash === '#') {
            routeKeyFromHash = 'home';
        }

        if (!publicRoutes.includes(routeKeyFromHash)) {
            console.log(`App.js: Initial load on protected route "${routeKeyFromHash}" without session. Redirecting to login.`);
            navigate('login');
        }

        // Log errors
        if (error.status === 401) {
            // This is expected if not logged in, so console.log might be too noisy if not debugging.
            // console.log('No active session found or user not authenticated (401). Message:', error.message);
        } else if (error.isNetworkError) {
            console.error('Network error during session check:', error.message);
        } else {
            console.error(`Error checking session: Status ${error.status}, Message: ${error.message}`, error.details || '');
        }
        return false;
    }
}

document.addEventListener('DOMContentLoaded', async () => { // Make outer listener async
    if (!appContainer) {
        console.error('App container not found! Cannot initialize router.');
        return;
    }

    // Initialize the router. This will call handleRouteChange once for the initial hash.
    initRouter(appContainer, routeDefinitions, publicRoutes);

    await checkUserSessionAndInitialize();

    const logoutButton = document.getElementById('logout-button');
    if (logoutButton) {
        logoutButton.addEventListener('click', async () => {
            await logoutUser(appContainer);
        });
    } else {
        console.error("Logout button not found in the DOM on initial load.");
    }

    const homeButton = document.getElementById('home-button');
    if (homeButton) {
        homeButton.addEventListener('click', () => {
            navigate('home');
        });
    } else {
        console.error("Home button not found in the DOM on initial load.");
    }

    const songButton = document.getElementById('songs-button');
    if (songButton) {
        songButton.addEventListener('click', () => {
            navigate('songs');
        });
    } else {
        console.error("Song button not found in the DOM on initial load.");
    }
});
