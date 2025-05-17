import { renderHomeView } from "../views/homeView.js";
import { logoutUser } from "./loginHandler.js";


export function initHomePage(appContainer) {
    renderHomeView(appContainer);
    const logoutButton = document.getElementById('logoutButton');
    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            logoutUser(appContainer)
        });
    } else {
        console.error("Logout button not found");
    }
}