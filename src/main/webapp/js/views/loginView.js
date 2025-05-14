
export function renderLoginView(appContainer) {
    appContainer.innerHTML = `
        <div class="container">
            <div class="title-container">
                <img src="images/SpolifyIcon.jpeg" alt="App Icon" class="form-icon">
                <h1>LOGIN</h1>
            </div>
            <form id="loginForm">
                <div class="form-field">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" required>
                </div>
                <div class="form-field">
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <div class="form-field">
                    <button type="submit" id="loginButton" class="styled-button">Login</button>
                </div>
            </form>
            <p>Don't have an account? <a href="#" id="signupLink">Sign up</a></p>
        </div>
    `;
}

export function renderSignupView(appContainer) {
    appContainer.innerHTML = `
        <div class="container">
            <div class="title-container">
                <img src="images/SpolifyIcon.jpeg" alt="App Icon" class="form-icon">
                <h1>SIGN UP</h1>
            </div>
            <form id="signupForm">
                <div class="form-field">
                    <label for="signupUsername">Username:</label>
                    <input type="text" id="signupUsername" name="username" required>
                </div>
                <div class="form-field">
                    <label for="name">Name:</label>
                    <input type="text" id="name" name="name" required>
                </div>
                <div class="form-field">
                    <label for="surname">Surname:</label>
                    <input type="text" id="surname" name="surname" required>
                </div>
                <div class="form-field">
                    <label for="signupPassword">Password:</label>
                    <input type="password" id="signupPassword" name="password" required>
                </div>
                <div class="form-field">
                    <button type="submit" class="styled-button">Sign Up</button>
                </div>
            </form>
            <p>Already have an account? <a href="#" id="loginLink">Login</a></p>
        </div>
    `;
}
