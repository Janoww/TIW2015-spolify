import { initLoginPage } from './handlers/loginHandler.js';
import { renderHomeView } from './views/homeView.js';

const appContainer = document.getElementById('app');

async function checkUserSessionAndInitialize() {
    if (!appContainer) {
        console.error('App container not found!');
        return;
    }

    const delayPromise = new Promise(resolve => setTimeout(resolve, 1000));
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
            renderHomeView(appContainer);
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

// Entry point for the application
document.addEventListener('DOMContentLoaded', () => {
    checkUserSessionAndInitialize();
});
