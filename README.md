Radio Battletoads (Alpha!)
==========================

This is a Alpha version of a new player based on libVLC. It's ugly and simple. It just plays
the radio. And displays a stupid text pretending to be funny. The ETA joke is hilarious (nope).

How to compile?
===============
This depends on libVLC. But this source tree does NOT contain libVLC.

 * Download and compile VLC following this instructions: https://wiki.videolan.org/AndroidCompile
 * Copy the compiled libvlcjni.so on libs/<arch> directory (tipically, /libs/armeabi)
 * Drop the entire org.videolan.libvlc package inside your project (inside the src directory).
 * Compile using Eclipse, as usual.
 
What's next for this project?
=============================
I'm just testing if libVLC works OK for most people. So give me feedback, please! I'm listening
on @rbattletoads on Twitter.
After this initial testing phase, I'm planning an application rewrite and overhaul, following the
Android 4.4 visual style guidelines.
It will be much better than the old one, I promise.
With ice cream on the top.

License
=======
This project is released under the GNU GPLv3.
