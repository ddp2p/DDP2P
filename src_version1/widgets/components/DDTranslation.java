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
 package widgets.components;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import util.DBInterface;
import util.Util;

import static util.Util._;

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
	public static Translation translated(String source, Language from) {
		ArrayList<ArrayList<Object>> sel =
			translates(source, from);
		if(sel==null) return new Translation(source,null,null,source);
		return new Translation(Util.sval(sel.get(0).get(0),""),
				Util.sval(sel.get(0).get(1),""),
				Util.sval(sel.get(0).get(2),""),
				source);
	}
	public static String translate(String source, Language from) {
		ArrayList<ArrayList<Object>> sel =
			translates(source, from);
		if(sel==null) return source;
		return (String)sel.get(0).get(0);
	}
	static ArrayList<ArrayList<Object>> translates(String source, Language from) {
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
					where = " and "+table.translation.translation_lang+" = ? ";
					params = new String[]{source, preferred_languages[k].lang};
					params_submitter = new String[]{sID,source, preferred_languages[k].lang};
				}
				if(from != null) {
					where = " and "+table.translation.value_lang+" = ?  and "+table.translation.translation_lang+" = ? ";
					params = new String[]{source, from.lang, preferred_languages[k].lang};
					params_submitter = new String[]{sID,source, from.lang, preferred_languages[k].lang};
				}
			}else{
				if(from == null) {
					where = " and "+table.translation.translation_lang+" = ?  and "+table.translation.translation_flavor+" = ? ";
					params = new String[]{source, preferred_languages[k].lang, preferred_languages[k].flavor};
					params_submitter = new String[]{sID,source, preferred_languages[k].lang, preferred_languages[k].flavor};
				}				
				if(from != null) {
					where = " and "+table.translation.value_lang+" = ?  and "+table.translation.translation_lang+" = ? and "+table.translation.translation_flavor+" = ? ";
					params = new String[]{source, from.lang, preferred_languages[k].lang, preferred_languages[k].flavor};
					params_submitter = new String[]{sID,source, from.lang, preferred_languages[k].lang, preferred_languages[k].flavor};
				}
			}

			try{
				if(DEBUG) System.out.println("DDTranslation:translates:look1");
				sel=db.select("select "+table.translation.translation+","+ table.translation.translation_lang+"," +table.translation.translation_flavor+"," +table.translation.translation_charset+" from "+table.translation.TNAME+" where "+table.translation.submitter_ID+" = ? and "+table.translation.value+" = ? " +
						where + " GROUP BY "+table.translation.translation+"; ",
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
				sel=db.select("select "+table.translation.translation+","+ table.translation.translation_lang+","+ table.translation.translation_flavor+"," +table.translation.translation_charset+", count(*) AS counts from "+table.translation.TNAME +
					" where "+table.translation.value+" = ? "+where+" GROUP BY "+table.translation.translation+" ORDER BY counts DESC;",
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
					sel = db.select("select "+table.translation.translation+"," +table.translation.translation_lang+"," +table.translation.translation_flavor+","+ table.translation.translation_charset+", count(*) as counts from "+table.translation.TNAME+
						" where "+table.translation.value+" = ? and "+table.translation.translation_charset+" = ?;",
						new String[]{source, preferred_charsets[k]},DEBUG);
				else
					sel = db.select("select "+table.translation.translation+"," +table.translation.translation_lang+","+ table.translation.translation_flavor+"," +table.translation.translation_charset+" count(*) as counts from "+table.translation.TNAME +
							" where "+table.translation.value+" = ? and "+table.translation.value_lang+" = ? and "+table.translation.translation_charset+" = ?;",
							new String[]{source, from.lang, preferred_charsets[k]},DEBUG);
			}catch(Exception e) {
				e.printStackTrace();
	    		return null;				
			}
		}
		return null;
	}
}

