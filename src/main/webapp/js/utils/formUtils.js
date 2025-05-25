/**
 * Creates a div element containing a labeled form input field.
 *
 * @param {string} inputId - The ID of the input element.
 * @param {string} labelText - The label text to display.
 * @param {string} inputType - The type of the input element ('text', 'file', 'select', etc.).
 * @param {string} name - The name attribute of the input element.
 * @param {boolean} [required=true] - Whether the field is required.
 * @param {Array<{value: string, text: string, disabled?: boolean, selected?: boolean}>} [options=[]] - Options for a `<select>` input.
 * @param {Object} [attributes={}] - Additional HTML attributes as key-value pairs.
 * @returns {HTMLElement} A div containing the labeled input element.
 */
export function createFormField(inputId, labelText, inputType, name, required = true, options = [], attributes = {}) {
    const formFieldDiv = document.createElement('div');
    formFieldDiv.className = 'form-field';

    const innerDiv = document.createElement('div');

    const labelEl = document.createElement('label');
    labelEl.htmlFor = inputId;
    labelEl.textContent = labelText;
    innerDiv.appendChild(labelEl);

    let inputEl;
    if (inputType === 'select') {
        inputEl = document.createElement('select');
        if (options) {
            options.forEach(optConfig => {
                const optionEl = document.createElement('option');
                optionEl.value = optConfig.value;
                optionEl.textContent = optConfig.text;
                if (optConfig.disabled) optionEl.disabled = true;
                if (optConfig.selected) optionEl.selected = true;
                inputEl.appendChild(optionEl);
            });
        }
    } else {
        inputEl = document.createElement('input');
        inputEl.type = inputType;
    }

    inputEl.id = inputId;
    inputEl.name = name;
    if (required) {
        inputEl.required = true;
    }

    if (attributes) {
        for (const attrKey in attributes) {
            inputEl.setAttribute(attrKey, attributes[attrKey]);
        }
    }
    innerDiv.appendChild(inputEl);

    formFieldDiv.appendChild(innerDiv);

    const errorSpan = document.createElement('span');
    errorSpan.className = 'error-message';
    errorSpan.id = inputId + '-error';
    formFieldDiv.appendChild(errorSpan);

    return formFieldDiv;
}
