# Swing Sunmap

An auto-updating Swing visualization of the sunlit portion of Earth, as a Clojure
learning exercise.  My very first Swing app!

Developed as a follow-on to the Clojurescript  websunmap  which shares the
exact logic of  `sunmap.clj`.


## Credits

The code in  `sunmap.clj`  is translated or freely paraphrased from Java code
in SkyviewCafe which carry the following notice:

    ```
    Copyright (C) 2000-2007 by Kerry Shetline, kerry@shetline.com.

    This code is free for public use in any non-commercial application. All
    other uses are restricted without prior consent of the author, Kerry
    Shetline. The author assumes no liability for the suitability of this
    code in any application.

    2007 MAR 31   Initial release as Sky View Cafe 4.0.36.
    ```

The Swing visualization code in  `core.clj`  is inspired by:
    ``` 
    https://github.com/sebastianbenz/clojure-game-of-life

    Copyright (C) 2010 Sebastian Benz    Eclipse Public License
    ```

Hat tip to  https://Dillinger.io  for markdown edit/preview.

## Usage

Can be run  (from dir containing  src/...):

    ```
    $ clojure -M -m sw-sm.core
    ```
or from wherever you have placed the uberjar:

    ```
    $ java -jar path-to-jar/sw-sm.jar
    ```

A visualization of sunlit portion of Earth will appear, initialized at the UTC
 when the app was opened; as the countdown reaches 0 the image will update,
 the countdown will reset; and so on, continuing until the app is exited.

### Bugs
None known.


## License

Copyright © 2018-2023   L. E. Vandergriff

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
   ```
"Freely you have received, freely give."  Mt. 10:8
