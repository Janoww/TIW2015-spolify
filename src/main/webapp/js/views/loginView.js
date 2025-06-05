import { createFormField } from '../utils/formUtils.js';
import { createElement, createHeaderContainer } from '../utils/viewUtils.js';

// Helper function to create a link block (div > p > text + a)
function createLinkBlock(baseText, linkId, linkTextContent, linkHref = '#') {
    const linkContainerDiv = createElement('div', {className: 'link-container'});

    const pElement = createElement('p');
    pElement.appendChild(document.createTextNode(baseText));

    const linkElement = createElement('a', {
        id: linkId,
        textContent: linkTextContent,
        attributes: {href: linkHref}
    });

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

    const loginSection = createElement('section', {id: 'login-section', className: 'container'});
    loginSection.appendChild(createHeaderContainer('LOGIN', 'h1'));

    const loginGeneralErrorDiv = createElement('div', {id: 'login-general-error', className: 'general-error-message'});
    loginSection.appendChild(loginGeneralErrorDiv);

    const loginForm = createElement('form', {id: 'loginForm', attributes: {noValidate: true}});
    loginForm.appendChild(createFormField('username', 'Username:', 'text', 'username', true, [], {}));
    loginForm.appendChild(createFormField('password', 'Password:', 'password', 'password', true, [], {}));

    const buttonFieldDiv = createElement('div', {className: 'form-field'});
    const loginButton = createElement('button', {
        id: 'loginButton',
        textContent: 'Login',
        className: 'styled-button',
        attributes: {type: 'submit', style: 'font-size: 1.2em;'}
    });
    buttonFieldDiv.appendChild(loginButton);
    loginForm.appendChild(buttonFieldDiv);

    loginSection.appendChild(loginForm);

    loginSection.appendChild(createLinkBlock("Don't have an account? ", 'signupLink', 'Sign up', '#signup'));

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

    const signupSection = createElement('section', {id: 'signup-section', className: 'container'});
    signupSection.appendChild(createHeaderContainer('SIGN UP', 'h1'));

    const signupGeneralErrorDiv = createElement('div', {
        id: 'signup-general-error',
        className: 'general-error-message'
    });
    signupSection.appendChild(signupGeneralErrorDiv);

    const signupForm = createElement('form', {id: 'signupForm', attributes: {noValidate: true}});
    signupForm.appendChild(createFormField('signupUsername', 'Username:', 'text', 'username', true, [], {}));
    signupForm.appendChild(createFormField('name', 'Name:', 'text', 'name', true, [], {}));
    signupForm.appendChild(createFormField('surname', 'Surname:', 'text', 'surname', true, [], {}));
    signupForm.appendChild(createFormField('signupPassword', 'Password:', 'password', 'password', true, [], {}));

    const buttonFieldDiv = createElement('div', {className: 'form-field'});
    const signupButton = createElement('button', {
        textContent: 'Sign Up',
        className: 'styled-button',
        attributes: {type: 'submit', style: 'font-size: 1.2em;'}
    });
    buttonFieldDiv.appendChild(signupButton);
    signupForm.appendChild(buttonFieldDiv);

    signupSection.appendChild(signupForm);

    signupSection.appendChild(createLinkBlock('Already have an account? ', 'loginLink', 'Login', '#login'));

    appContainer.appendChild(signupSection);
}
