/**
 * @(#)ComboBoxEditor.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */
package widgets.updates;

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
		//         if(items==null) return null;                  
		//        ArrayList<String> a = (ArrayList<String>) items;
		//        String[] arr = new String[a.size()];
		//        for(int i=0; i< arr.length; i++)
		//        	arr[i]=(String) a.get(i);
		//        c= new JComboBox(arr);
		//        //return c;
		//        ((JComboBox) this.editorComponent).addItem("new"); 
		//        return this.editorComponent; 
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


