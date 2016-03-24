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
package net.ddp2p.widgets.org;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import net.ddp2p.common.util.Util;

@SuppressWarnings("serial")
public
class ColorRenderer extends JLabel implements TableCellRenderer {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	OrgsModel model;
	public ColorRenderer(OrgsModel _model) {
		super();
		model = _model;
		setOpaque(true);
	}
	@Override
	public Component getTableCellRendererComponent(JTable table, Object obj,
			boolean isSelected, boolean hasFocus, int row, int column) {
		int model_row = table.convertRowIndexToModel(row);
		if(model.isNotReady(model_row)) { // I am creating it
			setBackground(Color.BLACK);
			this.setForeground(Color.YELLOW);
			this.setText(Util.getString(obj));
			if(DEBUG) System.out.println("org.ColorRenderer:nr Row="+model_row+" Col="+column+"  val="+obj);
		}else if(model.isMine(model_row)) { // I created it
			setBackground(Color.YELLOW);
			this.setForeground(Color.BLUE);
			this.setText(Util.getString(obj));
			if(DEBUG) System.out.println("org.ColorRenderer:mine Row="+model_row+" Col="+column+"  val="+obj);
		}else if(model.isBlocked(model_row)) { // Do not store data about this
			setBackground(Color.WHITE);
			this.setForeground(Color.RED);
			this.setText(Util.getString(obj));
			if(DEBUG) System.out.println("org.ColorRenderer:blocked Row="+model_row+" Col="+column+"  val="+obj);
		}else if(model.isBroadcasted(model_row)) { // Advertise and send
			setBackground(Color.WHITE);
			this.setForeground(Color.BLUE);
			this.setText(Util.getString(obj));
			if(DEBUG) System.out.println("org.ColorRenderer:broadcasted Row="+model_row+" Col="+column+"  val="+obj);
		}else if(model.isRequested(model_row)) { // Request it
			setBackground(Color.WHITE);
			this.setForeground(Color.green);
			this.setText(Util.getString(obj));
			if(DEBUG) System.out.println("org.ColorRenderer:requested Row="+model_row+" Col="+column+"  val="+obj);
		}else{
			if(DEBUG) System.out.println("org.ColorRenderer:normal Row="+model_row+" Col="+column+"  val="+obj);
			setBackground(Color.WHITE);
			this.setForeground(Color.BLACK);		
			this.setText(Util.getString(obj));
		}
		//setToolTipText("");
		return this;
	}
	
}
