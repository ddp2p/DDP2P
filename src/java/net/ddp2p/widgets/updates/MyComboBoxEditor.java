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
import java.awt.Component;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MyComboBoxEditor extends DefaultCellEditor implements TableCellEditor {
  @SuppressWarnings({ "unchecked", "rawtypes" })
public MyComboBoxEditor(String[] items) {
    super(new JComboBox(items));
  }
   @SuppressWarnings("rawtypes")
public MyComboBoxEditor() {
     super(new JComboBox());
   }
   @SuppressWarnings("rawtypes")
   JComboBox c;
   @SuppressWarnings("rawtypes")
@Override
   public Component getTableCellEditorComponent(
                            JTable table, Object items,
                            boolean isSelected,
                            int row, int column) {
//         if(items==null) return null;                  
//        ArrayList<String> a = (ArrayList<String>) items;
//        String[] arr = new String[a.size()];
//        for(int i=0; i< arr.length; i++)
//        	arr[i]=(String) a.get(i);
//        c= new JComboBox(arr);
//        //return c;
//        ((JComboBox) this.editorComponent).addItem("new"); 
//        return this.editorComponent; 
         return (JComboBox) items;
   }
}
