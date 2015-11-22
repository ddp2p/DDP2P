package net.ddp2p.widgets.updatesKeys;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSeparator;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Tester;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.widgets.updates.TableJButton;
import net.ddp2p.widgets.updatesKeys.*;
public class TesterNameCellEditor implements TableCellEditor{
	private static final boolean DEBUG = false;
	TableCellEditor originalEditor;
    public TesterNameCellEditor(TableCellEditor originalEditor) {
    	this.originalEditor = originalEditor;
    }
    @Override
	public Object getCellEditorValue() {
		return originalEditor.getCellEditorValue();
	}
	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return originalEditor.isCellEditable(anEvent);
	}
	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return originalEditor.shouldSelectCell(anEvent);
	}
	@Override
	public boolean stopCellEditing() {
		return originalEditor.stopCellEditing();
	}
	@Override
	public void cancelCellEditing() {
		originalEditor.cancelCellEditing();
	}
	@Override
	public void addCellEditorListener(CellEditorListener l) {
		originalEditor.addCellEditorListener(l);
	}
	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		originalEditor.removeCellEditorListener(l);
	}
	@Override
	public Component getTableCellEditorComponent(final JTable table,
			final Object value, boolean isSelected, final int row,
			int column) {
		Component component = originalEditor
				.getTableCellEditorComponent(table, value,
						isSelected, row, column);
		if (!(component instanceof JTextField))
			throw new RuntimeException(
					"Programmer error, wrong type");
		final JTextField textField = (JTextField) component;
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
		dotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				D_Tester uKey = ((UpdatesKeysModel)table.getModel()).data.get(row);
		 		TesterInfoPanel testerPanel= new TesterInfoPanel(D_Tester.getTesterInfoByGID(uKey.testerGID, false, null, null)); 
				JPanel p = new JPanel(new BorderLayout());
				p.setBackground(Color.BLUE);
		        p.setMinimumSize(new Dimension(200,200));
				p.add(new JButton("hi"));
				JOptionPane.showMessageDialog(null,testerPanel,"Tester Info", JOptionPane.DEFAULT_OPTION, null);
			}
		});
		panel.add(dotButton, BorderLayout.EAST);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}
}
