# Polishing

* Fix bug where self hosted dogbin would still link to the public instance
* Fix bug where the edit button wouldn't work on `/v` style urls
* Set proper caching headers on static files (this should significantly speed up the site)
* I have recently started working on a simple RESTish api, which should allow for third party clients. [View the docs](/api)