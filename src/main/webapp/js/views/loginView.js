// Helper function to create a form field (div, label, input)
function createFormField(labelText, inputType, inputId, inputName, required = true) {
    const fieldDiv = document.createElement('div');
    fieldDiv.className = 'form-field';

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

    fieldDiv.appendChild(label);
    fieldDiv.appendChild(input);
    const errorSpan = document.createElement('span');
    errorSpan.className = 'error-message';
    errorSpan.id = `${inputId}-error`;
    fieldDiv.appendChild(errorSpan);

    return fieldDiv;
}

// Helper function to create a title container
function createTitleContainer(titleText) {
    const titleContainerDiv = document.createElement('div');
    titleContainerDiv.className = 'title-container';

    const img = document.createElement('img');
    img.src = 'images/SpolifyIcon.png';
    img.alt = 'App Icon';
    img.className = 'form-icon';

    const h1 = document.createElement('h1');
    h1.textContent = titleText;

    titleContainerDiv.appendChild(img);
    titleContainerDiv.appendChild(h1);
    return titleContainerDiv;
}

export function renderLoginView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '800px';


    const loginSection = document.createElement('section');
    loginSection.id = 'login-section';
    loginSection.className = 'container';

    loginSection.appendChild(createTitleContainer('LOGIN'));

    const loginGeneralErrorDiv = document.createElement('div');
    loginGeneralErrorDiv.id = 'login-general-error';
    loginGeneralErrorDiv.className = 'error-message general-error-message';
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
    buttonFieldDiv.appendChild(loginButton);
    loginForm.appendChild(buttonFieldDiv);

    loginSection.appendChild(loginForm);

    const pElement = document.createElement('p');
    pElement.textContent = "Don't have an account? ";
    const signupLink = document.createElement('a');
    signupLink.href = '#';
    signupLink.id = 'signupLink';
    signupLink.textContent = 'Sign up';
    pElement.appendChild(signupLink);
    loginSection.appendChild(pElement);

    appContainer.appendChild(loginSection);
}

export function renderSignupView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '800px';

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
    buttonFieldDiv.appendChild(signupButton);
    signupForm.appendChild(buttonFieldDiv);

    signupSection.appendChild(signupForm);

    const pElement = document.createElement('p');
    pElement.textContent = 'Already have an account? ';
    const loginLink = document.createElement('a');
    loginLink.href = '#';
    loginLink.id = 'loginLink';
    loginLink.textContent = 'Login';
    pElement.appendChild(loginLink);
    signupSection.appendChild(pElement);

    appContainer.appendChild(signupSection);
}
