package widgets.updatesKeys;

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
		// This component should be a JLabel
		Component component = originalRenderer.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

		// Just check for sanity, this is overkill.
		if (!(component instanceof JLabel))
			throw new RuntimeException(
					"Programmer error, wrong type");

		// The component is a label
		JLabel label = (JLabel) component;

		// This label must show no icon.
		label.setIcon(null);
		

		// Build a little panel to hold the controls
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		// Color appropriately for selection status
		if (isSelected) {
			panel.setForeground(table.getSelectionForeground());
			panel.setBackground(table.getSelectionBackground());
		} else {
			panel.setForeground(table.getForeground());
			panel.setBackground(table.getBackground());
		}

		// Create and add a button with the icon;
		// this button has no listener
		JButton dotButton = new JButton("...");
		dotButton.setPreferredSize(new Dimension(20,30));
		panel.add(dotButton, BorderLayout.EAST);

		// Add the original JLabel renderer
		panel.add(label, BorderLayout.CENTER);

		// The panel should be displayed
		return panel;
	}
    
}
