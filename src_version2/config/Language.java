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
 package config;

import java.util.regex.Pattern;

public
class Language{
	public String lang;
	public String flavor;
	public Language(String lang, String flavor) {
		this.lang = lang;
		this.flavor = flavor;
	}
	public Language(String authorship_lang) {
		if (authorship_lang == null) return;
		String []s = authorship_lang.split(Pattern.quote("_"));
		this.lang = s[0];
		if (s.length > 0)
			this.flavor = s[1];
	}
	public String toString(){
		return lang+"_"+flavor;
	}
}
