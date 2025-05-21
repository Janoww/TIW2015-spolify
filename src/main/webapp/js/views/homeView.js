// Helper function to create form fields
function createFormField({ id, labelText, inputType, name, required, options, attributes }) {
    const formFieldDiv = document.createElement('div');
    formFieldDiv.className = 'form-field';

    const innerDiv = document.createElement('div');

    const labelEl = document.createElement('label');
    labelEl.htmlFor = id;
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

    inputEl.id = id;
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
    errorSpan.id = id + '-error';
    formFieldDiv.appendChild(errorSpan);

    return formFieldDiv;
}

export function renderHomeView(appContainer) {
    appContainer.innerHTML = '';
    appContainer.style.maxWidth = '100%';

}
