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
package net.ddp2p.widgets.components;

import static net.ddp2p.common.util.Util.__;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.org.OrgExtra;
/**
 * If the value is null, show an empty "" element, set an empty list.
 * Else, split the value based on table.field_extra.SEP_list_of_values (";")
 * 
 * @author msilaghi
 *
 */
@SuppressWarnings("serial")
public
class LVEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	boolean initing = false;
	//final SpinnerModel model = new javax.swing.SpinnerNumberModel();
	//final JSpinner spin = new JSpinner(model);
	JComboBox lv = new JComboBox();
	JTable extras;
	int row,col;
	@SuppressWarnings("unchecked")
	public
	LVEditor() {
		JTextFieldIconed editor = new JTextFieldIconed();
		editor.showIcon = false;
		editor.translating = false;
		lv.setEditor(editor);
		lv.setEditable(true);
		lv.addActionListener(this);
		lv.setRenderer(new LabelRenderer());
		JTextField a = editor;
		a.setToolTipText(__("Type to add new item. Select to delete."));
	}
	/**
	 * Gets the items in the combobox and concatenates them with table.field_extra.SEP_list_of_values ";".
	 * Returns null when there is no item.
	 * @return
	 */
	String buildVal() {
		int k = lv.getItemCount();
		if (k == 0) return null;
		String result = Util.getString(lv.getItemAt(0));
		for (int i = 1; i < k; i ++) {
			result += net.ddp2p.common.table.field_extra.SEP_list_of_values + lv.getItemAt(i);
		}
		return result;		
	}
	@Override
	public Object getCellEditorValue() {
		return buildVal();
	}
	/**
	 * If the value is null, show an empty "" element, set an empty list.
	 * Else, split the value based on table.field_extra.SEP_list_of_values (";")
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable _table,
			Object value,
			boolean isSelected,
			int _row,
			int _column) {
		extras = _table;
		row = _row;
		col = _column;
		lv.getEditor().setItem("");
		lv.removeAllItems();
		if (value == null) return lv;
		String[] vals;
		if (value instanceof String[])
			vals = (String[]) value;
		else
		{
			String val = Util.getString(value);
			vals = val.split(net.ddp2p.common.table.field_extra.SEP_list_of_values);
		}
		initing = true;
		for (String v: vals) {
			if ((v == null) || (v.trim().length() == 0)) continue;
			lv.addItem(new FieldItem(v.trim()));
		}
		initing = false;
		return lv;
	}
    // Enables the editor only for double-clicks.
    public boolean isCellEditable(EventObject evt) {
        //if (evt instanceof MouseEvent) {return ((MouseEvent)evt).getClickCount() >= 2;}
        return true;
    }
	/**
	 * Called when org changed
	 * @param o
	 */
	public void editing_stopped(OrgExtra o) {
		//o.getModel().setValueAt(spin.getValue(), row, col);
		fireEditingStopped(); //Make the renderer reappear.
	}
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent ev) {
		if("comboBoxEdited".equals(ev.getActionCommand())) {
			String added = Util.getString(lv.getEditor().getItem());
			if(DEBUG) System.out.println("OrgExtra:lveditor: added = "+added);
			if((added == null)||((added=added.trim()).length()==0)) return;
			lv.addItem(new FieldItem(added));
		}
		if("comboBoxChanged".equals(ev.getActionCommand())) {
			Object item = lv.getSelectedItem();
			if((item==null) || !(item instanceof FieldItem)){
				if(DEBUG) System.out.println("LVCombo:lveditor: selected="+item);
				return;
			}
			if((!initing&&DD.DELETE_COMBOBOX_WITHOUT_CTRL)||((ev.getModifiers()&ActionEvent.CTRL_MASK)!=0)) {
				if(0==Application_GUI.ask(__("Are you sure to delete:")+"\n\""+item+"\"?", __("Delete"),JOptionPane.OK_CANCEL_OPTION)){
					lv.removeItem(item);
					initing = true;
					if(lv.getItemCount()>0) lv.setSelectedIndex(0);
					initing = false;
				}
				
			}
		}
		/*
		if(DEBUG) System.out.println("OrgExtra:lveditor:action: "+ev);
		String old = buildVal();
		if(DEBUG) System.out.println("OrgExtra:lveditor: old= "+old);
		if(old==null) old = added;
		else old += table.field_extra.SEP_list_of_values+added;
		extras.getModel().setValueAt(old, row, col);
		*/
	}
}
