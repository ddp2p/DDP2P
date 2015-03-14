DDP2P  (LICENSE AFERO GPL)
=====

Direct Democracy P2P

src/java/
	contains the latest version of the code, reorganized and significantly refactored such that objects
	are managed in cache.  Look for a file GUIDE in the DOC folder that would describe the architecture
	and how everything works (for who would have time to do what remains to be done).

src/maven
	contains the configuration code for compiling the project with maven.
	The source code itself is not in this folder, rather some UNIX symbolic links are provided which have
	to be changed to the actual folders in src/java/. Likely in Windows one needs to manually copy the
	corresponding folders from the src/java tree...

src/test
	contains a tree structure where unit tests are to be placed. The unit tests are not yet placed in the tree.

src_ddp2p_android_GUI:
	The android gui code that uses the DD_Android.jar compiled from scr_version2
	Currently this works but the connections over the network not yet fully tested with no-null instances


old/src_version1 contains the outdated code of version 0.9.55

old/src_version1_android
            contains the code reorganized such that the whole GUI of in the package widgets.
            therefore Android specific GUI can be based on the remaining packages (DD_Android.jar)
            A new Gui can get calbacks by implementing the config.Vendor* interfaces
            and to register instances of those classes by assigning them to the config.Application_GUI.gui and
            "config.Application_GUI.dbmail", respectively.

        No longer maintained after 0.9.55, since the development is merged in src_version2, where the compiler extracts
        itself the DD_Android.jar.

old/src_ddp2p_common
        contains the second generation of the code, significantly refactored such that objects
        are managed in cache.  Look for a file GUIDE in the DOC folder that would describe the architecture
        and how everything works (for who would have time to do what remains to be done).

