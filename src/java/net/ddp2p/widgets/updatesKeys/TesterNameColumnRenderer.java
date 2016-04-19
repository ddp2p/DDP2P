package net.ddp2p.widgets.updatesKeys;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
public class TesterNameColumnRenderer implements TableCellRenderer {
    static final boolean DEBUG = false;
    TableCellRenderer originalRenderer;
	public TesterNameColumnRenderer (TableCellRenderer originalRenderer) {
        this.originalRenderer =  originalRenderer;
    }
	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		Component component = originalRenderer.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
		if (!(component instanceof JLabel))
			throw new RuntimeException(
					"Programmer error, wrong type");
		JLabel label = (JLabel) component;
		label.setIcon(null);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		if (isSelected) {
			panel.setForeground(table.getSelectionForeground());
			panel.setBackground(table.getSelectionBackground());
		} else {
			panel.setForeground(table.getForeground());
			panel.setBackground(table.getBackground());
		}
		JButton dotButton = new JButton("...");
		dotButton.setPreferredSize(new Dimension(20,30));
		panel.add(dotButton, BorderLayout.EAST);
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
}
