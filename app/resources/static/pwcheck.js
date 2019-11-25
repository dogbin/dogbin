const protocol = location.protocol === "http:" ? "ws://" : "wss://";
const socket = new WebSocket(protocol + location.host + "/pw");
const pwField = document.getElementsByName("password")[0];

pwField.onchange = function () {
    if (pwField.value) {
        socket.send(pwField.value)
    }
};

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