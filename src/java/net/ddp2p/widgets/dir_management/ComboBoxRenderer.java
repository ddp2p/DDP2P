package net.ddp2p.widgets.dir_management;

import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;

public class ComboBoxRenderer extends JComboBox
                           implements TableCellRenderer {
    
   JComboBox b;
    public ComboBoxRenderer () {
        setOpaque(true); //MUST do this for background to show up.
//        setEditable(true);
//        setSize(2,2);
//        setEnabled(true);
    }

    public Component getTableCellRendererComponent(
                            JTable table, Object items,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
           
         if(items==null) return null;                  
//        ArrayList<String> a = (ArrayList<String>) items;
//        String[] arr = new String[a.size()];
//        for(int i=0; i< arr.length; i++)
//        	arr[i]=(String) a.get(i);
    //    table.getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyComboBoxEditor(arr));
//        JComboBox cc= (JComboBox) 
        //	table.getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).getCellEditor().;
 //       	cc.addItem(arr[0]);                     	
        
//        for(int i=0; i< a.size(); i++)
//           this.addItem(a.get(i).toString());
//           
//           if (isSelected) {
//      setForeground(table.getSelectionForeground());
//      super.setBackground(table.getSelectionBackground());
//    } else {
//      setForeground(table.getForeground());
//      setBackground(table.getBackground());
//    }
   // setSelectedItem(a.get(0));
    
        return (JComboBox) items;
    }
}
