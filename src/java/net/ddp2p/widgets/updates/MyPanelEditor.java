/**
 * @(#)ComboBoxEditor.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */
package net.ddp2p.widgets.updates;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;
import javax.swing.JTable;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Panel;
import java.util.ArrayList;
public class MyPanelEditor extends DefaultCellEditor implements TableCellEditor {
	static final boolean DEBUG = false;
	public MyPanelEditor() {
		super(new JComboBox());
	}
	@Override
	public Component getTableCellEditorComponent(
			JTable table, Object items,
			boolean isSelected,
			int row, int column) {
		if(items==null)
			if(DEBUG) System.out.println("MyPanelEditor: null items in getTableCellEditorComponent"); 
		return (JPanel) items;
	}
}
/**
 * @(#)PanelEditor.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */
