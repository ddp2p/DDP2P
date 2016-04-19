package net.ddp2p.widgets.components;
import static net.ddp2p.common.util.Util.__;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.ddp2p.common.util.Util;
public
class DocumentFilter extends FileFilter {
	public boolean accept(File f) {
	    if (f.isDirectory()) {
	    	return true;
	    }
	    String extension = Util.getExtension(f);
	    if (extension != null) {
	    	if (extension.toLowerCase().equals("pdf") ||extension.toLowerCase().equals("htm") ||
	    		extension.toLowerCase().equals("txt") ||
	    		extension.toLowerCase().equals("html")) {
		        	return true;
	    	} else {
	    		return false;
	    	}
	    }
	    return false;
	}
	@Override
	public String getDescription() {
		return __("Documents (.pdf, .html, .htm, .txt)");
	}
}
