import { createElement } from "./utils/viewUtils.js";

let routes = {};
let appContainerRef = null;
let publicRouteKeys = [];
let loaderElement = null;

/**
 * Shows the loader in the app container.
 */
function showLoader() {
    if (!appContainerRef) return;
    if (!loaderElement) {
        loaderElement = createElement('div', {
            className: 'loader'
        });
    }
    appContainerRef.innerHTML = '';
    appContainerRef.appendChild(loaderElement);
}

/**
 * Hides the loader.
 */
function hideLoader() {
    if (loaderElement && loaderElement.parentNode === appContainerRef) {
        appContainerRef.removeChild(loaderElement);
    }
    // The actual content rendering will replace the loader
}

/**
 * Initializes the router.
 * @param {HTMLElement} containerElement - The main DOM element where views will be rendered.
 * @param {Object} routeDefinitions - An object mapping route keys (e.g., 'home', 'playlist-:idplaylist') to handler functions.
 * @param {string[]} appPublicRoutes - An array of route keys that are considered public (e.g., ['login', 'signup']).
 */
function initRouter(containerElement, routeDefinitions, appPublicRoutes) {
    appContainerRef = containerElement;
    routes = routeDefinitions;
    publicRouteKeys = appPublicRoutes || [];
    window.addEventListener('hashchange', handleRouteChange);
    handleRouteChange();
}

/**
 * Navigates to the given route.
 * @param {string} routeKey - The route key including any dynamic parts (e.g., 'home', 'login', 'playlist-123').
 */
function navigate(routeKey) {
    if (routeKey.startsWith('#')) {
        location.hash = routeKey;
    } else {
        location.hash = '#' + routeKey;
    }
}

/**
 * Handles changes to the URL hash.
 */
async function handleRouteChange() {
    const currentHash = location.hash;
    let processedPath;

    if (currentHash === '' || currentHash === '#') {
        processedPath = 'home';
        if (location.hash !== '#home') {
            navigate('#home');
            return;
        }
    } else {
        processedPath = currentHash.substring(1);
    }

    if (!appContainerRef) {
        console.error("Router not initialized: App container reference is missing.");
        return;
    }
    showLoader();

    await new Promise(resolve => setTimeout(resolve, 100));

    let handlerFound = false;
    let params = {};

    // Iterate over defined routes to find a match
    for (const routePattern in routes) {
        const paramNames = [];

        const regexPatternText = '^' + routePattern.replace(/:(\w+)/g, (_, paramName) => {
            paramNames.push(paramName);
            return '([^/-]+)';
        }) + '$';
        const match = RegExp(regexPatternText).exec(processedPath);

        if (match) {
            paramNames.forEach((name, index) => {
                params[name] = decodeURIComponent(match[index + 1]);
            });

            const handler = routes[routePattern];
            await executeHandler(handler, routePattern, params);
            handlerFound = true;
            break;
        }
    }

    if (!handlerFound) {
        hideLoader();
        console.error(`No handler found for path: #${processedPath}. Displaying 404.`);
        appContainerRef.innerHTML = '<h1>404 - Page Not Found</h1>';
    }
}

/**
 * Executes the route handler and manages global UI elements like the navbar.
 * @param {Function} handler - The handler function for the route.
 * @param {string} matchedRoutePattern - The route pattern key from definitions (e.g., 'login', 'playlist-:idplaylist').
 * @param {Object} params - Extracted URL parameters.
 */
async function executeHandler(handler, matchedRoutePattern, params) {

    if (!publicRouteKeys.includes(matchedRoutePattern)) {
        const currentUser = sessionStorage.getItem('currentUser');
        if (!currentUser) {
            console.log(`Router: Protected route "${matchedRoutePattern}" access attempt without session. Redirecting to login.`);
            navigate('login');
            return;
        }
    }

    const navbar = document.getElementById('navbar');
    if (navbar) {
        if (publicRouteKeys.includes(matchedRoutePattern)) {
            navbar.style.display = 'none';
        } else {
            navbar.style.display = 'inline-block';
        }
    }

    try {
        await handler(appContainerRef, params);
    } catch (error) {
        console.error(`Error executing handler for route pattern ${matchedRoutePattern}:`, error);
        appContainerRef.innerHTML = '<h1>Error loading page</h1><p>Sorry, an error occurred. Please try again.</p>';
    } finally {
        hideLoader();
    }
}

export { initRouter, navigate };
