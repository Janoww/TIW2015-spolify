

import { logoutUser } from '../handlers/loginHandler.js';

export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';

    const containerDiv = document.createElement('div');
    containerDiv.className = 'container';

    const h1 = document.createElement('h1');
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));
    if (currentUser && currentUser.name) {
        h1.textContent = `Welcome to Spolify, ${currentUser.name}!`;
    } else {
        h1.textContent = 'Welcome to Spolify!';
    }
    containerDiv.appendChild(h1);

    // Add Logout Button
    const logoutButton = document.createElement('button');
    logoutButton.id = 'logoutButton';
    logoutButton.textContent = 'Logout';
    logoutButton.className = 'styled-button';
    logoutButton.addEventListener('click', () => {
        logoutUser(appContainer);
    });
    containerDiv.appendChild(logoutButton);


    appContainer.appendChild(containerDiv);
}