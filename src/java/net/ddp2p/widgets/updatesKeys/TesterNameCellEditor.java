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
	//private TesterNameCellPanel myValue;
	TableCellEditor originalEditor;
	
    public TesterNameCellEditor(TableCellEditor originalEditor) {
    	//super(new JComboBox());
        //setOpaque(true); //MUST do this for background to show up.
    	this.originalEditor = originalEditor;
    }

//    @Override
//	public Component getTableCellEditorComponent(
//			JTable table, Object items,
//			boolean isSelected,
//			int row, int column) {
//
//		//if(items==null)
//			if(!DEBUG)System.out.println("TesterNameCellEditor:null items in getTableCellRendererComponent==>"+((TesterNameCellPanel)items).nameTxt.getText()+":"+((TesterNameCellPanel)items).b.getActionCommand() );                  
//		
//		return (TesterNameCellPanel) items;
//	}

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

		// Just check for sanity, this is overkill.
		if (!(component instanceof JTextField))
			throw new RuntimeException(
					"Programmer error, wrong type");

		// The component is a text field and the icons are
		// available.
		final JTextField textField = (JTextField) component;

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

		// Create a button with the icon;
		JButton dotButton = new JButton("...");
		dotButton.setPreferredSize(new Dimension(20,30));

		// Define listener that pops up bigger text editor
		dotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//TableJButton bb =(TableJButton)e.getSource();
				D_Tester uKey = ((UpdatesKeysModel)table.getModel()).data.get(row);
		 		TesterInfoPanel testerPanel= new TesterInfoPanel(D_Tester.getTesterInfoByGID(uKey.testerGID, false, null, null)); 
				JPanel p = new JPanel(new BorderLayout());
				p.setBackground(Color.BLUE);
		        p.setMinimumSize(new Dimension(200,200));
				p.add(new JButton("hi"));
				JOptionPane.showMessageDialog(null,testerPanel,"Tester Info", JOptionPane.DEFAULT_OPTION, null);
			}
		});
		// Add button to panel
		panel.add(dotButton, BorderLayout.EAST);

		// Add the original editor to panel
		panel.add(component, BorderLayout.CENTER);

		// The panel should be displayed
		return panel;
		
	}

}