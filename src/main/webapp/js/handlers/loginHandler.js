import { renderLoginView, renderSignupView } from "../views/loginView.js";

function displayLogin(appContainer) {
    renderLoginView(appContainer);

    const loginForm = document.getElementById("loginForm");
    const signupLink = document.getElementById("signupLink");

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const username = event.target.username.value;
            const password = event.target.password.value;

            console.log("Login attempt: ", { username, password });
            // TODO: Make the login logic to api.
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
    const loginLink = document.getElementById("loginLink"); // Link on signup page to go back to login

    if (signupForm) {
        signupForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const username = event.target.username.value;
            const name = event.target.name.value;
            const surname = event.target.surname.value;
            const password = event.target.password.value;

            console.log("Signup attempt: ", { username, name, surname, password });
            // TODO: Make the signup logic to api.
        });
    }

    if (loginLink) {
        loginLink.addEventListener("click", (event) => {
            event.preventDefault();
            displayLogin(appContainer);
        });
    }
}

export function initLoginPage(appContainer) {
    displayLogin(appContainer); // Start by displaying the login page
}
