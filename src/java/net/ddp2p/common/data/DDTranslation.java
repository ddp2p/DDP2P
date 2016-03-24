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
 package net.ddp2p.common.data;
import java.util.ArrayList;

import net.ddp2p.common.config.Language;
import net.ddp2p.common.util.DBInterface;

/**
 * Class to query the database about translations of given texts in given languages
 * @author msilaghi
 *
 */
public class DDTranslation {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static Language[] preferred_languages;
	public static String[] preferred_charsets;
	public static DBInterface db;
	public static Language authorship_lang;
	public static String authorship_charset;
	public static long constituentID;
	public static long organizationID;
	public static Language org_language=new Language("en","US");
	/**
	 * Translated into the first preferred_languages
	 * 
	 * @param source
	 * @param from
	 * @return
	 */
	public static String translate(String source, Language from) {
		ArrayList<ArrayList<Object>> sel =
			translates(source, from);
		if (sel==null || sel.size() == 0) return source;
		return (String)sel.get(0).get(0);
	}
	/**
	 * Translated into preferred_languages
	 * @param source
	 * @param from
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> translates(String source, Language from) {
		ArrayList<ArrayList<Object>> sel;
		String where = "";
		String params[]=new String[]{};
		String params_submitter[]=new String[]{};
		String sID=DDTranslation.constituentID+"";
		if(source == null) return null;
		if(preferred_languages==null) return null;
		for(int k = 0; k<preferred_languages.length; k++) {
			if(preferred_languages[k].lang==null) return null;
			if(preferred_languages[k].flavor==null) {
				if(from == null) {
					where = " and "+net.ddp2p.common.table.translation.translation_lang+" = ? ";
					params = new String[]{source, preferred_languages[k].lang};
					params_submitter = new String[]{sID,source, preferred_languages[k].lang};
				}
				if(from != null) {
					where = " and "+net.ddp2p.common.table.translation.value_lang+" = ?  and "+net.ddp2p.common.table.translation.translation_lang+" = ? ";
					params = new String[]{source, from.lang, preferred_languages[k].lang};
					params_submitter = new String[]{sID,source, from.lang, preferred_languages[k].lang};
				}
			}else{
				if(from == null) {
					where = " and "+net.ddp2p.common.table.translation.translation_lang+" = ?  and "+net.ddp2p.common.table.translation.translation_flavor+" = ? ";
					params = new String[]{source, preferred_languages[k].lang, preferred_languages[k].flavor};
					params_submitter = new String[]{sID,source, preferred_languages[k].lang, preferred_languages[k].flavor};
				}				
				if(from != null) {
					where = " and "+net.ddp2p.common.table.translation.value_lang+" = ?  and "+net.ddp2p.common.table.translation.translation_lang+" = ? and "+net.ddp2p.common.table.translation.translation_flavor+" = ? ";
					params = new String[]{source, from.lang, preferred_languages[k].lang, preferred_languages[k].flavor};
					params_submitter = new String[]{sID,source, from.lang, preferred_languages[k].lang, preferred_languages[k].flavor};
				}
			}

			try{
				if(DEBUG) System.out.println("DDTranslation:translates:look1");
				sel=db.select("select "+net.ddp2p.common.table.translation.translation+","+ net.ddp2p.common.table.translation.translation_lang+"," +net.ddp2p.common.table.translation.translation_flavor+"," +net.ddp2p.common.table.translation.translation_charset+" from "+net.ddp2p.common.table.translation.TNAME+" where "+net.ddp2p.common.table.translation.submitter_ID+" = ? and "+net.ddp2p.common.table.translation.value+" = ? " +
						where + " GROUP BY "+net.ddp2p.common.table.translation.translation+"; ",
					params_submitter, DEBUG);
				if(sel.size() > 0){
					return sel;
				}
			}catch (Exception e) {
				e.printStackTrace();
	    		return null;
			}
			
			try{
				if(DEBUG) System.out.println("DDTranslation:translates:look2");
				sel=db.select("select "+net.ddp2p.common.table.translation.translation+","+ net.ddp2p.common.table.translation.translation_lang+","+ net.ddp2p.common.table.translation.translation_flavor+"," +net.ddp2p.common.table.translation.translation_charset+", count(*) AS counts from "+net.ddp2p.common.table.translation.TNAME +
					" where "+net.ddp2p.common.table.translation.value+" = ? "+where+" GROUP BY "+net.ddp2p.common.table.translation.translation+" ORDER BY counts DESC;",
					params, DEBUG);
				if(sel.size() > 0){
					return sel;
				}
			}catch(Exception e) {
				e.printStackTrace();
	    		return null;
			}
		}
		if(preferred_charsets == null) return null;
		for(int k = 0; k<preferred_charsets.length;  k++) {
			if(DEBUG) System.out.println("DDTranslation:translates:look3");
			try {
				if(from==null)
					sel = db.select("select "+net.ddp2p.common.table.translation.translation+"," +net.ddp2p.common.table.translation.translation_lang+"," +net.ddp2p.common.table.translation.translation_flavor+","+ net.ddp2p.common.table.translation.translation_charset+", count(*) as counts from "+net.ddp2p.common.table.translation.TNAME+
						" where "+net.ddp2p.common.table.translation.value+" = ? and "+net.ddp2p.common.table.translation.translation_charset+" = ?;",
						new String[]{source, preferred_charsets[k]},DEBUG);
				else
					sel = db.select("select "+net.ddp2p.common.table.translation.translation+"," +net.ddp2p.common.table.translation.translation_lang+","+ net.ddp2p.common.table.translation.translation_flavor+"," +net.ddp2p.common.table.translation.translation_charset+" count(*) as counts from "+net.ddp2p.common.table.translation.TNAME +
							" where "+net.ddp2p.common.table.translation.value+" = ? and "+net.ddp2p.common.table.translation.value_lang+" = ? and "+net.ddp2p.common.table.translation.translation_charset+" = ?;",
							new String[]{source, from.lang, preferred_charsets[k]},DEBUG);
			}catch(Exception e) {
				e.printStackTrace();
	    		return null;				
			}
		}
		return null;
	}
}

