# Codelab: Building Beautiful Transitions with Material Motion for Android

The Material motion system for Android is a set of transition patterns within
the [MDC-Android library](https://material.io/components/android) that can help
users understand and navigate an app, as described in the
[Material Design guidelines](https://material.io/design/motion/the-motion-system.html).

This repo houses the source for the
[Material motion system codelab](https://codelabs.developers.google.com/codelabs/material-motion-android),
during which you will build Material transitions into an example email app
called Reply.

The starter code is available on the default `develop` branch, and the complete
code is available on the `complete` branch, which can you can checkout by
running `git checkout complete`.

<img src="screenshots/reply-transitions.gif" alt="Reply transitions"/>

## Container Transform

A container transform in the MDC-Android library is called a MaterialContainerTransform. By default, 
this Transition subclass operates as a shared element transition, meaning the Android Transition 
system is able to pick up two views in different layouts when marked with a transitionName.


