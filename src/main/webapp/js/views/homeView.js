
export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '80%';

    const homeSection = document.createElement('section');
    homeSection.className = 'section';
    homeSection.id = 'home-section';

    document.getElementById('navbar').style.display = 'inline-block';

    const h1 = document.createElement('h1');
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));
    if (currentUser && currentUser.name) {
        h1.textContent = `Welcome to Spolify, ${currentUser.name}!`;
    } else {
        h1.textContent = 'Welcome to Spolify!';
    }
    homeSection.appendChild(h1);

    appContainer.appendChild(homeSection);
}
