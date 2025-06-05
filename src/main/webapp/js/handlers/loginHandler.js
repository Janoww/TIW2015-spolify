import { renderLoginView, renderSignupView } from "../views/loginView.js";
import { login as apiLogin, logout as apiLogout, signup as apiSignup } from '../apiService.js';
import { navigate } from '../router.js';
import { validateForm } from '../utils/formUtils.js';
import { stopPlayback } from './playerHandler.js';

function displayLogin(appContainer) {
    renderLoginView(appContainer);

    const loginForm = document.getElementById("loginForm");

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fieldIds = ['username', 'password'];
            if (validateForm(loginForm, fieldIds)) {
                const username = event.target.username.value;
                const password = event.target.password.value;

                try {
                    const userData = await apiLogin({username, password});
                    console.log('Login successful:', userData);
                    sessionStorage.setItem('currentUser', JSON.stringify(userData));
                    navigate('home');
                } catch (error) {
                    console.error(`Login failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    const generalErrorElement = document.getElementById('login-general-error');
                    if (generalErrorElement) {
                        generalErrorElement.textContent = error.message || 'An unexpected error occurred. Please try again.';
                    } else {
                        const sanitizedErrorMessage = DOMPurify.sanitize(error.message || 'An unexpected error occurred during login. Please try again.', {ALLOWED_TAGS: []});
                        alert(sanitizedErrorMessage);
                    }
                }
            } else {
                console.log('Login form has errors.');
            }
        });
    }
}

function displaySignup(appContainer) {
    renderSignupView(appContainer);

    const signupForm = document.getElementById("signupForm");

    if (signupForm) {
        signupForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fieldIds = ['signupUsername', 'name', 'surname', 'signupPassword'];
            if (validateForm(signupForm, fieldIds)) {
                const username = event.target.signupUsername.value;
                const name = event.target.name.value;
                const surname = event.target.surname.value;
                const password = event.target.signupPassword.value;

                try {
                    const newUserData = await apiSignup({username, name, surname, password});
                    console.log('Signup successful:', newUserData);
                    const sanitizedUsername = DOMPurify.sanitize(newUserData.username, {ALLOWED_TAGS: []});
                    alert('Signup successful! User: ' + sanitizedUsername + '. Please log in.');
                    displayLogin(appContainer);
                } catch (error) {
                    console.error(`Signup failed: Status ${error.status}, Message: ${error.message}`, error.details || '');
                    const generalErrorElement = document.getElementById('signup-general-error');
                    if (generalErrorElement) {
                        generalErrorElement.textContent = error.message || 'An unexpected error occurred. Please try again.';
                    } else {
                        const sanitizedErrorMessage = DOMPurify.sanitize(error.message || 'An unexpected error occurred during signup. Please try again.', {ALLOWED_TAGS: []});
                        alert(sanitizedErrorMessage);
                    }
                }
            } else {
                console.log('Signup form has errors.');
            }
        });
    }
}

/**
 * Initializes the login page.
 * Hides the navbar and renders the login view within the given application container.
 * @param {HTMLElement} appContainer - The main application container element.
 */
export function initLoginPage(appContainer) {
    console.log("Initializing login page via router...")
    displayLogin(appContainer);
}

/**
 * Initializes the signup page.
 * Renders the signup view within the given application container.
 * @param {HTMLElement} appContainer - The main application container element.
 */
export function initSignupPage(appContainer) {
    console.log("Initializing signup page via router...");
    displaySignup(appContainer);
}

/**
 * Logs out the current user.
 * Sends a POST request to the logout API endpoint, clears the 'currentUser'
 * from session storage, and then re-initializes the login page.
 * @returns {Promise<void>} A promise that resolves when the logout process is complete
 *                          and the login page is re-initialized.
 */
export async function logoutUser() {
    console.log("Attempting to logout user...");
    stopPlayback();
    try {
        const result = await apiLogout();
        if (typeof result === 'object' && result.message) {
            console.log('Logout successful:', result.message);
        } else {
            console.log('Logout successful (non-JSON or unexpected response):', result);
        }
    } catch (error) {
        console.warn(`Logout request failed on server: Status ${error.status}, Message: ${error.message}`, error.details || '');
    } finally {
        sessionStorage.removeItem('currentUser');
        console.log('Client-side user session cleared.');
        navigate('login');
    }
}
