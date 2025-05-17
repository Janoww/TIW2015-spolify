import { renderHomeView } from "../views/homeView.js";
import { logoutUser } from "./loginHandler.js";


export function initHomePage(appContainer) {
    renderHomeView(appContainer);
}
