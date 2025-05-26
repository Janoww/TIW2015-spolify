import {createFormField} from '../utils/formUtils.js';

// Helper function to create a title container
function createTitleContainer(titleText) {
    const h1 = document.createElement('h1');
    h1.textContent = titleText;
    return h1;
}

// Helper function to create a link block (div > p > text + a)
function createLinkBlock(baseText, linkId, linkTextContent, linkHref = '#') {
    const linkContainerDiv = document.createElement('div');
    linkContainerDiv.className = 'link-container';

    const pElement = document.createElement('p');
    pElement.appendChild(document.createTextNode(baseText));

    const linkElement = document.createElement('a');
    linkElement.href = linkHref;
    linkElement.id = linkId;
    linkElement.textContent = linkTextContent;

    pElement.appendChild(linkElement);
    linkContainerDiv.appendChild(pElement);

    return linkContainerDiv;
}

/**
 * Renders the login view within the provided application container.
 * Clears the container, sets its max-width, and appends the login form
 * and a link to the signup page.
 * @param {HTMLElement} appContainer - The DOM element where the login view will be rendered.
 */
export function renderLoginView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '60vw';


    const loginSection = document.createElement('section');
    loginSection.id = 'login-section';
    loginSection.className = 'container';

    loginSection.appendChild(createTitleContainer('LOGIN'));

    const loginGeneralErrorDiv = document.createElement('div');
    loginGeneralErrorDiv.id = 'login-general-error';
    loginGeneralErrorDiv.className = 'general-error-message';
    loginSection.appendChild(loginGeneralErrorDiv);

    const loginForm = document.createElement('form');
    loginForm.id = 'loginForm';
    loginForm.noValidate = true;

    loginForm.appendChild(createFormField('username', 'Username:', 'text', 'username', true, [], {}));
    loginForm.appendChild(createFormField('password', 'Password:', 'password', 'password', true, [], {}));

    const buttonFieldDiv = document.createElement('div');
    buttonFieldDiv.className = 'form-field';
    const loginButton = document.createElement('button');
    loginButton.type = 'submit';
    loginButton.id = 'loginButton';
    loginButton.className = 'styled-button';
    loginButton.textContent = 'Login';
    loginButton.style.fontSize = '1.2em';
    buttonFieldDiv.appendChild(loginButton);
    loginForm.appendChild(buttonFieldDiv);

    loginSection.appendChild(loginForm);

    loginSection.appendChild(createLinkBlock("Don't have an account? ", 'signupLink', 'Sign up'));

    appContainer.appendChild(loginSection);
}

/**
 * Renders the signup view within the provided application container.
 * Clears the container, sets its max-width, and appends the signup form
 * and a link to the login page.
 * @param {HTMLElement} appContainer - The DOM element where the signup view will be rendered.
 */
export function renderSignupView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '60vw';

    const signupSection = document.createElement('section');
    signupSection.id = 'signup-section';
    signupSection.className = 'container';

    signupSection.appendChild(createTitleContainer('SIGN UP'));

    const signupGeneralErrorDiv = document.createElement('div');
    signupGeneralErrorDiv.id = 'signup-general-error';
    signupGeneralErrorDiv.className = 'general-error-message';
    signupSection.appendChild(signupGeneralErrorDiv);

    const signupForm = document.createElement('form');
    signupForm.id = 'signupForm';
    signupForm.noValidate = true;

    signupForm.appendChild(createFormField('signupUsername', 'Username:', 'text', 'username', true, [], {}));
    signupForm.appendChild(createFormField('name', 'Name:', 'text', 'name', true, [], {}));
    signupForm.appendChild(createFormField('surname', 'Surname:', 'text', 'surname', true, [], {}));
    signupForm.appendChild(createFormField('signupPassword', 'Password:', 'password', 'password', true, [], {}));

    const buttonFieldDiv = document.createElement('div');
    buttonFieldDiv.className = 'form-field';
    const signupButton = document.createElement('button');
    signupButton.type = 'submit';
    signupButton.className = 'styled-button';
    signupButton.textContent = 'Sign Up';
    signupButton.style.fontSize = '1.2em';
    buttonFieldDiv.appendChild(signupButton);
    signupForm.appendChild(buttonFieldDiv);

    signupSection.appendChild(signupForm);

    signupSection.appendChild(createLinkBlock('Already have an account? ', 'loginLink', 'Login'));

    appContainer.appendChild(signupSection);
}
