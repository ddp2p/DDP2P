DDP2P
=====

Direct Democracy P2P

src contains the code of version 0.9.55

src_android
	    contains the code reorganized such that the whole GUI of in the package widgets.
            therefore Android specific GUI can be based on the remaining packages (DD_Android.jar)
            A new Gui can get calbacks by implementing the config.Vendor* interfaces
            and to register instances of those classes by assigning them to the config.Application_GUI.gui and
            "config.Application_GUI.dbmail", respectively.

src_version2
	contains the latest version of the code, significantly refactored such that objects
	are managed in cache. Still some likely minor bugs prevent the full synchronization of subobjects 
	(other than Peer and Organizations). I hope I'll have time to fix them (or at least to write a
	guide as how to fix that). Look for a file GUIDE in this folder that would describe the architecture
	and how everything works (for who would have tome to fix what remains to be done).

