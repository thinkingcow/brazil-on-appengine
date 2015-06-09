# Introduction #

This is the first pass at porting the Brazil Framework to run on Appengine.  Although the original Brazil framework will already run as a servlet, Appengine imposes several important restrictions that preclude the existing **servlet** configuration from running, including:
  * No access to `Socket`, `ServerSocket`, `Thread`s, and several other system classes that are used throughout the core of the system.
  * No **filesystem**, the availability of which is a key assumption underlying much of the existing code base.

In addition, the existing Brazil code is was kept compatible with older (way older) version of the Java VM, to insure that it would work on special platforms that are not relevant to Appengine.

# Goal #

Merge this port with the existing sources so there is only one branch,
fully updated to a modern VM, that runs both standalone and in most servlet containers.

# Status #

  * The system now runs in appengine, supporting most of the important existing capabilities, along with a few new ones that permit almost anything to be changed on the running server without the need to do a **re-deploy**.
  * This project includes a working "sample" server that incorporates most of the features of the current system.
  * The core `server` and `request` and associated classes were rewritten either as Interfaces or Abstract classes; the Appengine versions can then subclass/implement them in a way that avoids system classes unavailable to appengine
  * Modules (e.g. handlers, templates, etc) that don't make sense in the appengine environment were left out.
  * A "virtual" filesystem was added using the Appengine JDO datastore, and new modules were added to use that instead of a filesystem.
  * Where practical, the code was "updated" to Java 1.6 by the introduction of generics and new collection classes
  * Many of the public fields were replaced by accessors.

# ToDo #

  * The system no longer runs as a standalone Web server, as the refactoring of Request isn't finished.
  * The system uses `Properties` extensively, which can't be properly `generic`ized (because they are defined as <Object,Object> instead of <String,String> for some reason I can't grok).  This needs to be rewritten, along with StringMap, and a few other utility classes that have generic resistance, then the code can be made fully "generic".
  * Modules I didn't need were left by the wayside.  Some are obsolete and have been superceded, others I haven't gotten to yet.

# Miscellaneous stuff #

  * If this port proves to be viable, the test suite, that got completely trashed for this port, needs to be redone, probably using junit.
  * The build system now uses **Ant**, because that is what appengine plays with the best.