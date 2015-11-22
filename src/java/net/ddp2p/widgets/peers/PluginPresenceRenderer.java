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
package net.ddp2p.widgets.peers;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
public class PluginPresenceRenderer implements TableCellRenderer {
	TableCellRenderer renderer;
	TableCellRenderer noPluginRenderer;
	public PluginPresenceRenderer(TableCellRenderer _renderer, TableCellRenderer _noPluginRenderer, TableCellRenderer _default){
		renderer = _renderer;
		noPluginRenderer = _noPluginRenderer;
		if(renderer == null) renderer = _default;
		if(noPluginRenderer == null)  noPluginRenderer = _default;
	}
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if(value != null)
			return renderer.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);
		return noPluginRenderer.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);
	}
}
