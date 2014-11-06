/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package widgets.components;

import static util.Util.__;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import util.Util;

public class UpdatesFilterKey extends FileFilter {
	public static final String EXT_SK = "sk";
	public static final String EXT_BMP = "bmp";
	public static final String EXT_GIF = "gif";
	public boolean accept(File f) {
	    if (f.isDirectory()) {
	    	return false;
	    }

	    String extension = Util.getExtension(f);
	    if (extension != null) {
	    	//System.out.println("Extension: "+extension);
	    	if (extension.equals(EXT_SK)) {
	    			//System.out.println("Extension: "+extension+" passes");
		        	return true;
	    	}
	    	if (extension.equals(EXT_BMP))
	    		return true;
	    	
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
		return __("Updates Trusted Key File Type")+ "(."+EXT_SK+","+EXT_BMP+")";
	}
}