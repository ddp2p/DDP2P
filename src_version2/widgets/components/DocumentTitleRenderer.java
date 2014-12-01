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
package widgets.components;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import data.D_Document_Title;

public class DocumentTitleRenderer implements TableCellRenderer  {

	public static boolean DEBUG = false;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof String) {
			if (DEBUG) System.out.println("DocumentTitleRenderer: getTableCellRendererComponent: string:"+value);
			String val = (String) value;
			JLabel result = new JLabel(val);
			result.setBackground(Color.GRAY);
			result.setForeground(Color.GREEN);
			return result;
		}
		
		if (value instanceof D_Document_Title){
			if (DEBUG) System.out.println("DocumentTitleRenderer: getTableCellRendererComponent: ddt="+value);
			D_Document_Title dt = (D_Document_Title) value;
			if (dt.title_document == null) return null;
			String val = dt.title_document.getDocumentString();
			String fmt = dt.title_document.getFormatString();
			if (fmt == null) {
				JLabel c = new JLabel(val);
				return c;
			}
			JLabel c = new JLabel(val);
			return c;
		}
		return null;
	}

}
