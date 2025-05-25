import { renderLoginView, renderSignupView } from "../views/loginView.js";
import { initHomePage } from "./homeHandler.js";

// Helper function to validate a single form field
function validateField(inputElement, errorElementId) {
    const errorElement = document.getElementById(errorElementId);
    if (!inputElement) { // Defensive check
        console.error(`Input element not found for ${errorElementId}`);
        return false;
    }
    if (inputElement.required && !inputElement.value.trim()) {
        errorElement.textContent = `${inputElement.labels[0].textContent.replace(':', '')} is required.`;
        inputElement.classList.add('input-error');
        return false;
    }
    errorElement.textContent = '';
    inputElement.classList.remove('input-error');
    return true;
}

// Helper function to validate the entire form
function validateForm(formId, fieldIds) {
    let isValid = true;
    fieldIds.forEach(fieldId => {
        const inputElement = document.getElementById(fieldId);
        if (inputElement) {
            if (!validateField(inputElement, `${fieldId}-error`)) {
                isValid = false;
            }
        } else {
            console.warn(`validateForm: Element with ID ${fieldId} not found in form ${formId}`);
        }
    });
    return isValid;
}

function displayLogin(appContainer) {
    renderLoginView(appContainer);

    const loginForm = document.getElementById("loginForm");
    const signupLink = document.getElementById("signupLink");

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fieldIds = ['username', 'password'];
            if (validateForm('loginForm', fieldIds)) {
                const username = event.target.username.value;
                const password = event.target.password.value;

                try {
                    const response = await fetch('api/v1/auth/login', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ username, password })
                    });

                    const data = await response.json();

                    if (response.ok) {
                        console.log('Login successful:', data);
                        sessionStorage.setItem('currentUser', JSON.stringify(data));
                        initHomePage(appContainer);
                    } else {
                        console.error('Login failed:', data.error || response.statusText);
                        const generalErrorElement = document.getElementById('login-general-error');
                        if (generalErrorElement) {
                            generalErrorElement.textContent = data.error || `Login failed: ${response.statusText}`;
                        } else {
                            alert(`Login failed: ${data.error || response.statusText}`);
                        }
                    }
                } catch (error) {
                    console.error('Error during login:', error);
                    const generalErrorElement = document.getElementById('login-general-error');
                    if (generalErrorElement) {
                        generalErrorElement.textContent = 'An unexpected error occurred. Please try again.';
                    } else {
                        alert('An unexpected error occurred during login. Please try again.');
                    }
                }
            } else {
                console.log('Login form has errors.');
            }
        });
    }

    if (signupLink) {
        signupLink.addEventListener("click", (event) => {
            event.preventDefault();
            displaySignup(appContainer);
        });
    }
}

function displaySignup(appContainer) {
    renderSignupView(appContainer);

    const signupForm = document.getElementById("signupForm");
    const loginLink = document.getElementById("loginLink");

    if (signupForm) {
        signupForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fieldIds = ['signupUsername', 'name', 'surname', 'signupPassword'];
            if (validateForm('signupForm', fieldIds)) {
                const username = event.target.signupUsername.value;
                const name = event.target.name.value;
                const surname = event.target.surname.value;
                const password = event.target.signupPassword.value;

                try {
                    const response = await fetch('api/v1/users', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ username, name, surname, password })
                    });

                    const data = await response.json();

                    if (response.status === 201) {
                        console.log('Signup successful:', data);
                        alert('Signup successful! User: ' + data.username + '. Please log in.');
                        displayLogin(appContainer);
                    } else {
                        console.error('Signup failed:', data.error || response.statusText);
                        const generalErrorElement = document.getElementById('signup-general-error');
                        if (generalErrorElement) {
                            generalErrorElement.textContent = data.error || `Signup failed: ${response.statusText}`;
                        } else {
                            alert(`Signup failed: ${data.error || response.statusText}`);
                        }
                    }
                } catch (error) {
                    console.error('Error during signup:', error);
                    const generalErrorElement = document.getElementById('signup-general-error');
                    if (generalErrorElement) {
                        generalErrorElement.textContent = 'An unexpected error occurred. Please try again.';
                    } else {
                        alert('An unexpected error occurred during signup. Please try again.');
                    }
                }
            } else {
                console.log('Signup form has errors.');
            }
        });
    }

    if (loginLink) {
        loginLink.addEventListener("click", (event) => {
            event.preventDefault();
            displayLogin(appContainer);
        });
    }
}

/**
 * Initializes the login page.
 * Hides the navbar and renders the login view within the given application container.
 * @param {HTMLElement} appContainer - The main application container element.
 */
export function initLoginPage(appContainer) {
    document.getElementById("navbar").style.display = "none";
    console.log("Initializing login page...")
    displayLogin(appContainer);
}

/**
 * Logs out the current user.
 * Sends a POST request to the logout API endpoint, clears the 'currentUser'
 * from session storage, and then re-initializes the login page.
 * @param {HTMLElement} appContainer - The main application container element.
 * @returns {Promise<void>} A promise that resolves when the logout process is complete
 *                          and the login page is re-initialized.
 */
export async function logoutUser(appContainer) {
    console.log("Attempting to logout user...");
    try {
        const response = await fetch('api/v1/auth/logout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (response.ok) {
            console.log('Logout successful:', data.message);
        } else {
            console.warn('Logout request failed on server:', data.error || response.statusText);
        }
    } catch (error) {
        console.error('Error during logout fetch:', error);
    } finally {
        sessionStorage.removeItem('currentUser');
        console.log('Client-side user session cleared.');
        initLoginPage(appContainer);
    }
}
