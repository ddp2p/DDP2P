package net.ddp2p.widgets.components;

import static net.ddp2p.common.util.Util.__;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.ddp2p.common.util.Util;

public class StegoFilterKey extends FileFilter {
	public static final String EXT_BMP = "bmp";
	public static final String EXT_GIF = "gif";
	public boolean accept(File f) {
	    if (f.isDirectory()) {
	    	return false;
	    }

	    String extension = Util.getExtension(f);
	    if (extension != null) {
	    	//System.out.println("Extension: "+extension);
	    	if (extension.equals(EXT_BMP)) {
	    		//System.out.println("Extension: "+extension+" passes");
	    		return true;
	    	}
	    	if (extension.equals(EXT_GIF))
	    		return true;
	    	
	    	{
    			//System.out.println("Extension: "+extension+" fails");
	    		return false;
	    	}
	    }
		//System.out.println("Extension: absent - "+f);
	    return false;
	}

	@Override
	public String getDescription() {
		return __("Stego File Type")+ "(."+EXT_GIF+","+EXT_BMP+")";
	}
}