// Helper function to create a form field (div, label, input)
function createFormField(labelText, inputType, inputId, inputName, required = true) {
    const fieldDiv = document.createElement('div');
    fieldDiv.className = 'form-field';

    const inputDiv = document.createElement('div');

    const label = document.createElement('label');
    label.setAttribute('for', inputId);
    label.textContent = labelText;

    const input = document.createElement('input');
    input.type = inputType;
    input.id = inputId;
    input.name = inputName;
    if (required) {
        input.required = true;
    }

    inputDiv.appendChild(label);
    inputDiv.appendChild(input);
    fieldDiv.appendChild(inputDiv)
    const errorSpan = document.createElement('span');
    errorSpan.className = 'error-message';
    errorSpan.id = `${inputId}-error`;
    fieldDiv.appendChild(errorSpan);

    return fieldDiv;
}

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

    loginForm.appendChild(createFormField('Username:', 'text', 'username', 'username'));
    loginForm.appendChild(createFormField('Password:', 'password', 'password', 'password'));

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

    signupForm.appendChild(createFormField('Username:', 'text', 'signupUsername', 'username'));
    signupForm.appendChild(createFormField('Name:', 'text', 'name', 'name'));
    signupForm.appendChild(createFormField('Surname:', 'text', 'surname', 'surname'));
    signupForm.appendChild(createFormField('Password:', 'password', 'signupPassword', 'password'));

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
