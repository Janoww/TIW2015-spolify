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

// --- Validation Helper Functions ---

function getFieldLabel(inputElement) {
    if (inputElement.labels && inputElement.labels.length > 0) {
        return inputElement.labels[0].textContent.replace(':', '');
    }
    return 'Field';
}

function validateRadioGroup(inputElement, form, config, label) {
    if (typeof config === 'object' && config.type === 'radio' && config.name) {
        const radioGroup = form.querySelectorAll(`input[type="radio"][name="${config.name}"]`);
        let radioSelected = false;
        radioGroup.forEach(radio => {
            if (radio.checked) radioSelected = true;
        });
        if (inputElement.required && !radioSelected) {
            return `${label} selection is required.`;
        }
    }
    return '';
}

function validateCheckbox(inputElement, label) {
    if (inputElement.type === 'checkbox' && inputElement.required && !inputElement.checked) {
        return `You must accept the ${label.toLowerCase()}.`;
    }
    return '';
}

function validateFile(inputElement, label) {
    if (inputElement.type === 'file' && inputElement.required && inputElement.files.length === 0) {
        return `Please select a file for ${label.toLowerCase()}.`;
    }
    return '';
}

function validateSelect(inputElement, label) {
    if (inputElement.tagName.toLowerCase() === 'select' && inputElement.required && !inputElement.value) {
        return `Please select an option for ${label.toLowerCase()}.`;
    }
    return '';
}

function validateTextBasedInput(inputElement, label) {
    const value = inputElement.value.trim();
    const originalValue = inputElement.value; // For pattern matching, use non-trimmed

    if (inputElement.required && !value) {
        return `${label} is required.`;
    }

    // Only apply further validation if there's a value, or if it's not required but has a value that needs checking
    if (originalValue) { // Check originalValue because a field might not be required but still have pattern/type validation
        if (inputElement.type === 'number') {
            const num = Number(originalValue);
            const min = inputElement.min ? Number(inputElement.min) : null;
            const max = inputElement.max ? Number(inputElement.max) : null;
            if ((min !== null && num < min) || (max !== null && num > max)) {
                let rangeMessage;
                if (min !== null && max !== null) {
                    rangeMessage = `between ${min} and ${max}`;
                } else if (min !== null) {
                    rangeMessage = `at least ${min}`;
                } else {
                    rangeMessage = `no more than ${max}`;
                }
                return `${label} must be ${rangeMessage}.`;
            }
        }
        if (inputElement.minLength > 0 && originalValue.length < inputElement.minLength) {
            return `${label} must be at least ${inputElement.minLength} characters long.`;
        }
        if (inputElement.maxLength > 0 && originalValue.length > inputElement.maxLength) {
            return `${label} must be no more than ${inputElement.maxLength} characters long.`;
        }
        if (inputElement.pattern) {
            const regex = new RegExp(inputElement.pattern);
            if (!regex.test(originalValue)) {
                return inputElement.title || `Invalid format for ${label.toLowerCase()}.`;
            }
        }
    }
    return '';
}


/**
 * Validates a form based on a list of field configurations.
 *
 * @param {string|HTMLFormElement} formElementOrId - The form element or its ID.
 * @param {Array<string|{id: string, name?: string, type?: 'radio'}>} fieldConfigs - An array of field IDs or configuration objects.
 *        For radio buttons, provide an object like { id: 'anyRadioInGroupId', name: 'radioGroupName', type: 'radio' }.
 * @returns {boolean} True if all specified fields are valid, false otherwise.
 */
export function validateForm(formElementOrId, fieldConfigs) {
    const form = typeof formElementOrId === 'string' ? document.getElementById(formElementOrId) : formElementOrId;

    if (!form) {
        console.error(`Form with id "${formElementOrId}" not found.`);
        return false;
    }

    let isValid = true;

    fieldConfigs.forEach(config => {
        const fieldId = typeof config === 'string' ? config : config.id;
        const inputElement = form.querySelector(`#${fieldId}`);

        const errorElement = form.querySelector(`#${fieldId}-error`) || document.getElementById(`${fieldId}-error`);


        if (!inputElement) {
            console.warn(`Input element not found for ID: ${fieldId} in form ${form.id}`);
            return;
        }
        if (!errorElement) {
            console.warn(`Error element not found for ID: ${fieldId}-error for field ${fieldId} in form ${form.id}`);
        }

        if (errorElement) errorElement.textContent = '';
        inputElement.classList.remove('input-error');
        let errorMessage = '';
        const label = getFieldLabel(inputElement);

        // Determine which validation function to call
        switch (true) {
            case (typeof config === 'object' && config.type === 'radio'):
                errorMessage = validateRadioGroup(inputElement, form, config, label);
                break;
            case (inputElement.type === 'checkbox'):
                errorMessage = validateCheckbox(inputElement, label);
                break;
            case (inputElement.type === 'file'):
                errorMessage = validateFile(inputElement, label);
                break;
            case (inputElement.tagName.toLowerCase() === 'select'):
                errorMessage = validateSelect(inputElement, label);
                break;
            default:
                errorMessage = validateTextBasedInput(inputElement, label);
                break;
        }

        if (errorMessage) {
            if (errorElement) errorElement.textContent = errorMessage;
            inputElement.classList.add('input-error');
            isValid = false;
        }
    });

    return isValid;
}
