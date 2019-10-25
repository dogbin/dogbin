/* eslint-env browser */

// SimpleAnalytics reporting script
// slightly modified version of https://github.com/simpleanalytics/scripts/blob/master/src/default.js
(function (window, hostname, path) {
    if (!window) return;

    // Set urls outside try block because they are needed in the catch block
    var protocol = 'https://';
    var apiUrl = protocol + hostname + path;
    var con = window.console;

    try {
        var nav = window.navigator;
        var loc = window.location;
        var userAgent = nav.userAgent;
        var dis = window.dispatchEvent;
        var lastSendUrl;
        var notSending = 'Not sending requests ';

        // A simple log function so the user knows why a request is not being send
        var warn = function (message) {
            if (con && con.warn) con.warn('Simple Analytics: ' + message);
        };

        // We do advanced bot detection in our API, but this line filters already most bots
        if (userAgent.search(/(bot|spider|crawl)/ig) > -1) return warn(notSending + 'because user agent is a robot');

        var post = function (isPushState) {
            // -- dogbin change: use slug instead of path if available --
            var path = loc.pathname;
            if (haste_document) {
                var doc = new haste_document();
                if (doc.key) {
                    path = "/" + doc.key;
                }
            }
            // Obfuscate personal data in URL by dropping the search and hash
            var url = loc.protocol + '//' + loc.hostname + path;

            // Don't send the last URL again (this could happen when pushState is used to change the URL hash or search)
            if (lastSendUrl === url) return;
            lastSendUrl = url;

            // Skip prerender requests
            if ('visibilityState' in doc && doc.visibilityState === 'prerender') return warn(notSending + 'when prerender');

            // Don't track when Do Not Track is set to true
            if ('doNotTrack' in nav && nav.doNotTrack === '1') return warn(notSending + 'when doNotTrack is enabled');

            // Don't track when localhost
            if (loc.hostname === 'localhost' || loc.protocol === 'file:') return warn(notSending + 'from localhost');

            // From the search we grab the utm_source and ref and save only that
            var refMatches = loc.search.match(/[?&](utm_source|source|ref)=([^?&]+)/gi);
            var refs = refMatches ? refMatches.map(function (m) {
                return m.split('=')[1]
            }) : [];

            var data = {url: url};
            if (userAgent) data.ua = userAgent;
            if (refs && refs[0]) data.urlReferrer = refs[0];
            if (doc.referrer && !isPushState) data.referrer = doc.referrer;
            if (window.innerWidth) data.width = window.innerWidth;

            try {
                data.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
            } catch (error) {
                // nothing
            }

            var request = new XMLHttpRequest();
            request.open('POST', apiUrl, true);

            // We use content type text/plain here because we don't want to send an
            // pre-flight OPTIONS request
            request.setRequestHeader('Content-Type', 'text/plain; charset=UTF-8');
            request.send(JSON.stringify(data));
        };

        var his = window.history;
        var hisPushState = his ? his.pushState : null;
        if (hisPushState && Event && dis) {
            var stateListener = function (type) {
                var orig = his[type];
                return function () {
                    var rv = orig.apply(this, arguments);
                    var event = new Event(type);
                    event.arguments = arguments;
                    dis(event);
                    return rv;
                };
            };
            his.pushState = stateListener('pushState');
            window.addEventListener('pushState', function () {
                post(true);
            });
        }

        post();
    } catch (e) {
        if (con && con.error) con.error(e);
        var url = apiUrl + '.gif';
        if (e && e.message) url = url + '?error=' + encodeURIComponent(e.message);
        new Image().src = url;
    }
})(window, 'api.simpleanalytics.io', '/post');