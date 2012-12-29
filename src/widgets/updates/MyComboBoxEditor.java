/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed
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
import java.awt.Component;
import java.util.ArrayList;

public class MyComboBoxEditor extends DefaultCellEditor implements TableCellEditor {
 // @SuppressWarnings("unchecked")
public MyComboBoxEditor(String[] items) {
    super(new JComboBox<String>(items));
  }
   public MyComboBoxEditor() {
     super(new JComboBox());
   }
   JComboBox c;
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
