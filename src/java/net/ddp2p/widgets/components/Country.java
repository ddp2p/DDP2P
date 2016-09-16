/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
 package net.ddp2p.widgets.components;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import net.ddp2p.widgets.app.DDIcons;


public class Country implements IconedItem {
	public String code;
	public String name;
	public Country(String code, String name) {
		this.code = code;
		this.name = name;
	}
	public String getTip(){
		String tip=null;
		name=DDCountrySelector.c_hash.get(code);
		//name=DDTranslation.translate((String)name,new Language("en","US"));
		tip=name;
		return tip;
	}
	public String toString(){
		return name;
	}
	public static String getIconName(String ietf){
		return "steag/c-"+ietf+".png";
	}
	public URL getIconURL() throws MalformedURLException{
		return DDIcons.getResourceURL(getIconName());
		//return new URL ("file:"+getIconName());
	}
	public String getIconName(){
		return "steag/c-"+code+".png";
	}
	@Override
	public ImageIcon getImageIcon() {
		return DDIcons.getImageIconFromResource(getIconName());
	}
}
