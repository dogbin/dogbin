// From: https://davidwalsh.name/javascript-debounce-function
// Returns a function, that, as long as it continues to be invoked, will not
// be triggered. The function will be called after it stops being called for
// N milliseconds. If `immediate` is passed, trigger the function on the
// leading edge, instead of the trailing.
function debounce(func, wait, immediate) {
    var timeout;
    return function () {
        var context = this, args = arguments;
        var later = function () {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        var callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
};

const protocol = location.protocol === "http:" ? "ws://" : "wss://";
const socket = new WebSocket(protocol + location.host + "/pw");
const pwField = document.getElementsByName("password")[0];

pwField.oninput = debounce(function () {
    if (pwField.value) {
        socket.send(pwField.value)
    }
}, 250);

socket.onmessage = function (event) {
    // TODO: proper UI, displaying the score and more suggestions
    let result = JSON.parse(event.data);
    if (result.accepted) {
        pwField.setCustomValidity("");
    } else {
        let error = result.suggestions[0];
        if (!error) {
            error = result.warning;
        }
        pwField.setCustomValidity(error);
    }
};