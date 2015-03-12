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

import static util.Util._;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import widgets.app.DDIcons;
import config.DD;


class FieldItem implements IconedItem {
	public String name;
	public FieldItem(String name) {
		this.name = name;
	}
	@Override
	public String getTip() {
		return _("Type to add new. Select item to delete it!");
	}
	public String toString(){
		return name;
	}
	//public static String getIconName(){return "icons/del.png";}
	@Override
	public URL getIconURL() throws MalformedURLException{
		return DDIcons.getDeleURL();//DD.class.getResource();
		//return new URL ("file:"+getIconName());
	}
	@Override
	public ImageIcon getImageIcon() {
		return DDIcons.getDeleImageIcon(null);
	}
}
