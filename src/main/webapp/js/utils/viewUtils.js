/**
 * Creates an HTML element with specified properties.
 * @param {string} tagName - The HTML tag name (e.g., 'div', 'button', 'img').
 * @param {object} [options={}] - An object containing properties to set on the element.
 * @param {string} [options.id] - The ID of the element.
 * @param {string} [options.className] - The class name(s) for the element.
 * @param {string} [options.textContent] - The text content of the element.
 * @param {object} [options.dataset] - An object for data-* attributes.
 * @param {object} [options.attributes] - An object for other HTML attributes (e.g., { type: 'text', value: 'hello' }).
 * @returns {HTMLElement} The created HTML element.
 */
export function createElement(tagName, options = {}) {
    const element = document.createElement(tagName);

    options.id && (element.id = options.id);
    options.className && (element.className = options.className);
    options.textContent && (element.textContent = options.textContent);

    if (options.dataset) {
        Object.entries(options.dataset).forEach(([key, value]) => {
            element.dataset[key] = value;
        });
    }

    if (options.attributes) {
        Object.entries(options.attributes).forEach(([key, value]) => {
            if (key === 'htmlFor' && tagName.toLowerCase() === 'label') {
                element.htmlFor = value;
            } else {
                element.setAttribute(key, value);
            }
        });
    }

    return element;
}

/**
 * Helper function to create a header element (h1, h2, etc.).
 * @param {string} titleText - The text content for the header.
 * @param {string} size - The header size (e.g., 'h1', 'h2', 'h3').
 * @returns {HTMLElement} The created header element.
 */
export function createHeaderContainer(titleText, size) {
    return createElement(size, { textContent: titleText });
}

/**
 * Helper function to create a paragraph element.
 * @param {string} text - The text content for the paragraph.
 * @param {string} [id] - Optional ID for the paragraph element.
 * @param {string} [className] - Optional CSS class name for the paragraph element.
 * @returns {HTMLParagraphElement} The created paragraph element.
 */
export function createParagraphElement(text, id, className) {
    const opts = { textContent: text };
    id && (opts.id = id);
    className && (opts.className = className);
    return createElement('p', opts);
}
