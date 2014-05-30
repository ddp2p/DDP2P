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

import config.Language;
import util.*;

/**
 * Implement the selection & editing of Neighborhood subdivisions
 * @author Marius Silaghi
 *
 */
public
class DivisionSelector extends JComboBox{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	
	@SuppressWarnings("unchecked")
	public DivisionSelector(){
		editor = new JTextFieldIconed();
		setEditor(editor);
		setEditable(true);	
		setRenderer(new LabelRenderer());
		addActionListener(this);
	}
	public DivisionSelector(String fields, String divisions, Language lang){
		setEditable(true);
		//String[]splits = divisions.split(":");
		init(fields, divisions, lang);
	}

	@SuppressWarnings("unchecked")
	public void init(String fields, String divisions, Language lang){
		this.removeAllItems();
		if(divisions==null) return;
		String[]splits = divisions.split(":");
		//for(int k=1;k<splits.length;k++) addItem(DDTranslation.translate(splits[k],lang));
		for(int k=1;k<splits.length;k++)
			addItem(DDTranslation.translated(splits[k],lang));
		//addItem(new Translation(DDTranslation.translate(splits[k],lang),lang.lang,lang.flavor,splits[k]));
	}
	public Object getSelectedItem(){
		Object result=super.getSelectedItem();
		if(result instanceof Translation) return result;
		String text = (result==null)?null:result.toString();
		return new Translation(text,
				DDTranslation.authorship_lang.lang,
				DDTranslation.authorship_lang.flavor,text);
	}
	public void actionPerformed(ActionEvent evt) {
		super.actionPerformed(evt);
		onEdited("comboBoxEdited".equals(evt.getActionCommand()),
				"comboBoxEdited".equals(evt.getActionCommand()));
	}
	void onEdited(boolean edited, boolean changed){
        int idx = getSelectedIndex();
		if(DEBUG) System.err.println("DivisionSelector:onEdited:Index="+idx);
        if(idx != -1)  {
        	Translation crt = (Translation)getSelectedItem();
        	setToolTipText(crt.getTip());
        }else{
        	setToolTipText(Translation.getTip(DDTranslation.authorship_lang.toString()));
        	if (edited) {
        		//setTranslation();
        	} 	
        }
	}	
}
