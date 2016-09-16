package net.ddp2p.widgets.components;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class XUtil {

	public static void clipboardCopy(String exportText) {
		StringSelection selection = new StringSelection(exportText);
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(selection, selection);
	}

}
