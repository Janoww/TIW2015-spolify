console.log("Spolify JS App Loaded!");

// Example: Clear the "Loading..." message
document.addEventListener('DOMContentLoaded', () => {
    const appContainer = document.getElementById('app');
    if (appContainer) {
        // You'll replace this with actual app rendering logic
        appContainer.innerHTML = '<h1>Spolify (JS Version)</h1><div id="content"></div>';
    }
    // Example API call (you'll build this out)
    // fetch('/api/some-data')
    //     .then(response => response.json())
    //     .then(data => {
    //         console.log(data);
    //         document.getElementById('content').innerText = JSON.stringify(data);
    //     })
    //     .catch(error => console.error('Error fetching data:', error));
});
