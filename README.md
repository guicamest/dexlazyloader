Dex Lazy Loader (DYL)
======

*Call me when you need me! - Somebody*

Dex Lazy Loader (DYL) library and plugin were born as a result of reading Macarse's Lazy Loading Dex files article. If you haven't read it yet, I strongly recommend you do before considering using DYL.

The library provides a LazyLoader to load dexed jars on runtime.

The plugin takes care of dexing provided dependencies and jars located at dyl folder into your assets folder.
For more information, visit the website.

Sample
------

You can find a sample application under the *sample* folder.

Known Issues
------------

- Android Studio doesn't automatically rebuild if the dyl folder is modified (like it does with other resources). Therefore, if you add/remove jars to be dexed you will have to manually rebuild before they appear in assets folder.

Planned Features
----------------

- Suggestions are welcome
