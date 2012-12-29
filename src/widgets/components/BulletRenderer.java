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
import static util.Util._;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import config.DDIcons;
import util.Util;

@SuppressWarnings("serial")
public
class BulletRenderer extends JLabel
implements TableCellRenderer {
	static ImageIcon iconOn, iconOff, icon_;
	static JLabel lIconOn, lIconOff, lIcon_;
	public BulletRenderer() {
		try{
			icon_ = DDIcons.getGreyBallImageIcon("UNKNOWN"); // Util.createImageIcon(BULLET_UNKNOWN, "ON");
			iconOn = DDIcons.getGreenBallImageIcon("ON"); // Util.createImageIcon(BULLET_ON, "ON");
			iconOff = DDIcons.getRedBallImageIcon("OFF"); // Util.createImageIcon(BULLET_OFF, "OFF");
			lIcon_ = new JLabel(icon_);
			lIconOn = new JLabel(iconOn); lIconOff = new JLabel(iconOff);
			lIcon_.setToolTipText(_("The last connection not yet tried."));
			lIconOn.setToolTipText(_("The last connection was successful."));
			lIconOff.setToolTipText(_("The last connection was not successful."));
		}catch(Exception e){
			e.printStackTrace();
			lIcon_ = lIconOn = lIconOff = new JLabel("Test");
		}
	}
	@Override
	public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
		try{
		Boolean b;
		if (value == null) return lIcon_;
		if (value instanceof Integer){
			Integer i = (Integer) value;
			int ii = i.intValue();
			switch(ii){
			case 0: return lIconOff;
			default: return lIconOn;
			}
		}
		else if (value instanceof Boolean)	b = (Boolean) value;
		else if (value instanceof String)	b = new Boolean("1".equals(value));
		else return lIcon_;
		if(b.booleanValue())return lIconOn;
		return lIconOff;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
/*
 * previously used by directories
@SuppressWarnings("serial")
class BulletRenderer extends JLabel
implements TableCellRenderer {
	static ImageIcon iconOn, iconOff, icon_;
	static JLabel lIconOn, lIconOff, lIcon_;
	BulletRenderer() {
		icon_ = DDIcons.getGreyBallImageIcon("UNKNOWN"); // Util.createImageIcon(BULLET_UNKNOWN, "ON");
		iconOn = DDIcons.getGreenBallImageIcon("ON"); // Util.createImageIcon(BULLET_ON, "ON");
		iconOff = DDIcons.getRedBallImageIcon("OFF"); // Util.createImageIcon(BULLET_OFF, "OFF");
		lIcon_ = new JLabel(icon_);
		lIconOn = new JLabel(iconOn); lIconOff = new JLabel(iconOff);
		lIcon_.setToolTipText(_("The last connection not yet tried."));
		lIconOn.setToolTipText(_("The last connection was successful."));
		lIconOff.setToolTipText(_("The last connection was not successful."));
	}
	@Override
	public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
		Boolean b;
		if (value == null) return lIcon_;
		if (value instanceof Boolean)	b = (Boolean) value;
		else if (value instanceof String)	b = new Boolean("1".equals(value));
		else return lIcon_;
		if(b.booleanValue())return lIconOn;
		return lIconOff;
	}
}
*/
