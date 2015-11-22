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
package net.ddp2p.widgets.components;
import static net.ddp2p.common.util.Util.__;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public class LVComboBox extends JComboBox {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	boolean initing = false;
	int row,col;
	@SuppressWarnings("unchecked")
	public
	LVComboBox() {
		super();
		JTextFieldIconed editor = new JTextFieldIconed();
		editor.showIcon = false;
		editor.translating = false;
		setEditor(editor);
		setEditable(true);
		addActionListener(this);
		setRenderer(new LabelRenderer());
		JTextField a = editor;
		a.setToolTipText(__("Type to add new item. Select to delete."));
	}
	/**
	 * use as separator table.field_extra.SEP_list_of_values = ";"
	 * @return
	 */
	String buildVal() {
		return buildVal(net.ddp2p.common.table.field_extra.SEP_list_of_values);
	}
	/**
	 * Use separator sep
	 * @param sep separator
	 * @return
	 */
	public String buildVal(String sep){
		int k=getItemCount();
		if (k==0) return null;
		String result = Util.getString(getItemAt(0));
		for(int i = 1; i < k; i++) {
			result+=sep+getItemAt(i);
		}
		return result;		
	}
	public String[] getVal(){
		String[] result;
		int k=getItemCount();
		if (k==0) return null;
		result = new String[k];
		for(int i = 0; i < k; i++) result[i] = Util.getString(getItemAt(i));
		return result;		
	}
	/**
	 * When editing, user should use the table.field_extra.SEP_list_of_values=";" separator
	 * @return
	 */
	public Object getCellEditorValue() {
		return buildVal();
	}
	/**
	 * Use table.field_extra.SEP_list_of_values=";" as separator
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Component setValue(Object value) {
		return setValue(value, net.ddp2p.common.table.field_extra.SEP_list_of_values);
	}
	/**
	 * split based on separator sep
	 * empty fields, null or "" are discarded
	 * @param value
	 * @param sep separator
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Component setValue(Object value, String sep) {
		if(value==null) return setArrayValue(null);
		String val = Util.getString(value);
		String[] vals = val.split(sep);
		return setArrayValue(vals);
	}
	/**
	 * empty fields, null or "" are discarded
	 * @param value
	 * @param sep separator
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Component setArrayValue(String[] vals) {
		this.getEditor().setItem("");
		this.removeAllItems();
		if(vals==null) return this;
		initing = true;
		for(String v: vals){
			if((v==null) || (v.trim().length()==0)) continue;
			this.addItem(new FieldItem(v.trim()));
		}
		initing = false;
		this.fireLV();
		return this;
	}
 	@Override
 	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent ev) {
		if(DEBUG) System.out.println("LVCombo:lveditor: action = "+ev);
		if(!"comboBoxChanged".equals(ev.getActionCommand())) {
			String added = Util.getString(this.getEditor().getItem());
			if(DEBUG) System.out.println("LVComboBox:lveditor: added = "+added);
			if((added == null)||((added=added.trim()).length()==0)){
				if(DEBUG) System.out.println("LVCombo:lveditor: added null");
				return;
			}
			this.addItem(new FieldItem(added));
			this.fireLV();
		}
		if("comboBoxChanged".equals(ev.getActionCommand())) {
			Object item = this.getSelectedItem();
			if((item==null) || !(item instanceof FieldItem)){
				if(DEBUG) System.out.println("LVCombo:lveditor: selected="+item);
				return;
			}
			if((!initing&&DD.DELETE_COMBOBOX_WITHOUT_CTRL)||((ev.getModifiers()&ActionEvent.CTRL_MASK)!=0)) {
				if(0==Application_GUI.ask(__("Are you sure to delete:")+"\n\""+item+"\"?", __("Delete"),JOptionPane.OK_CANCEL_OPTION)){
					this.removeItem(item);
					initing = true;
					if(this.getItemCount()>0)this.setSelectedIndex(0);
					initing = false;
					this.fireLV();
				}				
			}
		}
	}
 	public ArrayList<LVListener> listeners = new ArrayList<LVListener>();
 	void fireLV(){
		if(DEBUG) System.out.println("LVComboBox: fireLV: set = #"+listeners.size());
		for (LVListener l : listeners) {
 			try{
 				if(DEBUG) System.out.println("LVComboBox: fireLV: "+l);
				l.listenLV(this);
 			}catch(Exception e){e.printStackTrace();}
 		}
 	}
	public void addLVListener(LVListener l) {
		if(DEBUG) System.out.println("LVComboBox: addLVListener: "+l);
		if(DEBUG) System.out.println("LVComboBox: addLVListener: set = #"+listeners.size());
		listeners.add(l);
		if(DEBUG) System.out.println("LVComboBox: addLVListener: after set = #"+listeners.size());
	}
	public void removeLVListener(LVListener l) {
		if(DEBUG) System.out.println("LVComboBox: removeLVListener: "+l);
		if(DEBUG) System.out.println("LVComboBox: removeLVListener: set = #"+listeners.size());
		while(listeners.contains(l))
			listeners.remove(l);
		if(DEBUG) System.out.println("LVComboBox: removeLVListener: after set = #"+listeners.size());
	}
}
