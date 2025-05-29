import {initRouter, navigate} from './router.js';
import {initHomePage} from './handlers/homeHandler.js';
import {initLoginPage, initSignupPage, logoutUser} from './handlers/loginHandler.js';
import {initPlaylistPage} from './handlers/playlistHandler.js';
import {checkAuthStatus} from './apiService.js';
import {initSongPage} from './handlers/songsHandler.js';
import {createElement} from './utils/viewUtils.js';

const appContainer = document.getElementById('app');

const publicRoutes = ['login', 'signup'];

const routeDefinitions = {
    'home': initHomePage,
    'login': initLoginPage,
    'signup': initSignupPage,
    'songs': initSongPage,
    'playlist-:idplaylist': initPlaylistPage
};

/**
 *
 * @param {HTMLElement} navbar
 */
function addNavLinks(navbar) {

    const availableLinks = ['Home', 'Songs'];

    availableLinks.map(linkText => {
        return createElement('a', {
            className: 'styled-button',
            textContent: linkText,
            attributes: {href: `#${linkText.toLowerCase()}`}
        })
    }).forEach(element => {
        navbar.appendChild(element)
    });
}

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
        if (error.isNetworkError) {
            console.error('Network error during session check:', error.message);
        } else if (error.status !== 401) {
            console.error(`Error checking session: Status ${error.status}, Message: ${error.message}`, error.details || '');
        }
        return false;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    if (!appContainer) {
        console.error('App container not found! Cannot initialize router.');
        return;
    }

    // Initialize the router. This will call handleRouteChange once for the initial hash.
    initRouter(appContainer, routeDefinitions, publicRoutes);

    await checkUserSessionAndInitialize();

    const navbar = document.getElementById('navbar');
    navbar.innerHTML = '';

    addNavLinks(navbar);

    const logoutButton = createElement('button', {
        id: 'logout-button',
        className: 'styled-button',
        textContent: 'Logout'
    });

    if (logoutButton) {
        logoutButton.addEventListener('click', async () => {
            await logoutUser();
        });
    } else {
        console.error("Logout button not created");
    }

    navbar.appendChild(logoutButton);
});
