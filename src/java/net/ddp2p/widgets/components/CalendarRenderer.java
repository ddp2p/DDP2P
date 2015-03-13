/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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

import java.awt.Component;
import java.util.Calendar;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public
class CalendarRenderer extends JLabel
implements TableCellRenderer {
	/*
	public static final int SPACED = 1;
	public static final int TIME_ONLY = 2;
	public static final int DAY_ONLY = 3;
	*/
	int mode = Util.CALENDAR_SPACED;
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value == null){
			this.setText("");
			return this;
		}
		if (value instanceof Calendar) {
			String val = Util.renderNicely((Calendar)value, mode);
			//System.out.println("CalendatRenderer: row="+row+" col="+column+" val="+val);
			this.setText(val);
		}
		return this;
	}
	/*
	static String renderNicely(Calendar value, int mode2) {
		if (value == null) return null;
		String result;
		switch (mode2) {
		case SPACED:
			result = String.format("%1$tY/%1$tm / %1$td %1$tH:%1$tM:%1$tS.%1$tLZ",value);	
			return result;
		case TIME_ONLY:
			result = String.format("%1$tH:%1$tM:%1$tS.%1$tLZ",value);	
			return result;
		case DAY_ONLY:
			result = String.format("%1$tY/%1$tm/%1$td",value);	
			return result;
		default:
			return Encoder.getGeneralizedTime(value);
		}
	}
	*/
}