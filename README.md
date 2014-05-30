DDP2P
=====

Direct Democracy P2P

src contains the code of version 0.9.55

src_android
	    contains the code reorganize such that the whole GUI of in the package widgets.
            therefore Android specific GUI can be based on the remaining packages (DD_Android.jar)
            A new Gui can get calbacks by implementing the config.Vendor* interfaces
            and to register instances of those classes by assigning them to the config.Application_GUI.gui and
            "config.Application_GUI.dbmail", respectively.
