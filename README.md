ftl-profile-editor
==================

A 3rd-party tool to edit user files for [FTL](https://subsetgames.com/). It depends on resources from the game directory, but the game itself will not be modified.

With this, you can unlock any or all ships and achievements in your profile, or tweak most aspects of saved games: crew, fires, breaches, stores, etc.


Status
-----
* FTL 1.6.4 profiles are fully editable, some Advanced Edition features are not supported.
* FTL 1.5.4-1.6.3 profiles are fully editable. (ae_prof.sav)
* FTL 1.01-1.03.3 profiles are fully editable. (prof.sav)
<br /><br />
* FTL 1.6.4 saved games are fully editable, some Advanced Edition features are not supported.
* FTL 1.5.4-1.6.3 saved games are partially editable.
* FTL 1.01-1.03.3 saved games are fully editable.

<a href="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss1.png"><img src="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss1.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss2.png"><img src="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss1.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss3.png"><img src="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss3.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss4.png"><img src="https://raw.github.com/reseto/ftl-profile-editor/master/img/ftl-edit-v29-ss4.png" width="145px" height="auto" /></a>

To download compiled binaries, [click here](https://sourceforge.net/projects/ftleditor/).

Comments can be made in a forum thread [here](https://subsetgames.com/forum/viewtopic.php?f=7&t=10959).

I do not have a PayPal, please give all credit to Vhati :) <br />
Vhati accepts PayPal donations [here](https://vhati.github.io/donate.html). <br />
Thanks and enjoy!


Usage
-----
* Quit FTL before editing profiles.
<br /><br />
* For saved games, you must NOT be actively playing a campaign.
    * FTL 1.5.4+: The main menu is safe.
    * FTL 1.01-1.03.3: "Save+Quit".
<br /><br />
* Double-click FTLProfileEditor.exe.
* Switch to the appropriate tab: "Profile" or "Saved Game".
* Open a profile (ae_prof.sav or prof.sav) or saved game (continue.sav).
* Make any desired changes, and save.
<br /><br />
* Continue playing FTL.


Requirements
------------
* Java (1.7 or higher, tested on the latest Java 17.0.1).
    * http://www.java.com/en/download/
* FTL (1.01-1.03.3 or 1.5.4-1.6.4, Windows/OSX/Linux, Steam/GOG/Standalone).
    * https://subsetgames.com/
    * some Advanced Edition features are not supported by this editor
* WinXP SP1 can't run Java 1.7.
    * (1.7 was built with VisualStudio 2010, causing a DecodePointer error.)
    * To get 1.6, you may have to google "jdk-6u45-windows-i586.exe".


History
-------
This project forked to release v12 and continue development. The original codebase started by ComaToes can be found [here](https://github.com/ComaToes/ftl-profile-editor) and its associated forum thread is [here](https://subsetgames.com/forum/viewtopic.php?f=7&t=2877).
