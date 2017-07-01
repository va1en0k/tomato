# tomato

A programmer's interactive graphics editor.

## Overview


  Well, as a programmer, those big Adobe Illustrator-type pieces of apocalyptic
  junk have always intimidated me; then there is Processing, which requires of me
  more spatial intuition than I have, as well as too much willingness to put up with
  imperative API... I like to describe stuff with my programmer's words, but I'm bad
  with guessing the right coordinates. And well, even a person like me sometimes just
  needs to throw a few basic shapes together.

  **Tomato** is a **programmer's SVG editor**. It aims to allow you to switch as
  seamlessly as possible from changing your code in your editor to moving stuff around in
  the interactive graphical editor. The interactive features are aiming to be as
  inobtrusive as possible, so you could quickly use the code you wrote in your own app,
  whatever it is (yeah, you can always just export the image to SVG or GIF).

  What I've been looking for is a set of simple concepts to build such a piece of software
  around. Hopefully I'll find a way to make you feel how it all comes together (if it
  actually does). It's at the same time the matter of (worrying) dreams and
  an incredibly outdated endeavour; an ambitious goal and a way to procrastinate.


## Todos

* Library features
    * satisfies vs extends???

* Editor features
    * Hover code cell to highlight
    * Time
    * Randomness
    * Export as SVG, GIF, CLJ
    * Gallery, save to gist
    * JavaScript

* Architecture
    * Decouple the library from the editor

* Performance
    * Don't re-eval the unchanged
    * Debounce evals
    * Put evals into workers
    * Maybe find thus a way to interrupt a computation

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
