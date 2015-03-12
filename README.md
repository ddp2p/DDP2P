DDP2P  (LICENSE AFERO GPL)
=====

Direct Democracy P2P

src_version2
	contains the latest version of the code, significantly refactored such that objects
	are managed in cache.  Look for a file GUIDE in the DOC folder that would describe the architecture
	and how everything works (for who would have time to do what remains to be done).

src_android_GUI:
	The android gui code that uses the DD_Android.jar compiled from scr_version2
	Currently this works but the connections over the network not yet fully tested with no-null instances


src_version1 contains the outdated code of version 0.9.55

src_version1_android
            contains the code reorganized such that the whole GUI of in the package widgets.
            therefore Android specific GUI can be based on the remaining packages (DD_Android.jar)
            A new Gui can get calbacks by implementing the config.Vendor* interfaces
            and to register instances of those classes by assigning them to the config.Application_GUI.gui and
            "config.Application_GUI.dbmail", respectively.

        No longer maintained after 0.9.55, since the development is merged in src_version2, where the compiler extracts
        inself the DD_Android.jar.

