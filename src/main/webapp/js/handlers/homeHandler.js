import { renderHomeView } from "../views/homeView.js";
import { logoutUser } from "./loginHandler.js";


export function initHomePage(appContainer) {
    const navbar = document.getElementById('navbar');
    if (navbar) {
        navbar.style.display = 'inline-block';
    } else {
        console.error("Navbar element not found in initHomePage.");
    }
    renderHomeView(appContainer);
}
