# All new everything
* Completely rewrite dogbin in Kotlin
* Run code highlighting on the server (far better client performance)
    * We use a different (more accurate) highlighter now (chroma)
    * Android logcats will now be highlighted using a custom syntax definition
* Create proper UI for the user system
* Add api keys to allow creating/modifying documents from anywhere (not properly documented yet)
* Use self-hosted iframely for embeds in markdown
* Properly leverage caching server side
* Improve overall performance
* Move away from MongoDB (we use [xodus](https://github.com/jetbrains/xodus) now)
* Display stats collected via Simple Analytics + collect url stats there
* Some small improvements to the frontend
    * Hovering over the creator will now show the creation time
    * Clicking on the view count will now redirect to the public stats on SA
    * An all new syntax theme!