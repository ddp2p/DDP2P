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
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import util.Util;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import static util.Util._;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class DDLanguageSelector extends JComboBox{
	static String[] l=new String[]{"af","ar","be","bg","ca","cs","da","de","el","en","es","et","eu","fa","fi","fo","fr","ga","gd","he","hi","hr","hu","id","is","it","ja","ji","ko","lt","lv","mk","ms","mt","nl","no","pl","pt","rm","ro","ru","sb","sk","sl","sq","sr","sv","sx","sz","th","tn","tr","ts","uk","ur","ve","vi","xh","zh","zu"};
	static String[] ly=new String[]{"af","Afrikaans","ar","Arabic","be","Belarusian","bg","Bulgarian","ca","Catalan","cs","Czech","da","Danish","de","German","el","Greek","en","English","es","Spanish","et","Estonian","eu","Basque","fa","Farsi","fi","Finnish","fo","Faeroese","fr","French","ga","Irish","gd","Gaelic","he","Hebrew","hi","Hindi","hr","Croatian","hu","Hungarian","id","Indonesian","is","Icelandic","it","Italian","ja","Japanese","ji","Yiddish","ko","Korean","lt","Lithuanian","lv","Latvian","mk","Macedonian","ms","Malaysian","mt","Maltese","nl","Dutch","no","Norwegian","pl","Polish","pt","Portuguese","rm","Rhaetomanic","ro","Romanian","ru","Russian","sb","Sorbian","sk","Slovak","sl","Slovenian","sq","Albanian","sr","Serbian","sv","Swedish","sx","Sutu","sz","Sami","th","Thai","tn","Tswana","tr","Turkish","ts","Tsonga","uk","Ukrainian","ur","Urdu","ve","Venda","vi","Vietnamese","xh","Xhosa","zh","Chinese","zu","Zulu"};
	static String[] lcommon=new String[]{"af","ar-ae","ar-bh","ar-dz","ar-eg","ar-iq","ar-jo","ar-kw","ar-lb","ar-ly","ar-ma","ar-om","ar-qa","ar-sa","ar-sy","ar-tn","ar-ye","be","bg","ca","cs","da","de","de-at","de-ch","de-li","de-lu","el","en","en","en-au","en-bz","en-ca","en-gb","en-ie","en-jm","en-nz","en-tt","en-us","en-za","es","es-ar","es-bo","es-cl","es-co","es-cr","es-do","es-ec","es-gt","es-hn","es-mx","es-ni","es-pa","es-pe","es-pr","es-py","es-sv","es-uy","es-ve","et","eu","fa","fi","fo","fr","fr-be","fr-ca","fr-ch","fr-lu","ga","gd","he","hi","hr","hu","id","is","it","it-ch","ja","ji","ko","ko","lt","lv","mk","ms","mt","nl","nl-be","no","no","pl","pt","pt-br","rm","ro","ro-mo","ru","ru-mo","sb","sk","sl","sq","sr","sr","sv","sv-fi","sx","sz","th","tn","tr","ts","uk","ur","ve","vi","xh","zh-cn","zh-hk","zh-sg","zh-tw","zu"};
	static Hashtable<String, String> l_hash=null;// = languages_map();
	@SuppressWarnings("unchecked")
	public DDLanguageSelector(){
		if(l_hash==null)l_hash=languages_map();
		//super(c);
		loadLanguages();
		//editor = new JTextFieldIconed();
		//editor.addMouseListener(this);
		//setEditor(editor);
		setEditable(true);
		setRenderer(new LabelRenderer());
		setChoice(DDTranslation.authorship_lang.flavor);
		addActionListener(this);
		//addMouseListener(this); /// listening on editor		
	}
	static Hashtable<String, String> languages_map() {
		if(l_hash!=null) return l_hash;
		Hashtable<String, String> result = new Hashtable<String, String>();
		for(int i = 0 ; i<ly.length; i+=2)
			result.put(ly[i], ly[i+1]);
		return result;
	}
	static String language(String c){
		Object result=l_hash.get(c);
		if(result == null){
			//System.err.println("Unknown language: "+c);
			return c;
		}
		return (String)result;
		//return DDTranslation.translate((String)result,new Language("en","US"));
	}
	public void setChoice(String c) {
		if(c==null) return;
		for(int k=0;k<this.getItemCount(); k++){
			LanguageItem item=(LanguageItem)this.getItemAt(k);
			if(item.code.equalsIgnoreCase(c)) {
				this.setSelectedItem(item);
				return;
			}
		}
		editor.setItem(new LanguageItem(c,language(c)));
	}
	@SuppressWarnings("unchecked")
	void loadLanguages(){
		for(int i=0; i<l.length; i++) {
			this.addItem(new LanguageItem(l[i],language(l[i])));
		}
	}	
}
