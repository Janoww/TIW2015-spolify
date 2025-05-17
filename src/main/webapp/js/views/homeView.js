import { logoutUser } from '../handlers/loginHandler.js';

export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';

    // Add Logout Button
    const logoutButton = document.createElement('button');
    logoutButton.id = 'logoutButton';
    logoutButton.textContent = 'Logout';
    logoutButton.className = 'styled-button logout-button-top-left';
    logoutButton.addEventListener('click', () => {
        logoutUser(appContainer);
    });
    appContainer.appendChild(logoutButton);

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

    // Logout button is now created and appended at the beginning of this function

    appContainer.appendChild(containerDiv);
}
