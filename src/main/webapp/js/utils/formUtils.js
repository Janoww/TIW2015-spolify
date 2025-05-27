import { createElement } from "./viewUtils.js";

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
    const formFieldDiv = createElement('div', {
        className: 'form-field'
    });

    const innerDiv = createElement('div');

    const labelEl = createElement('label', {
        textContent: labelText,
        attributes: { htmlFor: inputId }
    });
    innerDiv.appendChild(labelEl);

    let inputEl;
    const commonInputAttributes = { ...attributes, name: name };
    if (required) {
        commonInputAttributes.required = true;
    }

    if (inputType === 'select') {
        inputEl = createElement('select', { id: inputId, attributes: commonInputAttributes });
        options?.map(optConfig => {
            const optionAttrs = { value: optConfig.value };
            optConfig.disabled && (optionAttrs.disabled = true);
            optConfig.selected && (optionAttrs.selected = true);
            return createElement('option', { textContent: optConfig.text, attributes: optionAttrs });
        }).forEach(optionEl => { inputEl.appendChild(optionEl); });
    } else {
        commonInputAttributes.type = inputType;
        inputEl = createElement('input', { id: inputId, attributes: commonInputAttributes });
    }

    innerDiv.appendChild(inputEl);
    formFieldDiv.appendChild(innerDiv);

    const errorSpan = createElement('span', {
        className: 'error-message',
        id: inputId + '-error'
    });
    formFieldDiv.appendChild(errorSpan);

    return formFieldDiv;
}
