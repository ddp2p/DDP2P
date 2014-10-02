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
package widgets.org;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.table.TableCellEditor;

@SuppressWarnings("serial")
class SpinEditor extends AbstractCellEditor implements TableCellEditor {
	final SpinnerModel model =
	        new javax.swing.SpinnerNumberModel();
	final JSpinner spin = new JSpinner(model);
	int row,col;
	SpinEditor() {}
	@Override
	public Object getCellEditorValue() {
		return spin.getValue();
	}
	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value,
			boolean isSelected,
			int _row,
			int _column) {
		row = _row;
		col = _column;
		try{value = (Integer)value;}catch(Exception e){value = new Integer("-1");}
		if(value==null)value = new Integer("-1");
		spin.setValue(value);
		return spin;
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
}
