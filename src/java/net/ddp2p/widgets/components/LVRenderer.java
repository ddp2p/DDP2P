package net.ddp2p.widgets.components;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public
class LVRenderer extends JLabel
implements TableCellRenderer {
	 JLabel label = new JLabel();
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		String text;
		if (value == null) return null;
		if (value instanceof String[])
			text = Util.concat((String[])value, ", ");
		else text = Util.getString(value);
		label.setText(text);
		label.setToolTipText(text);
		return label;
	}
}
