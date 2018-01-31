function updatePrefs(prefs) {
    // var prefsObject = JSON.parse(prefs);
    // for (var key in prefsObject) {
    //     var value = prefsObject[key];
    //     // t.getPrefs().set(key, value);
    // }
}

document.addEventListener("DOMContentLoaded", function (event) {

    window.term = new Terminal({
        cursorBlink: true,
        scrollback: 2500
    });

    let termDom = document.querySelector('#terminal');

    term.on('resize', ({cols, rows}) => {
        app.resizeTerminal(cols, rows);
    });
    term.on('data', (command) => {
        app.command(command);
    });

    term.open(termDom, true);

    term.linkify();
    term.toggleFullscreen();
    term.fit();
    term.focus();
    //
    window.addEventListener('resize', debounce(term.fit.bind(term), 100, false), false);

    app.onTerminalReady();

});

const debounce = function (func, wait, immediate) {
    var timeout;
    return () => {
        const context = this, args = arguments;
        const later = function () {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}